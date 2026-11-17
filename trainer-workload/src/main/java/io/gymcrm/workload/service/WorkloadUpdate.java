package io.gymcrm.workload.service;

import io.gymcrm.workload.model.ActionType;

import java.time.LocalDate;

/** One training that was added to or deleted from Gym CRM. */
public record WorkloadUpdate(String trainerUsername,
                             String trainerFirstName,
                             String trainerLastName,
                             boolean active,
                             LocalDate trainingDate,
                             int trainingDurationMinutes,
                             ActionType actionType) {
}
