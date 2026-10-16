package io.gymcrm.services;

import io.gymcrm.dto.Credentials;
import io.gymcrm.dto.NewTraining;
import io.gymcrm.dto.TraineeTrainingCriteria;
import io.gymcrm.dto.TrainerTrainingCriteria;
import io.gymcrm.entities.Training;
import io.gymcrm.entities.TrainingType;

import java.util.List;

/**
 * Every method authenticates {@code auth} first and throws
 * {@link io.gymcrm.exceptions.AuthenticationException} if it doesn't match.
 */
public interface TrainingService {
    /**
     * Only the training's trainee or trainer can add it.
     *
     * @throws io.gymcrm.exceptions.ForbiddenException for anyone else
     */
    Training create(Credentials auth, NewTraining newTraining);

    List<Training> getTraineeTrainings(Credentials auth, String traineeUsername, TraineeTrainingCriteria criteria);

    List<Training> getTrainerTrainings(Credentials auth, String trainerUsername, TrainerTrainingCriteria criteria);

    List<TrainingType> getTrainingTypes(Credentials auth);
}
