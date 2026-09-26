package io.gymcrm.services.implementations;

import io.gymcrm.dao.TraineeDao;
import io.gymcrm.dao.TrainerDao;
import io.gymcrm.dao.TrainingDao;
import io.gymcrm.entities.Trainer;
import io.gymcrm.entities.Training;
import io.gymcrm.services.TrainingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
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
    public Training create(Training training) {
        log.debug("Creating training '{}'", training == null ? null : training.getTrainingName());
        validateFields(training);

        if (traineeDao.findById(training.getTraineeId()) == null) {
            log.warn("Training rejected, trainee not found: {}", training.getTraineeId());
            throw new NoSuchElementException("Trainee not found: " + training.getTraineeId());
        }
        Trainer trainer = trainerDao.findById(training.getTrainerId())
                .orElseThrow(() -> {
                    log.warn("Training rejected, trainer not found: {}", training.getTrainerId());
                    return new NoSuchElementException("Trainer not found: " + training.getTrainerId());
                });

        if (!trainer.getSpecialization().contains(training.getTrainingType())) {
            log.warn("Training rejected, trainer {} does not teach {} (teaches {})",
                    trainer.getUsername(), training.getTrainingType(), trainer.getSpecialization());
            throw new IllegalArgumentException("Trainer " + trainer.getUsername()
                    + " does not teach " + training.getTrainingType());
        }

        training.setTrainingId(null);
        Training created = trainingDao.create(training);
        log.info("Training created: '{}' (id {}), type {}, date {}, trainee {}, trainer {}",
                created.getTrainingName(), created.getTrainingId(), created.getTrainingType(),
                created.getTrainingDate(), created.getTraineeId(), created.getTrainerId());
        return created;
    }

    @Override
    public Training getById(UUID trainingId) {
        log.debug("Selecting training by id {}", trainingId);
        return trainingDao.findById(trainingId)
                .orElseThrow(() -> {
                    log.warn("Training not found by id {}", trainingId);
                    return new NoSuchElementException("Training not found: " + trainingId);
                });
    }

    @Override
    public List<Training> getAll() {
        List<Training> all = trainingDao.findAll();
        log.debug("Selected all trainings ({} records)", all.size());
        return all;
    }

    private void validateFields(Training training) {
        Objects.requireNonNull(training, "training must not be null");
        if (training.getTraineeId() == null || training.getTrainerId() == null
                || training.getTrainingType() == null || training.getTrainingDate() == null
                || training.getTrainingDuration() == null
                || training.getTrainingName() == null || training.getTrainingName().isBlank()) {
            log.warn("Rejected training with missing fields: {}", training);
            throw new IllegalArgumentException("All training fields except id are required");
        }
        if (training.getTrainingDuration().isNegative() || training.getTrainingDuration().isZero()) {
            log.warn("Rejected training '{}' with non-positive duration {}",
                    training.getTrainingName(), training.getTrainingDuration());
            throw new IllegalArgumentException("Training duration must be positive");
        }
    }
}
