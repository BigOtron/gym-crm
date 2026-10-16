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
import io.gymcrm.entities.TrainingType;
import io.gymcrm.services.AuthenticationService;
import io.gymcrm.services.TraineeService;
import io.gymcrm.services.TrainerService;
import io.gymcrm.services.TrainingService;
import org.springframework.stereotype.Component;

import java.util.List;

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

    // Registration (no authentication)

    public Trainee createTrainee(TraineeRegistration registration) {
        return traineeService.create(registration);
    }

    public Trainer createTrainer(TrainerRegistration registration) {
        return trainerService.create(registration);
    }

    // Credentials

    public boolean traineeCredentialsMatch(Credentials credentials) {
        return authenticationService.traineeCredentialsMatch(credentials);
    }

    public boolean trainerCredentialsMatch(Credentials credentials) {
        return authenticationService.trainerCredentialsMatch(credentials);
    }

    public void login(Credentials auth) {
        authenticationService.login(auth);
    }

    public void changePassword(Credentials auth, String newPassword) {
        // Without assuming a role first, so a wrong password is logged as a failed login of the user.
        authenticationService.authenticate(auth);
        if (authenticationService.traineeCredentialsMatch(auth)) {
            traineeService.changePassword(auth, newPassword);
        } else {
            trainerService.changePassword(auth, newPassword);
        }
    }


    public Trainee getTraineeByUsername(Credentials auth, String username) {
        return traineeService.getByUsername(auth, username);
    }

    public Trainee updateTrainee(Credentials auth, String username, TraineeUpdate update) {
        return traineeService.update(auth, username, update);
    }

    public void setTraineeActive(Credentials auth, String username, boolean active) {
        traineeService.setActive(auth, username, active);
    }

    public void deleteTrainee(Credentials auth, String username) {
        traineeService.delete(auth, username);
    }

    public List<Trainer> getTrainersNotAssignedToTrainee(Credentials auth, String traineeUsername) {
        return trainerService.getNotAssignedToTrainee(auth, traineeUsername);
    }

    public List<Trainer> updateTraineeTrainers(Credentials auth, String username, List<String> trainerUsernames) {
        return traineeService.updateTrainers(auth, username, trainerUsernames);
    }


    public Trainer getTrainerByUsername(Credentials auth, String username) {
        return trainerService.getByUsername(auth, username);
    }

    public Trainer updateTrainer(Credentials auth, String username, TrainerUpdate update) {
        return trainerService.update(auth, username, update);
    }

    public void setTrainerActive(Credentials auth, String username, boolean active) {
        trainerService.setActive(auth, username, active);
    }


    public List<Training> getTraineeTrainings(Credentials auth, String traineeUsername,
                                              TraineeTrainingCriteria criteria) {
        return trainingService.getTraineeTrainings(auth, traineeUsername, criteria);
    }

    public List<Training> getTrainerTrainings(Credentials auth, String trainerUsername,
                                              TrainerTrainingCriteria criteria) {
        return trainingService.getTrainerTrainings(auth, trainerUsername, criteria);
    }

    public Training addTraining(Credentials auth, NewTraining newTraining) {
        return trainingService.create(auth, newTraining);
    }

    public List<TrainingType> getTrainingTypes(Credentials auth) {
        return trainingService.getTrainingTypes(auth);
    }
}
