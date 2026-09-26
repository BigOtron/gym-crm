package io.gymcrm.dao.implementations;

import io.gymcrm.config.StorageNames;
import io.gymcrm.dao.TrainingDao;
import io.gymcrm.entities.Training;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Repository
public class TrainingDaoImpl implements TrainingDao {

    private Map<UUID, Training> storage;

    @Autowired
    public void setStorage(@Qualifier(StorageNames.TRAINING_STORAGE) Map<UUID, Training> storage) {
        this.storage = storage;
        log.debug("Training storage injected ({} records)", storage.size());
    }

    @Override
    public Training create(Training training) {
        if (training.getTrainingId() == null) {
            training.setTrainingId(UUID.randomUUID());
        }
        storage.put(training.getTrainingId(), training);
        log.debug("Stored training '{}' with id {}", training.getTrainingName(), training.getTrainingId());
        return training;
    }

    @Override
    public Optional<Training> findById(UUID trainingId) {
        Optional<Training> training = Optional.ofNullable(storage.get(trainingId));
        log.debug("Lookup training by id {}: {}", trainingId, training.isPresent() ? "found" : "not found");
        return training;
    }

    @Override
    public List<Training> findAll() {
        List<Training> all = List.copyOf(storage.values());
        log.debug("Fetched all trainings ({} records)", all.size());
        return all;
    }
}
