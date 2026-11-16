package io.gymcrm.services.implementations;

import io.gymcrm.dao.TraineeDao;
import io.gymcrm.dao.TrainerDao;
import io.gymcrm.dto.CreatedProfile;
import io.gymcrm.dto.TraineeRegistration;
import io.gymcrm.dto.TraineeUpdate;
import io.gymcrm.entities.Trainee;
import io.gymcrm.entities.Trainer;
import io.gymcrm.entities.User;
import io.gymcrm.exceptions.ConflictException;
import io.gymcrm.exceptions.NotFoundException;
import io.gymcrm.metrics.GymMetrics;
import io.gymcrm.security.JwtTokenService;
import io.gymcrm.security.Role;
import io.gymcrm.services.TraineeService;
import io.gymcrm.util.PasswordGenerator;
import io.gymcrm.util.UsernameGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static io.gymcrm.services.Validation.requireText;
import static io.gymcrm.services.Validation.requireValue;

@Slf4j
@Service
@Transactional
@PreAuthorize("isAuthenticated()")
public class TraineeServiceImpl implements TraineeService {

    /** Only the trainee who owns the profile may change it. */
    private static final String OWNER = "hasRole('TRAINEE') and #username == authentication.name";

    private TraineeDao traineeDao;
    private GymMetrics metrics;
    private TrainerDao trainerDao;
    private UsernameGenerator usernameGenerator;
    private PasswordGenerator passwordGenerator;
    private PasswordEncoder passwordEncoder;
    private JwtTokenService tokenService;

    @Autowired
    public void setMetrics(GymMetrics metrics) {
        this.metrics = metrics;
    }

    @Autowired
    public void setTraineeDao(TraineeDao traineeDao) {
        this.traineeDao = traineeDao;
    }

    @Autowired
    public void setTrainerDao(TrainerDao trainerDao) {
        this.trainerDao = trainerDao;
    }

    @Autowired
    public void setUsernameGenerator(UsernameGenerator usernameGenerator) {
        this.usernameGenerator = usernameGenerator;
    }

    @Autowired
    public void setTokenService(JwtTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Autowired
    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Autowired
    public void setPasswordGenerator(PasswordGenerator passwordGenerator) {
        this.passwordGenerator = passwordGenerator;
    }

    @Override
    @PreAuthorize("permitAll()")
    public CreatedProfile<Trainee> create(TraineeRegistration registration) {
        Objects.requireNonNull(registration, "registration must not be null");
        String firstName = requireText(registration.firstName(), "First name");
        String lastName = requireText(registration.lastName(), "Last name");
        log.debug("Creating trainee {} {}", firstName, lastName);

        String password = passwordGenerator.generate();
        User user = new User(firstName, lastName,
                usernameGenerator.generate(firstName, lastName), passwordEncoder.encode(password), true);
        Trainee trainee = new Trainee();
        trainee.setUser(user);
        trainee.setDateOfBirth(registration.dateOfBirth());
        trainee.setAddress(registration.address());

        Trainee created = traineeDao.save(trainee);
        log.info("Trainee created: {} (id {})", user.getUsername(), created.getId());
        metrics.userRegistered(GymMetrics.TRAINEE);
        return new CreatedProfile<>(created, password, tokenService.issue(user.getUsername(), Role.TRAINEE));
    }

    @Override
    @Transactional(readOnly = true)
    public Trainee getByUsername(String username) {
        log.debug("Selecting trainee by username {}", username);
        return find(username);
    }

    @Override
    @PreAuthorize(OWNER)
    public Trainee update(String username, TraineeUpdate update) {
        Objects.requireNonNull(update, "update must not be null");
        String firstName = requireText(update.firstName(), "First name");
        String lastName = requireText(update.lastName(), "Last name");
        boolean active = requireValue(update.isActive(), "Is active");

        Trainee trainee = find(username);
        trainee.getUser().setFirstName(firstName);
        trainee.getUser().setLastName(lastName);
        trainee.setDateOfBirth(update.dateOfBirth());
        trainee.setAddress(update.address());
        trainee.getUser().setActive(active);
        log.info("Trainee {} updated", username);
        return trainee;
    }

    @Override
    @PreAuthorize(OWNER)
    public void setActive(String username, boolean active) {
        User user = find(username).getUser();
        String state = active ? "active" : "inactive";
        if (user.isActive() == active) {
            log.warn("Trainee {} is already {}", username, state);
            throw new ConflictException("Trainee " + username + " is already " + state);
        }
        user.setActive(active);
        log.info("Trainee {} is now {}", username, state);
    }

    @Override
    @PreAuthorize(OWNER)
    public void delete(String username) {
        Trainee trainee = find(username);
        int trainings = trainee.getTrainings().size();
        traineeDao.delete(trainee);
        log.info("Trainee {} deleted together with {} trainings", username, trainings);
    }

    @Override
    @PreAuthorize(OWNER)
    public List<Trainer> updateTrainers(String username, List<String> trainerUsernames) {
        Objects.requireNonNull(trainerUsernames, "trainerUsernames must not be null");
        Set<String> requested = new LinkedHashSet<>(trainerUsernames);
        Trainee trainee = find(username);

        List<Trainer> trainers = trainerDao.findByUsernames(requested);
        if (trainers.size() != requested.size()) {
            Set<String> found = trainers.stream()
                    .map(trainer -> trainer.getUser().getUsername())
                    .collect(Collectors.toSet());
            requested.removeAll(found);
            log.warn("Trainee {} trainers update rejected, unknown trainers {}", username, requested);
            throw new NotFoundException("Trainers not found: " + requested);
        }

        trainee.getTrainers().clear();
        trainee.getTrainers().addAll(trainers);
        log.info("Trainee {} now has trainers {}", username, trainerUsernames);
        return List.copyOf(trainers);
    }

    private Trainee find(String username) {
        return traineeDao.findByUsername(username).orElseThrow(() -> {
            log.warn("Trainee not found by username {}", username);
            return new NotFoundException("Trainee not found: " + username);
        });
    }
}
