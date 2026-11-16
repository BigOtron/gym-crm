package io.gymcrm.services;

import io.gymcrm.dto.NewTraining;
import io.gymcrm.dto.TraineeTrainingCriteria;
import io.gymcrm.dto.TrainerTrainingCriteria;
import io.gymcrm.entities.Training;
import io.gymcrm.entities.TrainingType;

import java.util.List;

/**
 * Every method requires a logged-in user (@PreAuthorize on the implementation).
 */
public interface TrainingService {
    /** Only the training's own trainee or trainer may add it. */
    Training create(NewTraining newTraining);

    List<Training> getTraineeTrainings(String traineeUsername, TraineeTrainingCriteria criteria);

    List<Training> getTrainerTrainings(String trainerUsername, TrainerTrainingCriteria criteria);

    List<TrainingType> getTrainingTypes();
}
