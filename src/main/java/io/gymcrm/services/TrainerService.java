package io.gymcrm.services;

import io.gymcrm.entities.Trainer;

import java.util.List;
import java.util.UUID;

public interface TrainerService {
    Trainer create(Trainer trainer);
    Trainer update(Trainer trainer);
    Trainer getById(UUID userId);
    Trainer getByUsername(String username);
    List<Trainer> getAll();
}
