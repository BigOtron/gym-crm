package io.gymcrm.dao.implementations;

import io.gymcrm.config.StorageNames;
import io.gymcrm.dao.TrainerDao;
import io.gymcrm.entities.Trainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TrainerDaoImpl implements TrainerDao {

    private Map<UUID, Trainer> storage;

    @Autowired
    public void setStorage(@Qualifier(StorageNames.TRAINER_STORAGE) Map<UUID, Trainer> storage) {
        this.storage = storage;
    }

    @Override
    public Trainer create(Trainer trainer) {
        if (trainer.getUserId() == null) {
            trainer.setUserId(UUID.randomUUID());
        }
        return storage.put(trainer.getUserId(), trainer);
    }

    @Override
    public Trainer update(Trainer trainer) {
        UUID id = trainer.getUserId();
        if (id == null || !storage.containsKey(id)) {
            throw new NoSuchElementException("Trainer not found: " + id);
        }
        return storage.put(id, trainer);
    }

    @Override
    public Optional<Trainer> findById(UUID userId) {
        return Optional.ofNullable(storage.get(userId));
    }

    @Override
    public Optional<Trainer> findByUsername(String username) {
        return storage.values().stream()
                .filter(t -> t.getUsername().equals(username))
                .findFirst();
    }

    @Override
    public List<Trainer> findAll() {
        return List.copyOf(storage.values());
    }
}
