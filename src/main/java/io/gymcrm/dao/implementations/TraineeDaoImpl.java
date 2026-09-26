package io.gymcrm.dao.implementations;

import io.gymcrm.config.StorageNames;
import io.gymcrm.dao.TraineeDao;
import io.gymcrm.entities.Trainee;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;


@Repository
public class TraineeDaoImpl implements TraineeDao {

    private Map<UUID, Trainee> storage;

    @Autowired
    public void setStorage(@Qualifier(StorageNames.TRAINEE_STORAGE) Map<UUID, Trainee> storage) {
        this.storage = storage;
    }

    @Override
    public Trainee create(Trainee trainee) {
        if (trainee.getUserId() == null) {
            trainee.setUserId(UUID.randomUUID());
        }
        storage.put(trainee.getUserId(), trainee);
        return trainee;
    }

    @Override
    public Trainee update(Trainee trainee) {
        UUID id = trainee.getUserId();
        if (id == null || !storage.containsKey(id)) {
            throw new NoSuchElementException("Trainee not found: " + id);
        }
        storage.put(id, trainee);
        return trainee;
    }

    @Override
    public boolean delete(UUID userId) {
        return storage.remove(userId) != null;
    }

    @Override
    public Optional<Trainee> findById(UUID userId) {
        return Optional.ofNullable(storage.get(userId));
    }

    @Override
    public Optional<Trainee> findByUsername(String username) {
        return storage.values().stream()
                .filter(t -> t.getUsername().equals(username))
                .findFirst();
    }

    @Override
    public List<Trainee> findAll() {
        return List.copyOf(storage.values());
    }
}