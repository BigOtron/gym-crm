package io.gymcrm.services;

import io.gymcrm.dto.Credentials;
import io.gymcrm.dto.TraineeRegistration;
import io.gymcrm.dto.TraineeUpdate;
import io.gymcrm.entities.Trainee;
import io.gymcrm.entities.Trainer;

import java.util.List;

/**
 * Every method except {@link #create} authenticates its {@code auth} credentials first and throws
 * {@link io.gymcrm.exceptions.AuthenticationException} if they don't match. Methods that change a
 * profile act on the authenticated trainee's own profile.
 */
public interface TraineeService {
    Trainee create(TraineeRegistration registration);

    /** Any authenticated user can read a trainee's profile. */
    Trainee getByUsername(Credentials auth, String username);

    void changePassword(Credentials auth, String newPassword);

    Trainee update(Credentials auth, TraineeUpdate update);

    /** Flips the active flag and returns the new value. */
    boolean toggleActive(Credentials auth);

    /** Deletes the authenticated trainee's profile, together with their trainings. */
    void delete(Credentials auth);

    /** Replaces the authenticated trainee's trainers with the given ones and returns them. */
    List<Trainer> updateTrainers(Credentials auth, List<String> trainerUsernames);
}
