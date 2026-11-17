package io.gymcrm.health;

import com.netflix.appinfo.InstanceInfo.InstanceStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.health.actuate.endpoint.StatusAggregator;
import org.springframework.boot.health.contributor.Status;
import org.springframework.cloud.netflix.eureka.EurekaHealthCheckHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Sends our health status to Eureka (eureka.client.healthcheck.enabled). Eureka knows only UP, DOWN,
 * OUT_OF_SERVICE and UNKNOWN, so our own WARNING (see TrainerAvailabilityHealthIndicator) would show as
 * UNKNOWN. It is reported as UP instead, just like the health endpoint answers it with HTTP 200.
 */
@Configuration
public class EurekaHealthConfig {

    @Bean
    @ConditionalOnProperty(name = "eureka.client.healthcheck.enabled", havingValue = "true")
    public EurekaHealthCheckHandler eurekaHealthCheckHandler(StatusAggregator statusAggregator) {
        return new WarningAsUpHealthCheckHandler(statusAggregator);
    }

    /** Uses our status order (application.yml), in which WARNING is between DOWN and UP. */
    public static class WarningAsUpHealthCheckHandler extends EurekaHealthCheckHandler {

        public WarningAsUpHealthCheckHandler(StatusAggregator statusAggregator) {
            super(statusAggregator);
        }

        @Override
        public InstanceStatus mapToInstanceStatus(Status status) {
            return TrainerAvailabilityHealthIndicator.WARNING.getCode().equals(status.getCode())
                    ? InstanceStatus.UP
                    : super.mapToInstanceStatus(status);
        }
    }
}
