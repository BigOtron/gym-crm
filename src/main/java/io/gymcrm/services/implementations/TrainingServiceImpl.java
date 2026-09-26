package io.gymcrm.services.implementations;

import io.gymcrm.dao.TraineeDao;
import io.gymcrm.dao.TrainerDao;
import io.gymcrm.dao.TrainingDao;
import io.gymcrm.entities.Trainer;
import io.gymcrm.entities.Training;
import io.gymcrm.services.TrainingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;

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
        validateFields(training);

        if (traineeDao.findById(training.getTraineeId()) == null) {
            throw new NoSuchElementException("Trainee not found: " + training.getTraineeId());
        }
        Trainer trainer = trainerDao.findById(training.getTrainerId())
                .orElseThrow(() -> new NoSuchElementException("Trainer not found: " + training.getTrainerId()));

        if (!trainer.getSpecialization().contains(training.getTrainingType())) {
            throw new IllegalArgumentException("Trainer " + trainer.getUsername()
                    + " does not teach " + training.getTrainingType());
        }

        training.setTrainingId(null);
        return trainingDao.create(training);
    }

    @Override
    public Training getById(UUID trainingId) {
        return trainingDao.findById(trainingId)
                .orElseThrow(() -> new NoSuchElementException("Training not found: " + trainingId));
    }

    @Override
    public List<Training> getAll() {
        return trainingDao.findAll();
    }

    private void validateFields(Training training) {
        Objects.requireNonNull(training, "training must not be null");
        if (training.getTraineeId() == null || training.getTrainerId() == null
                || training.getTrainingType() == null || training.getTrainingDate() == null
                || training.getTrainingDuration() == null
                || training.getTrainingName() == null || training.getTrainingName().isBlank()) {
            throw new IllegalArgumentException("All training fields except id are required");
        }
        if (training.getTrainingDuration().isNegative() || training.getTrainingDuration().isZero()) {
            throw new IllegalArgumentException("Training duration must be positive");
        }
    }
}
