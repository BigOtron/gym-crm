package io.gymcrm.integration.workload;

import lombok.extern.slf4j.Slf4j;

/**
 * Used when gymcrm.workload.enabled is false (the standalone profile runs Gym CRM alone): trainings are
 * not reported.
 */
@Slf4j
public class DisabledTrainerWorkloadPublisher implements TrainerWorkloadPublisher {

    @Override
    public void publish(TrainerWorkloadEvent event) {
        log.debug("Workload integration is disabled, not reporting {} for trainer {}", event.actionType(),
                event.trainerUsername());
    }
}
