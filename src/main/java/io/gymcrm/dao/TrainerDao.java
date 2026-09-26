package io.gymcrm.dao;

import io.gymcrm.entities.Trainer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrainerDao {
    Trainer create(Trainer Trainer);
    Trainer update(Trainer Trainer);
    Optional<Trainer> findById(UUID userId);
    Optional<Trainer> findByUsername(String username);
    List<Trainer> findAll();
}
