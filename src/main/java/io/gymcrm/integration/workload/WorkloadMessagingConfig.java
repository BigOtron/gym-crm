package io.gymcrm.integration.workload;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;
import tools.jackson.databind.json.JsonMapper;

/**
 * Wiring of the workload publisher: either the one that sends to ActiveMQ (gymcrm.workload.enabled=true)
 * or one that does nothing.
 */
@Slf4j
@Configuration
public class WorkloadMessagingConfig {

    @Bean
    @ConditionalOnProperty(name = "gymcrm.workload.enabled", havingValue = "false")
    public TrainerWorkloadPublisher disabledTrainerWorkloadPublisher() {
        return new DisabledTrainerWorkloadPublisher();
    }

    @Bean
    @ConditionalOnProperty(name = "gymcrm.workload.enabled", havingValue = "true", matchIfMissing = true)
    public TrainerWorkloadPublisher trainerWorkloadPublisher(JmsTemplate jmsTemplate, JsonMapper jsonMapper,
                                                             WorkloadProperties properties) {
        if (!jmsTemplate.isSessionTransacted()) {
            // Without it, a message would be sent before the database commit, even if that then fails.
            throw new IllegalStateException("spring.jms.template.session.transacted must be true");
        }
        log.info("Trainings are reported to the ActiveMQ queue {}", properties.queue());
        return new JmsTrainerWorkloadPublisher(jmsTemplate, jsonMapper, properties.queue());
    }
}
