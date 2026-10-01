package io.gymcrm.dao;

import io.gymcrm.entities.Trainee;

import java.util.Optional;

public interface TraineeDao {
    Trainee save(Trainee trainee);
    void delete(Trainee trainee);
    Optional<Trainee> findByUsername(String username);
}
