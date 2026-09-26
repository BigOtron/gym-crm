import io.gymcrm.config.AppConfig;
import io.gymcrm.config.StorageNames;
import io.gymcrm.entities.Trainee;
import io.gymcrm.entities.Trainer;
import io.gymcrm.entities.Training;
import io.gymcrm.entities.TrainingType;
import io.gymcrm.facade.GymFacade;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GymCrmIntegrationTest {

    private static final UUID JOHN_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID ANNA_ID = UUID.fromString("33333333-3333-4333-8333-333333333333");
    private static final UUID DAVID_ID = UUID.fromString("44444444-4444-4444-8444-444444444444");

    private AnnotationConfigApplicationContext context;
    private GymFacade facade;

    @BeforeEach
    void startContext() {
        context = new AnnotationConfigApplicationContext(AppConfig.class);
        facade = context.getBean(GymFacade.class);
    }

    @AfterEach
    void closeContext() {
        context.close();
    }

    private static Training training(UUID traineeId, UUID trainerId, TrainingType type) {
        Training training = new Training();
        training.setTraineeId(traineeId);
        training.setTrainerId(trainerId);
        training.setTrainingName("Integration session");
        training.setTrainingType(type);
        training.setTrainingDate(LocalDate.of(2026, 10, 1));
        training.setTrainingDuration(Duration.ofMinutes(45));
        return training;
    }

    @Test
    void seedDataIsLoadedAtStartup() {
        assertEquals(2, facade.getAllTrainees().size());
        assertEquals(2, facade.getAllTrainers().size());
        assertEquals(2, facade.getAllTrainings().size());
        assertEquals("John", facade.getTraineeByUsername("John.Smith").getFirstName());
    }

    @Test
    void eachEntityTypeHasItsOwnStorageBean() {
        Map<?, ?> trainees = context.getBean(StorageNames.TRAINEE_STORAGE, Map.class);
        Map<?, ?> trainers = context.getBean(StorageNames.TRAINER_STORAGE, Map.class);
        Map<?, ?> trainings = context.getBean(StorageNames.TRAINING_STORAGE, Map.class);

        assertNotSame(trainees, trainers);
        assertNotSame(trainers, trainings);
        assertTrue(trainees.containsKey(JOHN_ID));
        assertTrue(trainers.containsKey(ANNA_ID));
    }

    @Test
    void createdTraineeGetsSerialWhenNameIsTaken() {
        Trainee trainee = new Trainee();
        trainee.setFirstName("John");
        trainee.setLastName("Smith");

        Trainee created = facade.createTrainee(trainee);

        assertEquals("John.Smith1", created.getUsername());
        assertEquals(10, created.getPassword().length());
        assertTrue(created.isActive());
        assertNotNull(created.getUserId());
        assertEquals(created, facade.getTraineeById(created.getUserId()));
    }

    @Test
    void createdTrainerNameClashesWithTrainees() {
        Trainer trainer = new Trainer();
        trainer.setFirstName("John");
        trainer.setLastName("Smith");
        trainer.addSpecialization(TrainingType.RUNNING);

        Trainer created = facade.createTrainer(trainer);

        assertEquals("John.Smith1", created.getUsername());
    }

    @Test
    void trainingCanBeCreatedForSeededUsers() {
        Training created = facade.createTraining(training(JOHN_ID, ANNA_ID, TrainingType.YOGA));

        assertNotNull(created.getTrainingId());
        assertEquals(3, facade.getAllTrainings().size());
        assertEquals(created, facade.getTrainingById(created.getTrainingId()));
    }

    @Test
    void trainingIsRejectedWhenTrainerDoesNotTeachType() {
        Training training = training(JOHN_ID, DAVID_ID, TrainingType.YOGA);

        assertThrows(IllegalArgumentException.class, () -> facade.createTraining(training));
        assertEquals(2, facade.getAllTrainings().size());
    }

    @Test
    void updatedTraineeKeepsCredentials() {
        Trainee changes = new Trainee();
        changes.setUserId(JOHN_ID);
        changes.setFirstName("John");
        changes.setLastName("Smith");
        changes.setAddress("New address");
        changes.setUsername("Other.Name");

        facade.updateTrainee(changes);

        Trainee stored = facade.getTraineeById(JOHN_ID);
        assertEquals("New address", stored.getAddress());
        assertEquals("John.Smith", stored.getUsername());
        assertEquals("aB3dE5fG7h", stored.getPassword());
    }

    @Test
    void deletedTraineeCanNoLongerBeFound() {
        facade.deleteTrainee(JOHN_ID);

        assertThrows(NoSuchElementException.class, () -> facade.getTraineeById(JOHN_ID));
        assertEquals(1, facade.getAllTrainees().size());
    }
}
