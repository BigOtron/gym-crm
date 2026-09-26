package dao.implementations;

import io.gymcrm.dao.implementations.TraineeDaoImpl;
import io.gymcrm.entities.Trainee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraineeDaoImplTest {

    private Map<UUID, Trainee> storage;
    private TraineeDaoImpl dao;

    @BeforeEach
    void setUp() {
        storage = new HashMap<>();
        dao = new TraineeDaoImpl();
        dao.setStorage(storage);
    }

    private static Trainee trainee(String username) {
        Trainee trainee = new Trainee();
        trainee.setFirstName("John");
        trainee.setLastName("Smith");
        trainee.setUsername(username);
        return trainee;
    }

    @Test
    void createAssignsIdAndStoresTrainee() {
        Trainee trainee = trainee("John.Smith");

        Trainee result = dao.create(trainee);

        assertSame(trainee, result);
        assertNotNull(result.getUserId());
        assertSame(trainee, storage.get(result.getUserId()));
    }

    @Test
    void createKeepsExistingId() {
        UUID id = UUID.randomUUID();
        Trainee trainee = trainee("John.Smith");
        trainee.setUserId(id);

        dao.create(trainee);

        assertEquals(id, trainee.getUserId());
        assertSame(trainee, storage.get(id));
    }

    @Test
    void updateReplacesStoredTraineeAndReturnsNewVersion() {
        Trainee original = dao.create(trainee("John.Smith"));
        Trainee changed = trainee("John.Smith");
        changed.setUserId(original.getUserId());
        changed.setAddress("New address");

        Trainee result = dao.update(changed);

        assertSame(changed, result);
        assertEquals("New address", storage.get(original.getUserId()).getAddress());
        assertEquals(1, storage.size());
    }

    @Test
    void updateUnknownIdThrowsAndDoesNotInsert() {
        Trainee trainee = trainee("John.Smith");
        trainee.setUserId(UUID.randomUUID());

        assertThrows(NoSuchElementException.class, () -> dao.update(trainee));
        assertTrue(storage.isEmpty());
    }

    @Test
    void updateWithoutIdThrows() {
        Trainee trainee = trainee("John.Smith");

        assertThrows(NoSuchElementException.class, () -> dao.update(trainee));
    }

    @Test
    void deleteRemovesTrainee() {
        Trainee trainee = dao.create(trainee("John.Smith"));

        dao.delete(trainee.getUserId());

        assertTrue(storage.isEmpty());
    }

    @Test
    void deleteUnknownIdThrows() {
        UUID unknownId = UUID.randomUUID();

        assertThrows(NoSuchElementException.class, () -> dao.delete(unknownId));
    }

    @Test
    void findByIdReturnsStoredTrainee() {
        Trainee trainee = dao.create(trainee("John.Smith"));

        Optional<Trainee> result = dao.findById(trainee.getUserId());

        assertTrue(result.isPresent());
        assertSame(trainee, result.get());
    }

    @Test
    void findByIdUnknownReturnsEmpty() {
        assertTrue(dao.findById(UUID.randomUUID()).isEmpty());
    }

    @Test
    void findByUsernameReturnsMatchingTrainee() {
        dao.create(trainee("John.Smith"));
        Trainee second = dao.create(trainee("John.Smith1"));

        Optional<Trainee> result = dao.findByUsername("John.Smith1");

        assertTrue(result.isPresent());
        assertSame(second, result.get());
    }

    @Test
    void findByUsernameUnknownReturnsEmpty() {
        dao.create(trainee("John.Smith"));

        assertTrue(dao.findByUsername("Nobody.Here").isEmpty());
    }

    @Test
    void findByUsernameSkipsTraineesWithoutUsername() {
        dao.create(trainee(null));

        assertTrue(dao.findByUsername("John.Smith").isEmpty());
    }

    @Test
    void findAllReturnsAllTrainees() {
        dao.create(trainee("John.Smith"));
        dao.create(trainee("John.Smith1"));

        assertEquals(2, dao.findAll().size());
    }

    @Test
    void findAllReturnsUnmodifiableSnapshot() {
        dao.create(trainee("John.Smith"));

        List<Trainee> all = dao.findAll();
        dao.create(trainee("John.Smith1"));

        assertEquals(1, all.size());
        Trainee extra = trainee("X.Y");
        assertThrows(UnsupportedOperationException.class, () -> all.add(extra));
    }
}
