package io.gymcrm.services.implementations;

import io.gymcrm.dao.TrainerDao;
import io.gymcrm.entities.Trainer;
import io.gymcrm.services.TrainerService;
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
public class TrainerServiceImpl implements TrainerService {
    private TrainerDao trainerDao;
    private UsernameGenerator usernameGenerator;
    private PasswordGenerator passwordGenerator;

    @Autowired
    public void setTrainerDao(TrainerDao trainerDao) {
        this.trainerDao = trainerDao;
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
    public Trainer create(Trainer trainer) {
        log.debug("Creating trainer {} {}", trainer == null ? null : trainer.getFirstName(),
                trainer == null ? null : trainer.getLastName());
        validate(trainer);
        trainer.setUserId(null);
        trainer.setUsername(usernameGenerator.generate(trainer.getFirstName(), trainer.getLastName()));
        trainer.setPassword(passwordGenerator.generate());
        trainer.setActive(true);

        Trainer created = trainerDao.create(trainer);
        log.info("Trainer created: {} (id {}), specialization {}",
                created.getUsername(), created.getUserId(), created.getSpecialization());
        return created;
    }

    @Override
    public Trainer update(Trainer trainer) {
        log.debug("Updating trainer with id {}", trainer == null ? null : trainer.getUserId());
        validate(trainer);
        Trainer existing = getById(trainer.getUserId());

        trainer.setUsername(existing.getUsername());
        trainer.setPassword(existing.getPassword());

        Trainer updated = trainerDao.update(trainer);
        log.info("Trainer updated: {} (id {})", updated.getUsername(), updated.getUserId());
        return updated;
    }

    @Override
    public Trainer getById(UUID userId) {
        log.debug("Selecting trainer by id {}", userId);
        return trainerDao.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Trainer not found by id {}", userId);
                    return new NoSuchElementException("No trainer with id " + userId);
                });
    }

    @Override
    public Trainer getByUsername(String username) {
        log.debug("Selecting trainer by username {}", username);
        return trainerDao.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("Trainer not found by username {}", username);
                    return new NoSuchElementException("No trainer with username " + username);
                });
    }

    @Override
    public List<Trainer> getAll() {
        List<Trainer> all = trainerDao.findAll();
        log.debug("Selected all trainers ({} records)", all.size());
        return all;
    }

    private void validate(Trainer trainer) {
        Objects.requireNonNull(trainer, "trainer must not be null");
        if (isBlank(trainer.getFirstName()) || isBlank(trainer.getLastName())) {
            log.warn("Rejected trainer without first or last name (id {})", trainer.getUserId());
            throw new IllegalArgumentException("First name and last name are required");
        }
        if (trainer.getSpecialization().isEmpty()) {
            log.warn("Rejected trainer {} {} without specialization",
                    trainer.getFirstName(), trainer.getLastName());
            throw new IllegalArgumentException("Trainer must have at least one specialization");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
