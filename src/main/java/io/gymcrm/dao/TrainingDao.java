package io.gymcrm.dao;

import io.gymcrm.dto.TraineeTrainingCriteria;
import io.gymcrm.dto.TrainerTrainingCriteria;
import io.gymcrm.entities.Training;

import java.util.List;

public interface TrainingDao {
    Training save(Training training);
    List<Training> findTraineeTrainings(String traineeUsername, TraineeTrainingCriteria criteria);
    List<Training> findTrainerTrainings(String trainerUsername, TrainerTrainingCriteria criteria);
}
