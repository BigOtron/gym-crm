package io.gymcrm.services.implementations;

import io.gymcrm.dao.TraineeDao;
import io.gymcrm.dao.TrainerDao;
import io.gymcrm.dao.UserDao;
import io.gymcrm.dto.Credentials;
import io.gymcrm.entities.User;
import io.gymcrm.exceptions.AuthenticationException;
import io.gymcrm.services.AuthenticationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;

@Slf4j
@Service
@Transactional(readOnly = true)
public class AuthenticationServiceImpl implements AuthenticationService {

    private UserDao userDao;
    private TraineeDao traineeDao;
    private TrainerDao trainerDao;

    @Autowired
    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }

    @Autowired
    public void setTraineeDao(TraineeDao traineeDao) {
        this.traineeDao = traineeDao;
    }

    @Autowired
    public void setTrainerDao(TrainerDao trainerDao) {
        this.trainerDao = trainerDao;
    }

    @Override
    public boolean traineeCredentialsMatch(Credentials credentials) {
        return isComplete(credentials) && traineeDao.findByUsername(credentials.username())
                .map(trainee -> passwordMatches(trainee.getUser(), credentials.password()))
                .orElse(false);
    }

    @Override
    public boolean trainerCredentialsMatch(Credentials credentials) {
        return isComplete(credentials) && trainerDao.findByUsername(credentials.username())
                .map(trainer -> passwordMatches(trainer.getUser(), credentials.password()))
                .orElse(false);
    }

    @Override
    public void authenticateTrainee(Credentials credentials) {
        if (!traineeCredentialsMatch(credentials)) {
            reject(credentials, "trainee");
        }
        log.debug("Trainee {} authenticated", credentials.username());
    }

    @Override
    public void authenticateTrainer(Credentials credentials) {
        if (!trainerCredentialsMatch(credentials)) {
            reject(credentials, "trainer");
        }
        log.debug("Trainer {} authenticated", credentials.username());
    }

    @Override
    public void authenticate(Credentials credentials) {
        boolean matches = isComplete(credentials) && userDao.findByUsername(credentials.username())
                .map(user -> passwordMatches(user, credentials.password()))
                .orElse(false);
        if (!matches) {
            reject(credentials, "user");
        }
        log.debug("User {} authenticated", credentials.username());
    }

    private static boolean isComplete(Credentials credentials) {
        return credentials != null && credentials.username() != null && credentials.password() != null;
    }

    private static boolean passwordMatches(User user, String password) {
        return MessageDigest.isEqual(
                user.getPassword().getBytes(StandardCharsets.UTF_8),
                password.getBytes(StandardCharsets.UTF_8));
    }

    private static void reject(Credentials credentials, String role) {
        String username = Optional.ofNullable(credentials).map(Credentials::username).orElse(null);
        log.warn("Authentication failed for {} {}", role, username);
        throw new AuthenticationException("Invalid username or password");
    }
}
