package io.gymcrm.services;

import io.gymcrm.dto.TrainerRegistration;
import io.gymcrm.dto.TrainerUpdate;
import io.gymcrm.entities.Trainer;

import java.util.List;

public interface TrainerService {
    Trainer create(TrainerRegistration registration);
    Trainer getByUsername(String username);
    void changePassword(String username, String newPassword);
    Trainer update(String username, TrainerUpdate update);

    boolean toggleActive(String username);

    List<Trainer> getNotAssignedToTrainee(String traineeUsername);
}
