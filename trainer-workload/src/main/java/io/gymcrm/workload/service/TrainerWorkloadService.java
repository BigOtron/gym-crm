package io.gymcrm.workload.service;

import io.gymcrm.workload.model.TrainerWorkload;

import java.time.YearMonth;

public interface TrainerWorkloadService {

    /** Adds the training's minutes to its month, or removes them, depending on the action type. */
    void apply(WorkloadUpdate update);

    /** All years and months of the trainer. */
    TrainerWorkload getWorkload(String trainerUsername);

    /** The trainer's training minutes in one month (0 if none). */
    int getMinutes(String trainerUsername, YearMonth month);
}
