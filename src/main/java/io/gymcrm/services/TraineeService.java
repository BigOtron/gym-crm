package io.gymcrm.services;

import io.gymcrm.dto.CreatedProfile;
import io.gymcrm.dto.TraineeRegistration;
import io.gymcrm.dto.TraineeUpdate;
import io.gymcrm.entities.Trainee;
import io.gymcrm.entities.Trainer;

import java.util.List;

/**
 * Every method except {@link #create} requires a logged-in user. Methods that change a profile only
 * accept its owner (@PreAuthorize on the implementation).
 */
public interface TraineeService {
    /** Returns the new trainee with the generated password, which is stored only as a hash. */
    CreatedProfile<Trainee> create(TraineeRegistration registration);

    /** Any authenticated user can read a trainee's profile. */
    Trainee getByUsername(String username);

    Trainee update(String username, TraineeUpdate update);

    /** @throws io.gymcrm.exceptions.ConflictException if the trainee is already in that state */
    void setActive(String username, boolean active);

    /** Deletes the trainee's profile together with their trainings. */
    void delete(String username);

    /** Replaces the trainee's trainers with the given ones and returns them. */
    List<Trainer> updateTrainers(String username, List<String> trainerUsernames);
}
