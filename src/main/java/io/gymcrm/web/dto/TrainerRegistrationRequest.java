package io.gymcrm.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * @param specializationId id of a training type from {@code GET /api/training-types}
 */
public record TrainerRegistrationRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotNull Long specializationId) {
}
