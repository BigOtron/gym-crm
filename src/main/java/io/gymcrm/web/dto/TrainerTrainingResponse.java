package io.gymcrm.web.dto;

import java.time.LocalDate;

/**
 * @param trainingDuration duration in minutes
 * @param traineeName      the trainee's first and last name
 */
public record TrainerTrainingResponse(String trainingName, LocalDate trainingDate, String trainingType,
                                      Integer trainingDuration, String traineeName) {
}
