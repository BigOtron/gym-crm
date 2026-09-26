package dao.implementations;

import io.gymcrm.dao.implementations.TrainingDaoImpl;
import io.gymcrm.entities.Training;
import io.gymcrm.entities.TrainingType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrainingDaoImplTest {

    private Map<UUID, Training> storage;
    private TrainingDaoImpl dao;

    @BeforeEach
    void setUp() {
        storage = new HashMap<>();
        dao = new TrainingDaoImpl();
        dao.setStorage(storage);
    }

    private static Training training(String name) {
        Training training = new Training();
        training.setTraineeId(UUID.randomUUID());
        training.setTrainerId(UUID.randomUUID());
        training.setTrainingName(name);
        training.setTrainingType(TrainingType.YOGA);
        training.setTrainingDate(LocalDate.of(2026, 9, 26));
        training.setTrainingDuration(Duration.ofMinutes(60));
        return training;
    }

    @Test
    void createAssignsIdAndStoresTraining() {
        Training training = training("Morning yoga");

        Training result = dao.create(training);

        assertSame(training, result);
        assertNotNull(result.getTrainingId());
        assertSame(training, storage.get(result.getTrainingId()));
    }

    @Test
    void createKeepsExistingId() {
        UUID id = UUID.randomUUID();
        Training training = training("Morning yoga");
        training.setTrainingId(id);

        dao.create(training);

        assertEquals(id, training.getTrainingId());
        assertSame(training, storage.get(id));
    }

    @Test
    void findByIdReturnsStoredTraining() {
        Training training = dao.create(training("Morning yoga"));

        Optional<Training> result = dao.findById(training.getTrainingId());

        assertTrue(result.isPresent());
        assertSame(training, result.get());
    }

    @Test
    void findByIdUnknownReturnsEmpty() {
        assertTrue(dao.findById(UUID.randomUUID()).isEmpty());
    }

    @Test
    void findAllReturnsUnmodifiableSnapshot() {
        dao.create(training("Morning yoga"));

        List<Training> all = dao.findAll();
        dao.create(training("Evening yoga"));

        assertEquals(1, all.size());
        Training extra = training("Extra");
        assertThrows(UnsupportedOperationException.class, () -> all.add(extra));
    }
}
