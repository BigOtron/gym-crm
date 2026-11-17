package io.gymcrm.integration.workload;

import java.util.Optional;

/**
 * Talks to the Trainer Workload service.
 *
 * @see RestTrainerWorkloadClient
 */
public interface TrainerWorkloadClient {

    /**
     * Reports an added or deleted training.
     *
     * @throws io.gymcrm.exceptions.ServiceUnavailableException if the service can't be reached in time
     */
    void report(TrainerWorkloadEvent event);

    /**
     * The trainer's minutes per month, or empty if no training was ever reported for them.
     *
     * @throws io.gymcrm.exceptions.ServiceUnavailableException if the service can't be reached in time
     */
    Optional<TrainerWorkloadSummary> findWorkload(String trainerUsername);
}
