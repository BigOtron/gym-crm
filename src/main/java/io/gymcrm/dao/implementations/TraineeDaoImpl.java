package io.gymcrm.dao.implementations;

import io.gymcrm.config.StorageNames;
import io.gymcrm.dao.TraineeDao;
import io.gymcrm.entities.Trainee;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Repository
public class TraineeDaoImpl implements TraineeDao {

    private Map<UUID, Trainee> storage;

    @Autowired
    public void setStorage(@Qualifier(StorageNames.TRAINEE_STORAGE) Map<UUID, Trainee> storage) {
        this.storage = storage;
        log.debug("Trainee storage injected ({} records)", storage.size());
    }

    @Override
    public Trainee create(Trainee trainee) {
        if (trainee.getUserId() == null) {
            trainee.setUserId(UUID.randomUUID());
        }
        storage.put(trainee.getUserId(), trainee);
        log.debug("Stored trainee {} with id {}", trainee.getUsername(), trainee.getUserId());
        return trainee;
    }

    @Override
    public Trainee update(Trainee trainee) {
        UUID id = trainee.getUserId();
        if (id == null || !storage.containsKey(id)) {
            log.debug("Update failed, trainee not found: {}", id);
            throw new NoSuchElementException("Trainee not found: " + id);
        }
        storage.put(id, trainee);
        log.debug("Updated trainee with id {}", id);
        return trainee;
    }

    @Override
    public void delete(UUID userId) {
        if (storage.remove(userId) == null) {
            log.debug("Delete failed, trainee not found: {}", userId);
            throw new NoSuchElementException("Trainee not found: " + userId);
        }
        log.debug("Removed trainee with id {}", userId);
    }

    @Override
    public Optional<Trainee> findById(UUID userId) {
        return Optional.ofNullable(storage.get(userId));
    }

    @Override
    public Optional<Trainee> findByUsername(String username) {
        Optional<Trainee> trainee = storage.values().stream()
                .filter(t -> username.equals(t.getUsername()))
                .findFirst();
        log.debug("Found trainee by username {}", username);
        return trainee;
    }

    @Override
    public List<Trainee> findAll() {
        List<Trainee> all = List.copyOf(storage.values());
        log.debug("Fetched all trainees ({} records)", all.size());
        return all;
    }
}