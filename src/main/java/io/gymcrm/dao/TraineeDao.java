package io.gymcrm.dao;

import io.gymcrm.entities.Trainee;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TraineeDao {
    Trainee create(Trainee trainee);
    Trainee update(Trainee trainee);
    boolean delete(UUID userId);
    Optional<Trainee> findById(UUID userId);
    Optional<Trainee> findByUsername(String username);
    List<Trainee> findAll();
}
