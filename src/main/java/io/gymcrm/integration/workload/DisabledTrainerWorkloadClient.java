package io.gymcrm.integration.workload;

import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * Used when gymcrm.workload.enabled is false (the standalone profile runs Gym CRM alone): no workload is
 * known.
 */
@Slf4j
public class DisabledTrainerWorkloadClient implements TrainerWorkloadClient {

    @Override
    public Optional<TrainerWorkloadSummary> findWorkload(String trainerUsername) {
        log.debug("Workload integration is disabled, no workload for trainer {}", trainerUsername);
        return Optional.empty();
    }
}
