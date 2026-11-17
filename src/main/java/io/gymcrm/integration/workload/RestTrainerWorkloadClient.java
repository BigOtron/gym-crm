package io.gymcrm.integration.workload;

import io.gymcrm.exceptions.ServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/**
 * Calls the Trainer Workload service through a circuit breaker:
 * <ul>
 *   <li>closed: calls go through; connect and read timeouts stop calls that hang;</li>
 *   <li>open: after too many failures, calls fail at once for a while instead of waiting for timeouts,
 *       which protects both services;</li>
 *   <li>half-open: a few trial calls decide whether to close the circuit again.</li>
 * </ul>
 * Every failure becomes a {@link ServiceUnavailableException} (HTTP 503 to our client), and because the
 * call runs inside the service's transaction, the training isn't saved (or the trainee isn't deleted).
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
    public void report(TrainerWorkloadEvent event) {
        log.debug("Reporting {} of a {} min training on {} for trainer {}", event.actionType(),
                event.trainingDuration(), event.trainingDate(), event.trainerUsername());
        circuitBreaker.run(() -> restClient.post()
                        .uri(TRAINER_WORKLOADS)
                        .body(event)
                        .retrieve()
                        .toBodilessEntity(),
                failure -> {
                    throw unavailable("report a training of trainer " + event.trainerUsername(), failure);
                });
        log.info("Reported {} of a training on {} for trainer {} to the workload service", event.actionType(),
                event.trainingDate(), event.trainerUsername());
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
            throw unavailable("read the workload of trainer " + trainerUsername, failure);
        });
    }

    private static ServiceUnavailableException unavailable(String action, Throwable failure) {
        log.error("Could not {}: {}: {}", action, failure.getClass().getSimpleName(), failure.getMessage());
        return new ServiceUnavailableException("The trainer workload service is unavailable, please try again later",
                failure);
    }
}
