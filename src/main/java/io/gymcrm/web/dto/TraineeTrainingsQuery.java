package io.gymcrm.web.dto;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.AssertTrue;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Query parameters of {@code GET /api/trainees/{username}/trainings}; all optional.
 */
public record TraineeTrainingsQuery(
        @Parameter(description = "Trainings on or after this date", example = "2026-09-01")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @Parameter(description = "Trainings on or before this date", example = "2026-09-30")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @Parameter(description = "Part of the trainer's first or last name, case-insensitive", example = "Anna")
        String trainerName,
        @Parameter(description = "Training type name, case-insensitive", example = "Yoga")
        String trainingType) {

    @Parameter(hidden = true)
    @AssertTrue(message = "'from' must not be after 'to'")
    public boolean isPeriodValid() {
        return from == null || to == null || !from.isAfter(to);
    }
}
