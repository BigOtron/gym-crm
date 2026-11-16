package io.gymcrm.dto;

import java.time.LocalDate;


public record TrainerTrainingCriteria(LocalDate fromDate, LocalDate toDate, String traineeName) {

    public static TrainerTrainingCriteria none() {
        return new TrainerTrainingCriteria(null, null, null);
    }
}
