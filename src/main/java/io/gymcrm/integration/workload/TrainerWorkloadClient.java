package io.gymcrm.integration.workload;

import java.util.Optional;

/**
 * Reads from the Trainer Workload service. A read needs an answer right away, so it stays a synchronous
 * REST call; changes go through the message queue instead ({@link TrainerWorkloadPublisher}).
 *
 * @see RestTrainerWorkloadClient
 */
public interface TrainerWorkloadClient {

    /**
     * The trainer's minutes per month, or empty if no training was ever reported for them.
     *
     * @throws io.gymcrm.exceptions.ServiceUnavailableException if the service can't be reached in time
     */
    Optional<TrainerWorkloadSummary> findWorkload(String trainerUsername);
}
