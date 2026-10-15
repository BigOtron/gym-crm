package io.gymcrm.facade;

import io.gymcrm.dto.Credentials;
import io.gymcrm.dto.NewTraining;
import io.gymcrm.dto.TraineeRegistration;
import io.gymcrm.dto.TraineeTrainingCriteria;
import io.gymcrm.dto.TraineeUpdate;
import io.gymcrm.dto.TrainerRegistration;
import io.gymcrm.dto.TrainerTrainingCriteria;
import io.gymcrm.dto.TrainerUpdate;
import io.gymcrm.entities.Trainee;
import io.gymcrm.entities.Trainer;
import io.gymcrm.entities.Training;
import io.gymcrm.services.AuthenticationService;
import io.gymcrm.services.TraineeService;
import io.gymcrm.services.TrainerService;
import io.gymcrm.services.TrainingService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Single entry point to all operations. Authentication is done by the services themselves,
 * so it can't be skipped by calling a service directly.
 */
@Component
public class GymFacade {

    private final AuthenticationService authenticationService;
    private final TraineeService traineeService;
    private final TrainerService trainerService;
    private final TrainingService trainingService;

    public GymFacade(AuthenticationService authenticationService,
                     TraineeService traineeService,
                     TrainerService trainerService,
                     TrainingService trainingService) {
        this.authenticationService = authenticationService;
        this.traineeService = traineeService;
        this.trainerService = trainerService;
        this.trainingService = trainingService;
    }

    // 1, 2. Create profiles (no authentication)

    public Trainer createTrainer(TrainerRegistration registration) {
        return trainerService.create(registration);
    }

    public Trainee createTrainee(TraineeRegistration registration) {
        return traineeService.create(registration);
    }

    // 3, 4. Username and password matching

    public boolean traineeCredentialsMatch(Credentials credentials) {
        return authenticationService.traineeCredentialsMatch(credentials);
    }

    public boolean trainerCredentialsMatch(Credentials credentials) {
        return authenticationService.trainerCredentialsMatch(credentials);
    }

    // 5, 6. Select profile by username

    public Trainer getTrainerByUsername(Credentials auth, String username) {
        return trainerService.getByUsername(auth, username);
    }

    public Trainee getTraineeByUsername(Credentials auth, String username) {
        return traineeService.getByUsername(auth, username);
    }

    // 7, 8. Password change

    public void changeTraineePassword(Credentials auth, String newPassword) {
        traineeService.changePassword(auth, newPassword);
    }

    public void changeTrainerPassword(Credentials auth, String newPassword) {
        trainerService.changePassword(auth, newPassword);
    }

    // 9, 10. Update profile

    public Trainer updateTrainer(Credentials auth, TrainerUpdate update) {
        return trainerService.update(auth, update);
    }

    public Trainee updateTrainee(Credentials auth, TraineeUpdate update) {
        return traineeService.update(auth, update);
    }

    // 11, 12. Activate / de-activate (each call flips the flag)

    public boolean toggleTraineeActive(Credentials auth) {
        return traineeService.toggleActive(auth);
    }

    public boolean toggleTrainerActive(Credentials auth) {
        return trainerService.toggleActive(auth);
    }

    // 13. Delete trainee profile (cascades to trainings)

    public void deleteTrainee(Credentials auth) {
        traineeService.delete(auth);
    }

    // 14, 15. Trainings lists by criteria

    public List<Training> getTraineeTrainings(Credentials auth, String traineeUsername,
                                              TraineeTrainingCriteria criteria) {
        return trainingService.getTraineeTrainings(auth, traineeUsername, criteria);
    }

    public List<Training> getTrainerTrainings(Credentials auth, String trainerUsername,
                                              TrainerTrainingCriteria criteria) {
        return trainingService.getTrainerTrainings(auth, trainerUsername, criteria);
    }

    // 16. Add training (by its trainee or its trainer)

    public Training addTraining(Credentials auth, NewTraining newTraining) {
        return trainingService.create(auth, newTraining);
    }

    // 17. Trainers not assigned to a trainee

    public List<Trainer> getTrainersNotAssignedToTrainee(Credentials auth, String traineeUsername) {
        return trainerService.getNotAssignedToTrainee(auth, traineeUsername);
    }

    // 18. Update trainee's trainers list

    public List<Trainer> updateTraineeTrainers(Credentials auth, List<String> trainerUsernames) {
        return traineeService.updateTrainers(auth, trainerUsernames);
    }
}
