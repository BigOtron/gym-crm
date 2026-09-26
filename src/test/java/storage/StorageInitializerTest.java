package storage;

import io.gymcrm.config.StorageNames;
import io.gymcrm.entities.Trainee;
import io.gymcrm.entities.Trainer;
import io.gymcrm.entities.Training;
import io.gymcrm.entities.TrainingType;
import io.gymcrm.storage.StorageInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StorageInitializerTest {

    private static final UUID JOHN_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID ANNA_ID = UUID.fromString("33333333-3333-4333-8333-333333333333");
    private static final UUID EVENING_YOGA_ID = UUID.fromString("55555555-5555-4555-8555-555555555555");

    private StorageInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new StorageInitializer();
        initializer.setTraineeFile("classpath:test-data/trainees.json");
        initializer.setTrainerFile("classpath:test-data/trainers.json");
        initializer.setTrainingFile("classpath:test-data/trainings.json");
    }

    @Test
    void loadsTraineesIntoTraineeStorage() {
        Map<UUID, Trainee> storage = new HashMap<>();

        Object result = initializer.postProcessAfterInitialization(storage, StorageNames.TRAINEE_STORAGE);

        assertSame(storage, result);
        assertEquals(2, storage.size());
        Trainee john = storage.get(JOHN_ID);
        assertEquals("John.Smith", john.getUsername());
        assertEquals(LocalDate.of(1995, 4, 12), john.getDateOfBirth());
        assertTrue(john.isActive());
    }

    @Test
    void loadsTrainersWithSpecializations() {
        Map<UUID, Trainer> storage = new HashMap<>();

        initializer.postProcessAfterInitialization(storage, StorageNames.TRAINER_STORAGE);

        assertEquals(2, storage.size());
        assertEquals(Set.of(TrainingType.YOGA, TrainingType.FITNESS), storage.get(ANNA_ID).getSpecialization());
    }

    @Test
    void loadsTrainingsWithDateAndDuration() {
        Map<UUID, Training> storage = new HashMap<>();

        initializer.postProcessAfterInitialization(storage, StorageNames.TRAINING_STORAGE);

        assertEquals(2, storage.size());
        Training training = storage.get(EVENING_YOGA_ID);
        assertEquals(JOHN_ID, training.getTraineeId());
        assertEquals(ANNA_ID, training.getTrainerId());
        assertEquals(TrainingType.YOGA, training.getTrainingType());
        assertEquals(LocalDate.of(2026, 9, 20), training.getTrainingDate());
        assertEquals(Duration.ofHours(1), training.getTrainingDuration());
    }

    @Test
    void leavesOtherBeansUntouched() {
        Map<UUID, Trainee> unrelated = new HashMap<>();

        Object result = initializer.postProcessAfterInitialization(unrelated, "someOtherBean");

        assertSame(unrelated, result);
        assertTrue(unrelated.isEmpty());
    }

    @Test
    void missingFileLeavesStorageEmpty() {
        initializer.setTraineeFile("classpath:test-data/does-not-exist.json");
        Map<UUID, Trainee> storage = new HashMap<>();

        initializer.postProcessAfterInitialization(storage, StorageNames.TRAINEE_STORAGE);

        assertTrue(storage.isEmpty());
    }

    @Test
    void malformedFileFailsStartup() {
        initializer.setTraineeFile("classpath:test-data/broken.json");
        Map<UUID, Trainee> storage = new HashMap<>();

        assertThrows(IllegalStateException.class,
                () -> initializer.postProcessAfterInitialization(storage, StorageNames.TRAINEE_STORAGE));
    }

    @Test
    void fileWithWrongEntityFailsStartup() {
        initializer.setTraineeFile("classpath:test-data/trainings.json");
        Map<UUID, Trainee> storage = new HashMap<>();

        assertThrows(IllegalStateException.class,
                () -> initializer.postProcessAfterInitialization(storage, StorageNames.TRAINEE_STORAGE));
    }
}
