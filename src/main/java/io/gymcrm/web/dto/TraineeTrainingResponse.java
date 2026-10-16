package io.gymcrm.web.dto;

import java.time.LocalDate;

/**
 * @param trainingDuration duration in minutes
 * @param trainerName      the trainer's first and last name
 */
public record TraineeTrainingResponse(String trainingName, LocalDate trainingDate, String trainingType,
                                      Integer trainingDuration, String trainerName) {
}
