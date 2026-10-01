package io.gymcrm.services.implementations;

import io.gymcrm.dao.TraineeDao;
import io.gymcrm.dao.TrainerDao;
import io.gymcrm.dao.TrainingTypeDao;
import io.gymcrm.dto.TrainerRegistration;
import io.gymcrm.dto.TrainerUpdate;
import io.gymcrm.entities.Trainer;
import io.gymcrm.entities.TrainingType;
import io.gymcrm.entities.User;
import io.gymcrm.exceptions.NotFoundException;
import io.gymcrm.services.TrainerService;
import io.gymcrm.util.PasswordGenerator;
import io.gymcrm.util.UsernameGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

import static io.gymcrm.services.Validation.requireText;

@Slf4j
@Service
@Transactional
public class TrainerServiceImpl implements TrainerService {

    private TrainerDao trainerDao;
    private TrainingTypeDao trainingTypeDao;
    private TraineeDao traineeDao;
    private UsernameGenerator usernameGenerator;
    private PasswordGenerator passwordGenerator;

    @Autowired
    public void setTrainerDao(TrainerDao trainerDao) {
        this.trainerDao = trainerDao;
    }

    @Autowired
    public void setTrainingTypeDao(TrainingTypeDao trainingTypeDao) {
        this.trainingTypeDao = trainingTypeDao;
    }

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
    public Trainer create(TrainerRegistration registration) {
        Objects.requireNonNull(registration, "registration must not be null");
        String firstName = requireText(registration.firstName(), "First name");
        String lastName = requireText(registration.lastName(), "Last name");
        TrainingType specialization = findType(requireText(registration.specialization(), "Specialization"));
        log.debug("Creating trainer {} {} ({})", firstName, lastName, specialization.getTrainingTypeName());

        User user = new User(firstName, lastName,
                usernameGenerator.generate(firstName, lastName), passwordGenerator.generate(), true);
        Trainer trainer = new Trainer();
        trainer.setUser(user);
        trainer.setSpecialization(specialization);

        Trainer created = trainerDao.save(trainer);
        log.info("Trainer created: {} (id {}), specialization {}",
                user.getUsername(), created.getId(), specialization.getTrainingTypeName());
        return created;
    }

    @Override
    @Transactional(readOnly = true)
    public Trainer getByUsername(String username) {
        log.debug("Selecting trainer by username {}", username);
        return find(username);
    }

    @Override
    public void changePassword(String username, String newPassword) {
        String password = requireText(newPassword, "New password");
        find(username).getUser().setPassword(password);
        log.info("Trainer {} changed password", username);
    }

    @Override
    public Trainer update(String username, TrainerUpdate update) {
        Objects.requireNonNull(update, "update must not be null");
        String firstName = requireText(update.firstName(), "First name");
        String lastName = requireText(update.lastName(), "Last name");
        TrainingType specialization = findType(requireText(update.specialization(), "Specialization"));

        Trainer trainer = find(username);
        trainer.getUser().setFirstName(firstName);
        trainer.getUser().setLastName(lastName);
        trainer.setSpecialization(specialization);
        log.info("Trainer {} updated", username);
        return trainer;
    }

    @Override
    public boolean toggleActive(String username) {
        User user = find(username).getUser();
        user.setActive(!user.isActive());
        log.info("Trainer {} is now {}", username, user.isActive() ? "active" : "inactive");
        return user.isActive();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Trainer> getNotAssignedToTrainee(String traineeUsername) {
        if (traineeDao.findByUsername(traineeUsername).isEmpty()) {
            log.warn("Trainee not found by username {}", traineeUsername);
            throw new NotFoundException("Trainee not found: " + traineeUsername);
        }
        return trainerDao.findNotAssignedToTrainee(traineeUsername);
    }

    private Trainer find(String username) {
        return trainerDao.findByUsername(username).orElseThrow(() -> {
            log.warn("Trainer not found by username {}", username);
            return new NotFoundException("Trainer not found: " + username);
        });
    }

    private TrainingType findType(String name) {
        return trainingTypeDao.findByName(name).orElseThrow(() -> {
            log.warn("Training type not found: {}", name);
            return new NotFoundException("Training type not found: " + name);
        });
    }
}
