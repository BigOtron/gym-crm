package io.gymcrm.dao;

import io.gymcrm.entities.Trainer;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TrainerDao {
    Trainer save(Trainer trainer);
    Optional<Trainer> findByUsername(String username);
    List<Trainer> findByUsernames(Collection<String> usernames);
    List<Trainer> findNotAssignedToTrainee(String traineeUsername);
}
