package io.gymcrm.services;

import io.gymcrm.dto.Credentials;
import io.gymcrm.dto.TraineeRegistration;
import io.gymcrm.dto.TraineeUpdate;
import io.gymcrm.entities.Trainee;
import io.gymcrm.entities.Trainer;

import java.util.List;

/**
 * Every method except {@link #create} authenticates {@code auth} first and throws
 * {@link io.gymcrm.exceptions.AuthenticationException} if it doesn't match. Methods that change a
 * profile only accept the profile's owner and throw {@link io.gymcrm.exceptions.ForbiddenException}
 * for anyone else.
 */
public interface TraineeService {
    Trainee create(TraineeRegistration registration);

    /** Any authenticated user can read a trainee's profile. */
    Trainee getByUsername(Credentials auth, String username);

    /** Changes the authenticated trainee's own password. */
    void changePassword(Credentials auth, String newPassword);

    Trainee update(Credentials auth, String username, TraineeUpdate update);

    /** @throws io.gymcrm.exceptions.ConflictException if the trainee is already in that state */
    void setActive(Credentials auth, String username, boolean active);

    /** Deletes the trainee's profile together with their trainings. */
    void delete(Credentials auth, String username);

    /** Replaces the trainee's trainers with the given ones and returns them. */
    List<Trainer> updateTrainers(Credentials auth, String username, List<String> trainerUsernames);
}
