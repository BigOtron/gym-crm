package io.gymcrm.services;

import io.gymcrm.dto.Credentials;
import io.gymcrm.dto.TrainerRegistration;
import io.gymcrm.dto.TrainerUpdate;
import io.gymcrm.entities.Trainer;

import java.util.List;

/**
 * Every method except {@link #create} authenticates its {@code auth} credentials first and throws
 * {@link io.gymcrm.exceptions.AuthenticationException} if they don't match. Methods that change a
 * profile act on the authenticated trainer's own profile.
 */
public interface TrainerService {
    Trainer create(TrainerRegistration registration);

    /** Any authenticated user can read a trainer's profile. */
    Trainer getByUsername(Credentials auth, String username);

    void changePassword(Credentials auth, String newPassword);

    Trainer update(Credentials auth, TrainerUpdate update);

    /** Flips the active flag and returns the new value. */
    boolean toggleActive(Credentials auth);

    /** Active trainers that are not in the trainee's trainers list; any authenticated user can ask. */
    List<Trainer> getNotAssignedToTrainee(Credentials auth, String traineeUsername);
}
