package io.gymcrm.services.implementations;

import io.gymcrm.dao.TrainerDao;
import io.gymcrm.entities.Trainer;
import io.gymcrm.services.TrainerService;
import io.gymcrm.util.PasswordGenerator;
import io.gymcrm.util.UsernameGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;

@Service
public class TrainerServiceImpl implements TrainerService {
    private TrainerDao trainerDao;
    private UsernameGenerator usernameGenerator;
    private final PasswordGenerator passwordGenerator = new PasswordGenerator();

    @Autowired
    public void setTrainerDao(TrainerDao trainerDao) {
        this.trainerDao = trainerDao;
    }

    @Autowired
    public void setUsernameGenerator(UsernameGenerator usernameGenerator) {
        this.usernameGenerator = usernameGenerator;
    }

    @Override
    public Trainer create(Trainer trainer) {
        validate(trainer);
        trainer.setUserId(null);
        trainer.setUsername(usernameGenerator.generate(trainer.getFirstName(), trainer.getLastName()));
        trainer.setPassword(passwordGenerator.generate());
        trainer.setActive(true);

        return trainerDao.create(trainer);
    }

    @Override
    public Trainer update(Trainer trainer) {
        validate(trainer);
        Trainer existing = getById(trainer.getUserId());

        trainer.setUsername(existing.getUsername());
        trainer.setPassword(existing.getPassword());

        return trainerDao.update(trainer);
    }

    @Override
    public Trainer getById(UUID userId) {
        return trainerDao.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("No trainer with id " + userId));
    }

    @Override
    public Trainer getByUsername(String username) {
        return trainerDao.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("No trainer with id " + username));
    }

    @Override
    public List<Trainer> getAll() {
        return trainerDao.findAll();
    }
    private void validate(Trainer trainer) {
        Objects.requireNonNull(trainer, "trainer must not be null");
        if (isBlank(trainer.getFirstName()) || isBlank(trainer.getLastName())) {
            throw new IllegalArgumentException("First name and last name are required");
        }
        if (trainer.getSpecialization().isEmpty()) {
            throw new IllegalArgumentException("Trainer must have at least one specialization");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
