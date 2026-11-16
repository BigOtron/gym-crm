package io.gymcrm.services;

import io.gymcrm.dto.CreatedProfile;
import io.gymcrm.dto.TrainerRegistration;
import io.gymcrm.dto.TrainerUpdate;
import io.gymcrm.entities.Trainer;

import java.util.List;

/**
 * Every method except {@link #create} requires a logged-in user. Methods that change a profile only
 * accept its owner (@PreAuthorize on the implementation).
 */
public interface TrainerService {
    /** Returns the new trainer with the generated password, which is stored only as a hash. */
    CreatedProfile<Trainer> create(TrainerRegistration registration);

    /** Any authenticated user can read a trainer's profile. */
    Trainer getByUsername(String username);

    Trainer update(String username, TrainerUpdate update);

    /** @throws io.gymcrm.exceptions.ConflictException if the trainer is already in that state */
    void setActive(String username, boolean active);

    /** Active trainers that are not in the trainee's trainers list; any authenticated user can ask. */
    List<Trainer> getNotAssignedToTrainee(String traineeUsername);
}
