package io.gymcrm.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * @param specializationId read-only: accepted so clients can send the profile back as they got it,
 *                         but ignored, because a trainer's specialization can't be changed
 */
public record TrainerUpdateRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        Long specializationId,
        @NotNull Boolean isActive) {
}
