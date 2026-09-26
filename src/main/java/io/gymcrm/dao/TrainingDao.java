package io.gymcrm.dao;

import io.gymcrm.entities.Training;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrainingDao {
    Training create(Training training);
    Optional<Training> findById(UUID trainingId);
    List<Training> findAll();
}
