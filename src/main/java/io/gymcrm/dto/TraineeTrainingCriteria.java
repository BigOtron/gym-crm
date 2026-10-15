package io.gymcrm.dto;

import java.time.LocalDate;

public record TraineeTrainingCriteria(LocalDate fromDate, LocalDate toDate, String trainerName, String trainingType) {

    public static TraineeTrainingCriteria none() {
        return new TraineeTrainingCriteria(null, null, null, null);
    }
}
