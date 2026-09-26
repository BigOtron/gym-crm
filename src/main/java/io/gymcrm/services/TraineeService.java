package io.gymcrm.services;

import io.gymcrm.entities.Trainee;

import java.util.List;
import java.util.UUID;

public interface TraineeService {
    Trainee create(Trainee trainee);
    Trainee update(Trainee trainee);
    void delete(UUID userId);
    Trainee getById(UUID userId);
    Trainee getByUsername(String username);
    List<Trainee> getAll();
}
