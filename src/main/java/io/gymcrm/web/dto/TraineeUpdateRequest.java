package io.gymcrm.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record TraineeUpdateRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @Past LocalDate dateOfBirth,
        @Size(max = 255) String address,
        @NotNull Boolean isActive) {
}
