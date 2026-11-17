package io.gymcrm.integration.workload;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * gymcrm.workload.* settings (defaults in application.yml).
 *
 * @param enabled        false: trainings are not reported anywhere (standalone profile and most tests)
 * @param url            base URL; the host is the Eureka service name, which the load balancer resolves
 * @param connectTimeout how long to wait for a connection
 * @param readTimeout    how long to wait for the response
 */
@ConfigurationProperties("gymcrm.workload")
public record WorkloadProperties(boolean enabled,
                                 String url,
                                 Duration connectTimeout,
                                 Duration readTimeout,
                                 Jwt jwt,
                                 CircuitBreaker circuitBreaker) {

    /**
     * @param secret   shared with the Trainer Workload service, at least 32 bytes
     * @param lifetime how long one service token stays valid
     */
    public record Jwt(String secret, Duration lifetime) {
    }

    /**
     * @param slidingWindowSize      how many recent calls are looked at
     * @param minimumNumberOfCalls   no decision before this many calls
     * @param failureRateThreshold   percentage of failed calls that opens the circuit
     * @param waitDurationInOpenState how long calls fail fast before trying again
     * @param permittedCallsInHalfOpenState trial calls that decide whether to close the circuit again
     */
    public record CircuitBreaker(int slidingWindowSize,
                                 int minimumNumberOfCalls,
                                 float failureRateThreshold,
                                 Duration waitDurationInOpenState,
                                 int permittedCallsInHalfOpenState) {
    }
}
