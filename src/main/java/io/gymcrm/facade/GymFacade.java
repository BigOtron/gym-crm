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
import io.gymcrm.exceptions.AuthenticationException;
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


    public Trainer createTrainer(TrainerRegistration registration) {
        return trainerService.create(registration);
    }

    public Trainee createTrainee(TraineeRegistration registration) {
        return traineeService.create(registration);
    }


    public boolean traineeCredentialsMatch(Credentials credentials) {
        return authenticationService.traineeCredentialsMatch(credentials);
    }

    public boolean trainerCredentialsMatch(Credentials credentials) {
        return authenticationService.trainerCredentialsMatch(credentials);
    }


    public Trainer getTrainerByUsername(Credentials auth, String username) {
        authenticationService.authenticate(auth);
        return trainerService.getByUsername(username);
    }

    public Trainee getTraineeByUsername(Credentials auth, String username) {
        authenticationService.authenticate(auth);
        return traineeService.getByUsername(username);
    }


    public void changeTraineePassword(Credentials auth, String newPassword) {
        authenticationService.authenticateTrainee(auth);
        traineeService.changePassword(auth.username(), newPassword);
    }

    public void changeTrainerPassword(Credentials auth, String newPassword) {
        authenticationService.authenticateTrainer(auth);
        trainerService.changePassword(auth.username(), newPassword);
    }

    public Trainer updateTrainer(Credentials auth, TrainerUpdate update) {
        authenticationService.authenticateTrainer(auth);
        return trainerService.update(auth.username(), update);
    }

    public Trainee updateTrainee(Credentials auth, TraineeUpdate update) {
        authenticationService.authenticateTrainee(auth);
        return traineeService.update(auth.username(), update);
    }


    public boolean toggleTraineeActive(Credentials auth) {
        authenticationService.authenticateTrainee(auth);
        return traineeService.toggleActive(auth.username());
    }

    public boolean toggleTrainerActive(Credentials auth) {
        authenticationService.authenticateTrainer(auth);
        return trainerService.toggleActive(auth.username());
    }


    public void deleteTrainee(Credentials auth) {
        authenticationService.authenticateTrainee(auth);
        traineeService.deleteByUsername(auth.username());
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
            throw new AuthenticationException("Only the trainee or the trainer can add a training");
        }
        return trainingService.create(newTraining);
    }

    public List<Trainer> getTrainersNotAssignedToTrainee(Credentials auth, String traineeUsername) {
        authenticationService.authenticate(auth);
        return trainerService.getNotAssignedToTrainee(traineeUsername);
    }

    public List<Trainer> updateTraineeTrainers(Credentials auth, List<String> trainerUsernames) {
        authenticationService.authenticateTrainee(auth);
        return traineeService.updateTrainers(auth.username(), trainerUsernames);
    }
}
