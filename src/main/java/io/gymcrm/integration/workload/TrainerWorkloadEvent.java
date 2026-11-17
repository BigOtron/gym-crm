package io.gymcrm.integration.workload;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.gymcrm.entities.Training;
import io.gymcrm.entities.User;

import java.time.LocalDate;

/** The request body of POST /api/trainer-workloads in the Trainer Workload service. */
public record TrainerWorkloadEvent(String trainerUsername,
                                   String trainerFirstName,
                                   String trainerLastName,
                                   @JsonProperty("isActive") boolean isActive,
                                   LocalDate trainingDate,
                                   int trainingDuration,
                                   WorkloadAction actionType) {

    public static TrainerWorkloadEvent of(Training training, WorkloadAction action) {
        User trainer = training.getTrainer().getUser();
        return new TrainerWorkloadEvent(trainer.getUsername(), trainer.getFirstName(), trainer.getLastName(),
                trainer.isActive(), training.getTrainingDate(), training.getTrainingDuration(), action);
    }
}
