package io.gymcrm.services;

import io.gymcrm.dto.TraineeRegistration;
import io.gymcrm.dto.TraineeUpdate;
import io.gymcrm.entities.Trainee;
import io.gymcrm.entities.Trainer;

import java.util.List;

public interface TraineeService {
    Trainee create(TraineeRegistration registration);
    Trainee getByUsername(String username);
    void changePassword(String username, String newPassword);
    Trainee update(String username, TraineeUpdate update);

    boolean toggleActive(String username);

    void deleteByUsername(String username);

    List<Trainer> updateTrainers(String username, List<String> trainerUsernames);
}
