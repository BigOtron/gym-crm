package io.gymcrm.workload.messaging;

import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.RedeliveryPolicy;
import org.springframework.boot.activemq.autoconfigure.ActiveMQConnectionFactoryCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableConfigurationProperties(MessagingProperties.class)
public class MessagingConfig {

    /**
     * When the listener fails, its transaction is rolled back and ActiveMQ delivers the message again after a
     * growing delay (1 s, 2 s, 4 s with the defaults). The listener itself moves the message to the dead
     * letter queue on its last attempt; ActiveMQ's own limit below is only a safety net behind that.
     */
    @Bean
    public ActiveMQConnectionFactoryCustomizer redeliveryPolicy(MessagingProperties properties) {
        return factory -> {
            RedeliveryPolicy policy = factory.getRedeliveryPolicy();
            policy.setMaximumRedeliveries(properties.maxAttempts() - 1);
            policy.setInitialRedeliveryDelay(properties.redeliveryDelay().toMillis());
            policy.setRedeliveryDelay(properties.redeliveryDelay().toMillis());
            policy.setUseExponentialBackOff(true);
            policy.setBackOffMultiplier(properties.backOffMultiplier());
            log.info("Workload queue {}: {} attempts per message, then dead letter queue {}", properties.queue(),
                    properties.maxAttempts(), properties.deadLetterQueue());
        };
    }
}
