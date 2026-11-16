package io.gymcrm.facade;

import io.gymcrm.dto.CreatedProfile;
import io.gymcrm.dto.Credentials;
import io.gymcrm.dto.IssuedToken;
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
import io.gymcrm.entities.TrainingType;
import io.gymcrm.services.AuthenticationService;
import io.gymcrm.services.TraineeService;
import io.gymcrm.services.TrainerService;
import io.gymcrm.services.TrainingService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Single entry point to all operations; it only delegates. Who may do what is enforced by the services
 * themselves (@PreAuthorize), so it can't be skipped by calling a service directly.
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

    // Registration and login (no token needed)

    public CreatedProfile<Trainee> createTrainee(TraineeRegistration registration) {
        return traineeService.create(registration);
    }

    public CreatedProfile<Trainer> createTrainer(TrainerRegistration registration) {
        return trainerService.create(registration);
    }

    public IssuedToken login(Credentials credentials) {
        return authenticationService.login(credentials);
    }

    public void changePassword(String username, String oldPassword, String newPassword) {
        authenticationService.changePassword(username, oldPassword, newPassword);
    }

    // Trainee

    public Trainee getTraineeByUsername(String username) {
        return traineeService.getByUsername(username);
    }

    public Trainee updateTrainee(String username, TraineeUpdate update) {
        return traineeService.update(username, update);
    }

    public void setTraineeActive(String username, boolean active) {
        traineeService.setActive(username, active);
    }

    public void deleteTrainee(String username) {
        traineeService.delete(username);
    }

    public List<Trainer> getTrainersNotAssignedToTrainee(String traineeUsername) {
        return trainerService.getNotAssignedToTrainee(traineeUsername);
    }

    public List<Trainer> updateTraineeTrainers(String username, List<String> trainerUsernames) {
        return traineeService.updateTrainers(username, trainerUsernames);
    }

    // Trainer

    public Trainer getTrainerByUsername(String username) {
        return trainerService.getByUsername(username);
    }

    public Trainer updateTrainer(String username, TrainerUpdate update) {
        return trainerService.update(username, update);
    }

    public void setTrainerActive(String username, boolean active) {
        trainerService.setActive(username, active);
    }

    // Trainings

    public List<Training> getTraineeTrainings(String traineeUsername, TraineeTrainingCriteria criteria) {
        return trainingService.getTraineeTrainings(traineeUsername, criteria);
    }

    public List<Training> getTrainerTrainings(String trainerUsername, TrainerTrainingCriteria criteria) {
        return trainingService.getTrainerTrainings(trainerUsername, criteria);
    }

    public Training addTraining(NewTraining newTraining) {
        return trainingService.create(newTraining);
    }

    public List<TrainingType> getTrainingTypes() {
        return trainingService.getTrainingTypes();
    }
}
