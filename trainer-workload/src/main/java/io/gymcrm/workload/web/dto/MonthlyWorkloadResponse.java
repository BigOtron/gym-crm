package io.gymcrm.workload.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A trainer's training minutes in one month")
public record MonthlyWorkloadResponse(@Schema(example = "Anna.Lee") String trainerUsername,
                                      @Schema(example = "2026") int year,
                                      @Schema(example = "11") int month,
                                      @Schema(description = "Total training minutes in the month", example = "120")
                                      int trainingSummaryDuration) {
}
