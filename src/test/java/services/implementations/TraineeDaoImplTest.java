package services.implementations;

import io.gymcrm.dao.TraineeDao;
import io.gymcrm.entities.Trainee;
import io.gymcrm.entities.Trainer;
import io.gymcrm.entities.User;
import jakarta.persistence.PersistenceException;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import support.DaoTestBase;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraineeDaoImplTest extends DaoTestBase {

    @Autowired
    private TraineeDao traineeDao;

    @Test
    void saveCascadesToUser() {
        Trainee trainee = new Trainee();
        trainee.setUser(new User("John", "Smith", "John.Smith", "password", true));

        traineeDao.save(trainee);
        flushAndClear();

        assertNotNull(trainee.getId());
        assertNotNull(trainee.getUser().getId());
        assertEquals(1, count("users"));
        assertEquals(1, count("trainee"));
    }

    @Test
    void saveWithoutUserFails() {
        assertThrows(PersistenceException.class, () -> {
            traineeDao.save(new Trainee());
            entityManager.flush();
        });
    }

    @Test
    void duplicateUsernameIsRejectedByDatabase() {
        persistTrainee("John", "Smith", "John.Smith");

        assertThrows(PersistenceException.class, () -> {
            persistTrainee("John", "Smith", "John.Smith");
            entityManager.flush();
        });
    }

    @Test
    void findByUsernameLoadsProfileAndTrainers() {
        Trainee trainee = persistTrainee("John", "Smith", "John.Smith");
        Trainer anna = persistTrainer("Anna", "Lee", "Anna.Lee", "Yoga");
        trainee.getTrainers().add(anna);
        flushAndClear();

        Trainee found = traineeDao.findByUsername("John.Smith").orElseThrow();

        assertEquals(LocalDate.of(1995, 4, 12), found.getDateOfBirth());
        assertEquals("Tashkent", found.getAddress());
        // The test transaction would hide lazy loading, so check the query loaded the trainers itself.
        assertTrue(Hibernate.isInitialized(found.getTrainers()));
        assertEquals(1, found.getTrainers().size());
        assertEquals("Anna.Lee", found.getTrainers().iterator().next().getUser().getUsername());
    }

    @Test
    void findByUsernameReturnsTraineeWithoutTrainers() {
        persistTrainee("John", "Smith", "John.Smith");
        flushAndClear();

        assertTrue(traineeDao.findByUsername("John.Smith").orElseThrow().getTrainers().isEmpty());
    }

    @Test
    void findByUsernameIgnoresTrainers() {
        persistTrainer("Anna", "Lee", "Anna.Lee", "Yoga");

        assertTrue(traineeDao.findByUsername("Anna.Lee").isEmpty());
        assertTrue(traineeDao.findByUsername("Nobody").isEmpty());
    }

    @Test
    void deleteCascadesToUserTrainingsAndTrainerLinks() {
        Trainee trainee = persistTrainee("John", "Smith", "John.Smith");
        Trainer anna = persistTrainer("Anna", "Lee", "Anna.Lee", "Yoga");
        trainee.getTrainers().add(anna);
        persistTraining(trainee, anna, "Yoga 1", LocalDate.of(2026, 9, 1));
        persistTraining(trainee, anna, "Yoga 2", LocalDate.of(2026, 9, 2));
        flushAndClear();

        traineeDao.delete(traineeDao.findByUsername("John.Smith").orElseThrow());
        flushAndClear();

        assertEquals(0, count("trainee"));
        assertEquals(0, count("training"));
        assertEquals(0, count("trainee_trainer"));
        assertEquals(1, count("trainer"));
        assertEquals(1, count("users")); // only the trainer's user is left
    }
}
