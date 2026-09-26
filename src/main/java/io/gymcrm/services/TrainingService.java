package io.gymcrm.services;

import io.gymcrm.entities.Training;

import java.util.List;
import java.util.UUID;

public interface TrainingService {
    Training create(Training training);
    Training getById(UUID trainingId);
    List<Training> getAll();
}
