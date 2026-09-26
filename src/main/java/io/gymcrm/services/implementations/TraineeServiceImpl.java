package io.gymcrm.services.implementations;

import io.gymcrm.dao.TraineeDao;
import io.gymcrm.entities.Trainee;
import io.gymcrm.services.TraineeService;
import io.gymcrm.util.PasswordGenerator;
import io.gymcrm.util.UsernameGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
public class TraineeServiceImpl implements TraineeService {
    private TraineeDao traineeDao;
    private UsernameGenerator usernameGenerator;
    private PasswordGenerator passwordGenerator;

    @Autowired
    public void setTraineeDao(TraineeDao traineeDao) {
        this.traineeDao = traineeDao;
    }

    @Autowired
    public void setUsernameGenerator(UsernameGenerator usernameGenerator) {
        this.usernameGenerator = usernameGenerator;
    }

    @Autowired
    public void setPasswordGenerator(PasswordGenerator passwordGenerator) {
        this.passwordGenerator = passwordGenerator;
    }

    @Override
    public Trainee create(Trainee trainee) {
        log.debug("Creating trainee {} {}", trainee == null ? null : trainee.getFirstName(),
                trainee == null ? null : trainee.getLastName());
        validate(trainee);
        trainee.setUserId(null);
        trainee.setUsername(usernameGenerator.generate(trainee.getFirstName(), trainee.getLastName()));
        trainee.setPassword(passwordGenerator.generate());
        trainee.setActive(true);

        Trainee created = traineeDao.create(trainee);
        log.info("Trainee created: {} (id {})", created.getUsername(), created.getUserId());
        return created;
    }

    @Override
    public Trainee update(Trainee trainee) {
        log.debug("Updating trainee with id {}", trainee == null ? null : trainee.getUserId());
        validate(trainee);
        Trainee existing = getById(trainee.getUserId());

        trainee.setUsername(existing.getUsername());
        trainee.setPassword(existing.getPassword());

        Trainee updated = traineeDao.update(trainee);
        log.info("Trainee updated: {} (id {})", updated.getUsername(), updated.getUserId());
        return updated;
    }

    @Override
    public void delete(UUID userId) {
        log.debug("Deleting trainee with id {}", userId);
        traineeDao.delete(userId);
        log.info("Trainee deleted: {}", userId);
    }

    @Override
    public Trainee getById(UUID userId) {
        log.debug("Selecting trainee by id {}", userId);
        return traineeDao.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("No trainee found with id " + userId));
    }

    @Override
    public Trainee getByUsername(String username) {
        log.debug("Selecting trainee by username {}", username);
        return traineeDao.findByUsername(username);
    }

    @Override
    public List<Trainee> getAll() {
        List<Trainee> all = traineeDao.findAll();
        log.debug("Selected all trainees ({} records)", all.size());
        return all;
    }

    private void validate(Trainee trainee) {
        Objects.requireNonNull(trainee, "trainee must not be null");
        if (isBlank(trainee.getFirstName()) || isBlank(trainee.getLastName())) {
            log.warn("Rejected trainee without first or last name (id {})", trainee.getUserId());
            throw new IllegalArgumentException("First name and last name are required");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
