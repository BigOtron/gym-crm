package io.gymcrm.services;

import io.gymcrm.dto.Credentials;
import io.gymcrm.dto.NewTraining;
import io.gymcrm.dto.TraineeTrainingCriteria;
import io.gymcrm.dto.TrainerTrainingCriteria;
import io.gymcrm.entities.Training;

import java.util.List;

/**
 * Every method authenticates its {@code auth} credentials first and throws
 * {@link io.gymcrm.exceptions.AuthenticationException} if they don't match.
 */
public interface TrainingService {
    /** Only the training's trainee or trainer can add it. */
    Training create(Credentials auth, NewTraining newTraining);

    List<Training> getTraineeTrainings(Credentials auth, String traineeUsername, TraineeTrainingCriteria criteria);

    List<Training> getTrainerTrainings(Credentials auth, String trainerUsername, TrainerTrainingCriteria criteria);
}
