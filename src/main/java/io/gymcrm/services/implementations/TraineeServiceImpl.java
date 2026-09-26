package io.gymcrm.services.implementations;

import io.gymcrm.dao.TraineeDao;
import io.gymcrm.entities.Trainee;
import io.gymcrm.services.TraineeService;
import io.gymcrm.util.PasswordGenerator;
import io.gymcrm.util.UsernameGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class TraineeServiceImpl implements TraineeService {
    private TraineeDao traineeDao;
    private UsernameGenerator usernameGenerator;
    private final PasswordGenerator passwordGenerator = new PasswordGenerator();

    @Autowired
    public void setTraineeDao(TraineeDao traineeDao) {
        this.traineeDao = traineeDao;
    }

    @Autowired
    public void setUsernameGenerator(UsernameGenerator usernameGenerator) {
        this.usernameGenerator = usernameGenerator;
    }

    @Override
    public Trainee create(Trainee trainee) {
        validate(trainee);
        trainee.setUserId(null);
        trainee.setUsername(usernameGenerator.generate(trainee.getFirstName(), trainee.getLastName()));
        trainee.setPassword(passwordGenerator.generate());
        trainee.setActive(true);

        return traineeDao.create(trainee);
    }

    @Override
    public Trainee update(Trainee trainee) {
        validate(trainee);
        Trainee existing = getById(trainee.getUserId());

        trainee.setUsername(existing.getUsername());
        trainee.setPassword(existing.getPassword());

        return traineeDao.update(trainee);
    }

    @Override
    public void delete(UUID userId) {
        traineeDao.delete(userId);
    }

    @Override
    public Trainee getById(UUID userId) {
        return traineeDao.findById(userId);
    }

    @Override
    public Trainee getByUsername(String username) {
        return traineeDao.findByUsername(username);
    }

    @Override
    public List<Trainee> getAll() {
        return traineeDao.findAll();
    }

    private void validate(Trainee trainee) {
        Objects.requireNonNull(trainee, "trainee must not be null");
        if (isBlank(trainee.getFirstName()) || isBlank(trainee.getLastName())) {
            throw new IllegalArgumentException("First name and last name are required");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
