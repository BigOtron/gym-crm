package io.gymcrm.dao.implementations;

import io.gymcrm.config.StorageNames;
import io.gymcrm.dao.TrainingDao;
import io.gymcrm.entities.Training;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TrainingDaoImpl implements TrainingDao {
    private Map<UUID, Training> storage;

    @Autowired
    public void setStorage(@Qualifier(StorageNames.TRAINING_STORAGE) Map<UUID, Training> storage) {
        this.storage = storage;
    }

    @Override
    public Training create(Training training) {
        if (training.getTrainingId() == null) {
            training.setTrainingId(UUID.randomUUID());
        }
        storage.put(training.getTrainingId(), training);
        return training;
    }

    @Override
    public Optional<Training> findById(UUID trainingId) {
        return Optional.ofNullable(storage.get(trainingId));
    }

    @Override
    public List<Training> findAll() {
        return List.copyOf(storage.values());
    }
}
