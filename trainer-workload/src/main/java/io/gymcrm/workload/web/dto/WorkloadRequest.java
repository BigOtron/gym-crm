package io.gymcrm.workload.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.gymcrm.workload.model.ActionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

@Schema(description = "A training that was added to or deleted from Gym CRM")
public record WorkloadRequest(
        @Schema(example = "Anna.Lee") @NotBlank String trainerUsername,
        @Schema(example = "Anna") @NotBlank String trainerFirstName,
        @Schema(example = "Lee") @NotBlank String trainerLastName,
        @Schema(example = "true") @JsonProperty("isActive") @NotNull Boolean isActive,
        @Schema(example = "2026-11-20") @NotNull LocalDate trainingDate,
        @Schema(description = "Minutes", example = "60") @NotNull @Positive Integer trainingDuration,
        @Schema(example = "ADD") @NotNull ActionType actionType) {
}
