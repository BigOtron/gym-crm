package io.gymcrm.services.implementations;

import io.gymcrm.dao.TraineeDao;
import io.gymcrm.dao.TrainerDao;
import io.gymcrm.dao.TrainingDao;
import io.gymcrm.dto.NewTraining;
import io.gymcrm.dto.TraineeTrainingCriteria;
import io.gymcrm.dto.TrainerTrainingCriteria;
import io.gymcrm.entities.Trainee;
import io.gymcrm.entities.Trainer;
import io.gymcrm.entities.Training;
import io.gymcrm.exceptions.NotFoundException;
import io.gymcrm.services.TrainingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import static io.gymcrm.services.Validation.requireText;
import static io.gymcrm.services.Validation.requireValue;

@Slf4j
@Service
@Transactional
public class TrainingServiceImpl implements TrainingService {

    private TrainingDao trainingDao;
    private TraineeDao traineeDao;
    private TrainerDao trainerDao;

    @Autowired
    public void setTrainingDao(TrainingDao trainingDao) {
        this.trainingDao = trainingDao;
    }

    @Autowired
    public void setTraineeDao(TraineeDao traineeDao) {
        this.traineeDao = traineeDao;
    }

    @Autowired
    public void setTrainerDao(TrainerDao trainerDao) {
        this.trainerDao = trainerDao;
    }

    @Override
    public Training create(NewTraining newTraining) {
        Objects.requireNonNull(newTraining, "newTraining must not be null");
        String traineeUsername = requireText(newTraining.traineeUsername(), "Trainee username");
        String trainerUsername = requireText(newTraining.trainerUsername(), "Trainer username");
        String name = requireText(newTraining.trainingName(), "Training name");
        LocalDate date = requireValue(newTraining.trainingDate(), "Training date");
        Integer duration = requireValue(newTraining.durationMinutes(), "Training duration");
        if (duration <= 0) {
            log.warn("Rejected training '{}' with non-positive duration {}", name, duration);
            throw new IllegalArgumentException("Training duration must be positive");
        }
        log.debug("Creating training '{}' for trainee {} with trainer {}", name, traineeUsername, trainerUsername);

        Trainee trainee = traineeDao.findByUsername(traineeUsername).orElseThrow(() -> {
            log.warn("Training rejected, trainee not found: {}", traineeUsername);
            return new NotFoundException("Trainee not found: " + traineeUsername);
        });
        Trainer trainer = trainerDao.findByUsername(trainerUsername).orElseThrow(() -> {
            log.warn("Training rejected, trainer not found: {}", trainerUsername);
            return new NotFoundException("Trainer not found: " + trainerUsername);
        });

        Training training = new Training();
        training.setTrainee(trainee);
        training.setTrainer(trainer);
        training.setTrainingName(name);
        training.setTrainingType(trainer.getSpecialization());
        training.setTrainingDate(date);
        training.setTrainingDuration(duration);
        Training created = trainingDao.save(training);

        // A training links the trainee to the trainer.
        trainee.getTrainers().add(trainer);

        log.info("Training created: '{}' (id {}), type {}, date {}, trainee {}, trainer {}",
                name, created.getId(), trainer.getSpecialization().getTrainingTypeName(), date,
                traineeUsername, trainerUsername);
        return created;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Training> getTraineeTrainings(String traineeUsername, TraineeTrainingCriteria criteria) {
        if (traineeDao.findByUsername(traineeUsername).isEmpty()) {
            log.warn("Trainee not found by username {}", traineeUsername);
            throw new NotFoundException("Trainee not found: " + traineeUsername);
        }
        return trainingDao.findTraineeTrainings(traineeUsername,
                criteria == null ? TraineeTrainingCriteria.none() : criteria);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Training> getTrainerTrainings(String trainerUsername, TrainerTrainingCriteria criteria) {
        if (trainerDao.findByUsername(trainerUsername).isEmpty()) {
            log.warn("Trainer not found by username {}", trainerUsername);
            throw new NotFoundException("Trainer not found: " + trainerUsername);
        }
        return trainingDao.findTrainerTrainings(trainerUsername,
                criteria == null ? TrainerTrainingCriteria.none() : criteria);
    }
}
