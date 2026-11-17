package io.gymcrm.integration.workload;

import java.util.List;

/** The response of GET /api/trainer-workloads/{username} in the Trainer Workload service. */
public record TrainerWorkloadSummary(String trainerUsername,
                                     String trainerFirstName,
                                     String trainerLastName,
                                     String trainerStatus,
                                     List<Year> years) {

    public record Year(int year, List<Month> months) {
    }

    public record Month(int month, int trainingSummaryDuration) {
    }
}
