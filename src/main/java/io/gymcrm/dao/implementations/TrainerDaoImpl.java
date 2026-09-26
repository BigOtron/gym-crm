package io.gymcrm.dao.implementations;

import io.gymcrm.config.StorageNames;
import io.gymcrm.dao.TrainerDao;
import io.gymcrm.entities.Trainer;
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
public class TrainerDaoImpl implements TrainerDao {

    private Map<UUID, Trainer> storage;

    @Autowired
    public void setStorage(@Qualifier(StorageNames.TRAINER_STORAGE) Map<UUID, Trainer> storage) {
        this.storage = storage;
        log.debug("Trainer storage injected ({} records)", storage.size());
    }

    @Override
    public Trainer create(Trainer trainer) {
        if (trainer.getUserId() == null) {
            trainer.setUserId(UUID.randomUUID());
        }
        storage.put(trainer.getUserId(), trainer);
        log.debug("Stored trainer {} with id {}", trainer.getUsername(), trainer.getUserId());
        return trainer;
    }

    @Override
    public Trainer update(Trainer trainer) {
        UUID id = trainer.getUserId();
        if (id == null || !storage.containsKey(id)) {
            log.debug("Update failed, trainer not found: {}", id);
            throw new NoSuchElementException("Trainer not found: " + id);
        }
        storage.put(id, trainer);
        log.debug("Updated trainer with id {}", id);
        return trainer;
    }

    @Override
    public Optional<Trainer> findById(UUID userId) {
        Optional<Trainer> trainer = Optional.ofNullable(storage.get(userId));
        log.debug("Lookup trainer by id {}: {}", userId, trainer.isPresent() ? "found" : "not found");
        return trainer;
    }

    @Override
    public Optional<Trainer> findByUsername(String username) {
        Optional<Trainer> trainer = storage.values().stream()
                .filter(t -> username.equals(t.getUsername()))
                .findFirst();
        log.debug("Lookup trainer by username {}: {}", username, trainer.isPresent() ? "found" : "not found");
        return trainer;
    }

    @Override
    public List<Trainer> findAll() {
        List<Trainer> all = List.copyOf(storage.values());
        log.debug("Fetched all trainers ({} records)", all.size());
        return all;
    }
}
