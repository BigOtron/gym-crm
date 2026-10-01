package io.gymcrm.services;

import io.gymcrm.dto.NewTraining;
import io.gymcrm.dto.TraineeTrainingCriteria;
import io.gymcrm.dto.TrainerTrainingCriteria;
import io.gymcrm.entities.Training;

import java.util.List;

public interface TrainingService {
    Training create(NewTraining newTraining);
    List<Training> getTraineeTrainings(String traineeUsername, TraineeTrainingCriteria criteria);
    List<Training> getTrainerTrainings(String trainerUsername, TrainerTrainingCriteria criteria);
}
