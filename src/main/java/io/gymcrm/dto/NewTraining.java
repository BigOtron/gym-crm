package io.gymcrm.dto;

import java.time.LocalDate;

public record NewTraining(String traineeUsername, String trainerUsername, String trainingName,
                          LocalDate trainingDate, Integer durationMinutes) {
}
