package io.gymcrm.workload.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.gymcrm.workload.model.ActionType;
import io.gymcrm.workload.service.WorkloadUpdate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

/**
 * The JSON body of a message on the workload queue: a training that was added to or deleted from Gym CRM.
 * Every field is required; a message without one goes to the dead letter queue.
 */
public record WorkloadMessage(@NotBlank String trainerUsername,
                              @NotBlank String trainerFirstName,
                              @NotBlank String trainerLastName,
                              @JsonProperty("isActive") @NotNull Boolean isActive,
                              @NotNull LocalDate trainingDate,
                              @NotNull @Positive Integer trainingDuration,
                              @NotNull ActionType actionType) {

    WorkloadUpdate toUpdate() {
        return new WorkloadUpdate(trainerUsername, trainerFirstName, trainerLastName, isActive, trainingDate,
                trainingDuration, actionType);
    }
}
