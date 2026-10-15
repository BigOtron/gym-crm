package io.gymcrm.web.dto;

import jakarta.validation.constraints.AssertTrue;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Query parameters of {@code GET /api/trainers/{username}/trainings}; all optional.
 */
public record TrainerTrainingsQuery(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        String traineeName) {

    @AssertTrue(message = "'from' must not be after 'to'")
    public boolean isPeriodValid() {
        return from == null || to == null || !from.isAfter(to);
    }
}
