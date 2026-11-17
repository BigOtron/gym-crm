package io.gymcrm.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "A trainer's training minutes per year and month")
public record TrainerWorkloadResponse(@Schema(example = "Anna.Lee") String trainerUsername,
                                      @Schema(example = "Anna") String trainerFirstName,
                                      @Schema(example = "Lee") String trainerLastName,
                                      @Schema(example = "ACTIVE") String trainerStatus,
                                      List<YearSummary> years) {

    public record YearSummary(@Schema(example = "2026") int year, List<MonthSummary> months) {
    }

    public record MonthSummary(@Schema(example = "11") int month,
                               @Schema(description = "Total training minutes in the month", example = "120")
                               int trainingSummaryDuration) {
    }
}
