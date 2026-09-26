package dao.implementations;

import io.gymcrm.dao.implementations.TrainerDaoImpl;
import io.gymcrm.entities.Trainer;
import io.gymcrm.entities.TrainingType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrainerDaoImplTest {

    private Map<UUID, Trainer> storage;
    private TrainerDaoImpl dao;

    @BeforeEach
    void setUp() {
        storage = new HashMap<>();
        dao = new TrainerDaoImpl();
        dao.setStorage(storage);
    }

    private static Trainer trainer(String username) {
        Trainer trainer = new Trainer();
        trainer.setFirstName("Anna");
        trainer.setLastName("Lee");
        trainer.setUsername(username);
        trainer.addSpecialization(TrainingType.YOGA);
        return trainer;
    }

    @Test
    void createAssignsIdAndStoresTrainer() {
        Trainer trainer = trainer("Anna.Lee");

        Trainer result = dao.create(trainer);

        assertSame(trainer, result);
        assertNotNull(result.getUserId());
        assertSame(trainer, storage.get(result.getUserId()));
    }

    @Test
    void createKeepsExistingId() {
        UUID id = UUID.randomUUID();
        Trainer trainer = trainer("Anna.Lee");
        trainer.setUserId(id);

        dao.create(trainer);

        assertEquals(id, trainer.getUserId());
        assertSame(trainer, storage.get(id));
    }

    @Test
    void updateReplacesStoredTrainerAndReturnsNewVersion() {
        Trainer original = dao.create(trainer("Anna.Lee"));
        Trainer changed = trainer("Anna.Lee");
        changed.setUserId(original.getUserId());
        changed.setSpecialization(Set.of(TrainingType.FITNESS));

        Trainer result = dao.update(changed);

        assertSame(changed, result);
        assertEquals(Set.of(TrainingType.FITNESS), storage.get(original.getUserId()).getSpecialization());
        assertEquals(1, storage.size());
    }

    @Test
    void updateUnknownIdThrowsAndDoesNotInsert() {
        Trainer trainer = trainer("Anna.Lee");
        trainer.setUserId(UUID.randomUUID());

        assertThrows(NoSuchElementException.class, () -> dao.update(trainer));
        assertTrue(storage.isEmpty());
    }

    @Test
    void updateWithoutIdThrows() {
        Trainer trainer = trainer("Anna.Lee");

        assertThrows(NoSuchElementException.class, () -> dao.update(trainer));
    }

    @Test
    void findByIdReturnsStoredTrainer() {
        Trainer trainer = dao.create(trainer("Anna.Lee"));

        Optional<Trainer> result = dao.findById(trainer.getUserId());

        assertTrue(result.isPresent());
        assertSame(trainer, result.get());
    }

    @Test
    void findByIdUnknownReturnsEmpty() {
        assertTrue(dao.findById(UUID.randomUUID()).isEmpty());
    }

    @Test
    void findByUsernameReturnsMatchingTrainer() {
        dao.create(trainer("Anna.Lee"));
        Trainer second = dao.create(trainer("Anna.Lee1"));

        Optional<Trainer> result = dao.findByUsername("Anna.Lee1");

        assertTrue(result.isPresent());
        assertSame(second, result.get());
    }

    @Test
    void findByUsernameUnknownReturnsEmpty() {
        dao.create(trainer("Anna.Lee"));

        assertTrue(dao.findByUsername("Nobody.Here").isEmpty());
    }

    @Test
    void findByUsernameSkipsTrainersWithoutUsername() {
        dao.create(trainer(null));

        assertTrue(dao.findByUsername("Anna.Lee").isEmpty());
    }

    @Test
    void findAllReturnsUnmodifiableSnapshot() {
        dao.create(trainer("Anna.Lee"));

        List<Trainer> all = dao.findAll();
        dao.create(trainer("Anna.Lee1"));

        assertEquals(1, all.size());
        Trainer extra = trainer("X.Y");
        assertThrows(UnsupportedOperationException.class, () -> all.add(extra));
    }
}
