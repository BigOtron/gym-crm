package io.gymcrm.services;

import io.gymcrm.dto.Credentials;
import io.gymcrm.dto.TrainerRegistration;
import io.gymcrm.dto.TrainerUpdate;
import io.gymcrm.entities.Trainer;

import java.util.List;

/**
 * Every method except {@link #create} authenticates {@code auth} first and throws
 * {@link io.gymcrm.exceptions.AuthenticationException} if it doesn't match. Methods that change a
 * profile only accept the profile's owner and throw {@link io.gymcrm.exceptions.ForbiddenException}
 * for anyone else.
 */
public interface TrainerService {
    Trainer create(TrainerRegistration registration);

    /** Any authenticated user can read a trainer's profile. */
    Trainer getByUsername(Credentials auth, String username);

    /** Changes the authenticated trainer's own password. */
    void changePassword(Credentials auth, String newPassword);

    Trainer update(Credentials auth, String username, TrainerUpdate update);

    /** @throws io.gymcrm.exceptions.ConflictException if the trainer is already in that state */
    void setActive(Credentials auth, String username, boolean active);

    /** Active trainers that are not in the trainee's trainers list; any authenticated user can ask. */
    List<Trainer> getNotAssignedToTrainee(Credentials auth, String traineeUsername);
}
