package io.gymcrm.facade;

import io.gymcrm.entities.Trainee;
import io.gymcrm.entities.Trainer;
import io.gymcrm.entities.Training;
import io.gymcrm.services.TraineeService;
import io.gymcrm.services.TrainerService;
import io.gymcrm.services.TrainingService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class GymFacade {

    private final TraineeService traineeService;
    private final TrainerService trainerService;
    private final TrainingService trainingService;

    public GymFacade(TraineeService traineeService,
                     TrainerService trainerService,
                     TrainingService trainingService) {
        this.traineeService = traineeService;
        this.trainerService = trainerService;
        this.trainingService = trainingService;
    }

    // Trainee

    public Trainee createTrainee(Trainee trainee) {
        return traineeService.create(trainee);
    }

    public Trainee updateTrainee(Trainee trainee) {
        return traineeService.update(trainee);
    }

    public void deleteTrainee(UUID userId) {
        traineeService.delete(userId);
    }

    public Trainee getTraineeById(UUID userId) {
        return traineeService.getById(userId);
    }

    public Trainee getTraineeByUsername(String username) {
        return traineeService.getByUsername(username);
    }

    public List<Trainee> getAllTrainees() {
        return traineeService.getAll();
    }

    public Trainer createTrainer(Trainer trainer) {
        return trainerService.create(trainer);
    }

    public Trainer updateTrainer(Trainer trainer) {
        return trainerService.update(trainer);
    }

    public Trainer getTrainerById(UUID userId) {
        return trainerService.getById(userId);
    }

    public Trainer getTrainerByUsername(String username) {
        return trainerService.getByUsername(username);
    }

    public List<Trainer> getAllTrainers() {
        return trainerService.getAll();
    }

    public Training createTraining(Training training) {
        return trainingService.create(training);
    }

    public Training getTrainingById(UUID trainingId) {
        return trainingService.getById(trainingId);
    }

    public List<Training> getAllTrainings() {
        return trainingService.getAll();
    }
}