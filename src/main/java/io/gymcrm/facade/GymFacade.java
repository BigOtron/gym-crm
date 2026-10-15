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
import io.gymcrm.exceptions.ForbiddenException;
import io.gymcrm.services.AuthenticationService;
import io.gymcrm.services.TraineeService;
import io.gymcrm.services.TrainerService;
import io.gymcrm.services.TrainingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
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

    public Trainee createTrainee(TraineeRegistration registration) {
        return traineeService.create(registration);
    }

    public Trainer createTrainer(TrainerRegistration registration) {
        return trainerService.create(registration);
    }

    public boolean traineeCredentialsMatch(Credentials credentials) {
        return authenticationService.traineeCredentialsMatch(credentials);
    }

    public boolean trainerCredentialsMatch(Credentials credentials) {
        return authenticationService.trainerCredentialsMatch(credentials);
    }

    public void login(Credentials auth) {
        authenticationService.authenticate(auth);
    }

    public void changePassword(Credentials auth, String newPassword) {
        if (authenticationService.traineeCredentialsMatch(auth)) {
            traineeService.changePassword(auth.username(), newPassword);
        } else {
            authenticationService.authenticateTrainer(auth);
            trainerService.changePassword(auth.username(), newPassword);
        }
    }

    public Trainee getTraineeByUsername(Credentials auth, String username) {
        authenticationService.authenticate(auth);
        return traineeService.getByUsername(username);
    }

    public Trainee updateTrainee(Credentials auth, String username, TraineeUpdate update) {
        authenticationService.authenticateTrainee(auth);
        requireSelf(auth, username);
        return traineeService.update(username, update);
    }

    public void setTraineeActive(Credentials auth, String username, boolean active) {
        authenticationService.authenticateTrainee(auth);
        requireSelf(auth, username);
        traineeService.setActive(username, active);
    }

    public void deleteTrainee(Credentials auth, String username) {
        authenticationService.authenticateTrainee(auth);
        requireSelf(auth, username);
        traineeService.deleteByUsername(username);
    }

    public List<Trainer> getTrainersNotAssignedToTrainee(Credentials auth, String traineeUsername) {
        authenticationService.authenticate(auth);
        return trainerService.getNotAssignedToTrainee(traineeUsername);
    }

    public List<Trainer> updateTraineeTrainers(Credentials auth, String username, List<String> trainerUsernames) {
        authenticationService.authenticateTrainee(auth);
        requireSelf(auth, username);
        return traineeService.updateTrainers(username, trainerUsernames);
    }

    public Trainer getTrainerByUsername(Credentials auth, String username) {
        authenticationService.authenticate(auth);
        return trainerService.getByUsername(username);
    }

    public Trainer updateTrainer(Credentials auth, String username, TrainerUpdate update) {
        authenticationService.authenticateTrainer(auth);
        requireSelf(auth, username);
        return trainerService.update(username, update);
    }

    public void setTrainerActive(Credentials auth, String username, boolean active) {
        authenticationService.authenticateTrainer(auth);
        requireSelf(auth, username);
        trainerService.setActive(username, active);
    }

    public List<Training> getTraineeTrainings(Credentials auth, String traineeUsername,
                                              TraineeTrainingCriteria criteria) {
        authenticationService.authenticate(auth);
        return trainingService.getTraineeTrainings(traineeUsername, criteria);
    }

    public List<Training> getTrainerTrainings(Credentials auth, String trainerUsername,
                                              TrainerTrainingCriteria criteria) {
        authenticationService.authenticate(auth);
        return trainingService.getTrainerTrainings(trainerUsername, criteria);
    }

    public Training addTraining(Credentials auth, NewTraining newTraining) {
        authenticationService.authenticate(auth);
        if (newTraining != null && !auth.username().equals(newTraining.traineeUsername())
                && !auth.username().equals(newTraining.trainerUsername())) {
            log.warn("User {} tried to add a training they don't take part in", auth.username());
            throw new ForbiddenException("Only the trainee or the trainer can add a training");
        }
        return trainingService.create(newTraining);
    }

    public List<TrainingType> getTrainingTypes(Credentials auth) {
        authenticationService.authenticate(auth);
        return trainingService.getTrainingTypes();
    }

    private static void requireSelf(Credentials auth, String username) {
        if (!auth.username().equals(username)) {
            log.warn("User {} tried to change the profile of {}", auth.username(), username);
            throw new ForbiddenException("You can only change your own profile");
        }
    }
}
