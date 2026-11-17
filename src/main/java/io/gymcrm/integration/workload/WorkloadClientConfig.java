package io.gymcrm.integration.workload;

import io.gymcrm.web.logging.TransactionId;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Clock;

/**
 * Wiring of the Trainer Workload client: either the real one (gymcrm.workload.enabled=true) or one that
 * does nothing.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(WorkloadProperties.class)
public class WorkloadClientConfig {

    static final String CIRCUIT_BREAKER = "trainer-workload";

    @Bean
    @ConditionalOnProperty(name = "gymcrm.workload.enabled", havingValue = "false")
    public TrainerWorkloadClient disabledTrainerWorkloadClient() {
        log.warn("Trainer workload integration is disabled: trainings are not reported");
        return new DisabledTrainerWorkloadClient();
    }

    @Bean
    @ConditionalOnProperty(name = "gymcrm.workload.enabled", havingValue = "true", matchIfMissing = true)
    public TrainerWorkloadClient trainerWorkloadClient(WorkloadProperties properties,
                                                       LoadBalancerInterceptor loadBalancer,
                                                       CircuitBreakerFactory<?, ?> circuitBreakers,
                                                       Clock clock) {
        WorkloadTokenProvider tokens = new WorkloadTokenProvider(properties.jwt().secret(),
                properties.jwt().lifetime(), clock);

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout());
        requestFactory.setReadTimeout(properties.readTimeout());

        // Not a RestClient.Builder bean: the Eureka client would pick that one up for its own calls.
        RestClient restClient = RestClient.builder()
                .baseUrl(properties.url())
                .requestFactory(requestFactory)
                // Replaces the service name in the URL (http://trainer-workload) with an instance from Eureka.
                .requestInterceptor(loadBalancer)
                .requestInterceptor((request, body, execution) -> {
                    request.getHeaders().setBearerAuth(tokens.token());
                    String transactionId = TransactionId.current();
                    if (transactionId != null) {
                        // The workload service logs the same id, so one request can be followed across both.
                        request.getHeaders().set(TransactionId.HEADER, transactionId);
                    }
                    return execution.execute(request, body);
                })
                .defaultHeader(HttpHeaders.ACCEPT, "application/json, application/problem+json")
                .build();

        log.info("Trainer workload service at {} (timeouts: connect {}, read {})", properties.url(),
                properties.connectTimeout(), properties.readTimeout());
        return new RestTrainerWorkloadClient(restClient, circuitBreakers.create(CIRCUIT_BREAKER));
    }

    /** The circuit breaker's thresholds, from gymcrm.workload.circuit-breaker. */
    @Bean
    public org.springframework.cloud.client.circuitbreaker.Customizer<Resilience4JCircuitBreakerFactory>
    workloadCircuitBreakerCustomizer(WorkloadProperties properties) {
        WorkloadProperties.CircuitBreaker settings = properties.circuitBreaker();
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(settings.slidingWindowSize())
                .minimumNumberOfCalls(settings.minimumNumberOfCalls())
                .failureRateThreshold(settings.failureRateThreshold())
                .waitDurationInOpenState(settings.waitDurationInOpenState())
                .permittedNumberOfCallsInHalfOpenState(settings.permittedCallsInHalfOpenState())
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();
        return factory -> factory.configure(builder -> builder.circuitBreakerConfig(config), CIRCUIT_BREAKER);
    }
}
