package io.gymcrm.integration.workload;

import io.gymcrm.exceptions.BadGatewayException;
import io.gymcrm.exceptions.ServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/**
 * Reads workloads from the Trainer Workload service over REST, through a circuit breaker:
 * <ul>
 *   <li>closed: calls go through; connect and read timeouts stop calls that hang;</li>
 *   <li>open: after too many failures, calls fail at once for a while instead of waiting for timeouts,
 *       which protects both services;</li>
 *   <li>half-open: a few trial calls decide whether to close the circuit again.</li>
 * </ul>
 * An outage becomes a {@link ServiceUnavailableException} (HTTP 503 to our client).
 */
@Slf4j
public class RestTrainerWorkloadClient implements TrainerWorkloadClient {

    static final String TRAINER_WORKLOADS = "/api/trainer-workloads";

    private final RestClient restClient;
    private final CircuitBreaker circuitBreaker;

    public RestTrainerWorkloadClient(RestClient restClient, CircuitBreaker circuitBreaker) {
        this.restClient = restClient;
        this.circuitBreaker = circuitBreaker;
    }

    @Override
    public Optional<TrainerWorkloadSummary> findWorkload(String trainerUsername) {
        log.debug("Requesting the workload of trainer {}", trainerUsername);
        return circuitBreaker.run(() -> {
            try {
                return Optional.ofNullable(restClient.get()
                        .uri(TRAINER_WORKLOADS + "/{username}", trainerUsername)
                        .retrieve()
                        .body(TrainerWorkloadSummary.class));
            } catch (HttpClientErrorException.NotFound e) {
                // A valid answer: no training of this trainer was ever reported. Not a failure of the service.
                log.debug("The workload service has no data for trainer {}", trainerUsername);
                return Optional.empty();
            }
        }, failure -> {
            throw failed("read the workload of trainer " + trainerUsername, failure);
        });
    }

    /**
     * A 4xx answer means the service is up but refuses our request (e.g. 401 when the token secrets differ):
     * retrying won't help, so it becomes a 502 and doesn't count towards opening the circuit (see
     * WorkloadClientConfig). Everything else (no instance, connection refused, timeout, 5xx, open circuit)
     * is an outage: 503.
     */
    private static RuntimeException failed(String action, Throwable failure) {
        if (failure instanceof HttpClientErrorException rejected) {
            log.error("Could not {}: the workload service answered {}; check the configuration of both services "
                    + "(e.g. WORKLOAD_JWT_SECRET)", action, rejected.getStatusCode());
            return new BadGatewayException("The trainer workload service rejected the request", failure);
        }
        log.error("Could not {}: {}: {}", action, failure.getClass().getSimpleName(), failure.getMessage());
        return new ServiceUnavailableException("The trainer workload service is unavailable, please try again later",
                failure);
    }
}
