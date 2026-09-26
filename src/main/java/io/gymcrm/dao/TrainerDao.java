package io.gymcrm.dao;

import io.gymcrm.entities.Trainer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrainerDao {
    Trainer create(Trainer trainer);
    Trainer update(Trainer trainer);
    Optional<Trainer> findById(UUID userId);
    Optional<Trainer> findByUsername(String username);
    List<Trainer> findAll();
}
