package io.gymcrm.workload.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * workload.messaging.* settings (defaults in application.yml).
 *
 * @param queue             where Gym CRM sends added and deleted trainings
 * @param deadLetterQueue   where messages go that can't be processed, with the reason
 * @param maxAttempts       how often a message is tried before it goes to the dead letter queue
 * @param redeliveryDelay   the wait before the first retry
 * @param backOffMultiplier each further retry waits this many times longer than the one before
 */
@ConfigurationProperties("workload.messaging")
public record MessagingProperties(String queue,
                                  String deadLetterQueue,
                                  int maxAttempts,
                                  Duration redeliveryDelay,
                                  double backOffMultiplier) {
}
