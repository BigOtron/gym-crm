package io.gymcrm.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * @param trainingDuration duration in minutes
 */
public record NewTrainingRequest(
        @NotBlank String traineeUsername,
        @NotBlank String trainerUsername,
        @NotBlank @Size(max = 255) String trainingName,
        @NotNull LocalDate trainingDate,
        @NotNull @Positive Integer trainingDuration) {
}
