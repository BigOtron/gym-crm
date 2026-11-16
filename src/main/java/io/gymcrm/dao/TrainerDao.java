package io.gymcrm.dao;

import io.gymcrm.entities.Trainer;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface TrainerDao {
    Trainer save(Trainer trainer);
    Optional<Trainer> findByUsername(String username);
    List<Trainer> findByUsernames(Collection<String> usernames);
    List<Trainer> findNotAssignedToTrainee(String traineeUsername);

    /** Number of active trainers for every training type, in id order; types without any have 0. */
    Map<String, Long> countActiveTrainersPerTrainingType();

    /** Number of trainers whose profile is active. */
    long countActive();
}
