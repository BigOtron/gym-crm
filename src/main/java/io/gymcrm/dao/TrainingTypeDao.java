package io.gymcrm.dao;

import io.gymcrm.entities.TrainingType;

import java.util.List;
import java.util.Optional;

public interface TrainingTypeDao {
    Optional<TrainingType> findByName(String name);
    List<TrainingType> findAll();
}
