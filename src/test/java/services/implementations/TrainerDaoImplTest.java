package services.implementations;

import io.gymcrm.dao.TrainerDao;
import io.gymcrm.entities.Trainee;
import io.gymcrm.entities.Trainer;
import io.gymcrm.entities.User;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import support.DaoTestBase;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrainerDaoImplTest extends DaoTestBase {

    @Autowired
    private TrainerDao trainerDao;

    private static List<String> usernames(List<Trainer> trainers) {
        return trainers.stream().map(t -> t.getUser().getUsername()).toList();
    }

    @Test
    void saveCascadesToUser() {
        Trainer trainer = new Trainer();
        trainer.setUser(new User("Anna", "Lee", "Anna.Lee", "password", true));
        trainer.setSpecialization(type("Yoga"));

        trainerDao.save(trainer);
        flushAndClear();

        assertNotNull(trainer.getId());
        assertEquals(1, count("users"));
        assertEquals(1, count("trainer"));
    }

    @Test
    void findByUsernameLoadsSpecializationAndTrainees() {
        Trainer anna = persistTrainer("Anna", "Lee", "Anna.Lee", "Yoga");
        persistTrainee("John", "Smith", "John.Smith").getTrainers().add(anna);
        flushAndClear();

        Trainer found = trainerDao.findByUsername("Anna.Lee").orElseThrow();

        assertEquals("Yoga", found.getSpecialization().getTrainingTypeName());
        assertTrue(Hibernate.isInitialized(found.getTrainees()));
        assertEquals(1, found.getTrainees().size());
    }

    @Test
    void findByUsernameIgnoresTrainees() {
        persistTrainee("John", "Smith", "John.Smith");

        assertTrue(trainerDao.findByUsername("John.Smith").isEmpty());
    }

    @Test
    void findByUsernamesReturnsOnlyExistingTrainers() {
        persistTrainer("Anna", "Lee", "Anna.Lee", "Yoga");
        persistTrainer("David", "Brown", "David.Brown", "Heavy lifting");
        persistTrainee("John", "Smith", "John.Smith");

        List<Trainer> found = trainerDao.findByUsernames(List.of("Anna.Lee", "John.Smith", "Nobody"));

        assertEquals(List.of("Anna.Lee"), usernames(found));
    }

    @Test
    void findByUsernamesWithEmptyListReturnsEmptyList() {
        persistTrainer("Anna", "Lee", "Anna.Lee", "Yoga");

        assertTrue(trainerDao.findByUsernames(List.of()).isEmpty());
    }

    @Test
    void notAssignedReturnsActiveTrainersOutsideTheTraineesList() {
        Trainee john = persistTrainee("John", "Smith", "John.Smith");
        Trainer anna = persistTrainer("Anna", "Lee", "Anna.Lee", "Yoga");
        persistTrainer("David", "Brown", "David.Brown", "Heavy lifting");
        persistTrainer("Old", "Coach", "Old.Coach", "Fitness").getUser().setActive(false);
        john.getTrainers().add(anna);
        flushAndClear();

        assertEquals(List.of("David.Brown"), usernames(trainerDao.findNotAssignedToTrainee("John.Smith")));
    }

    @Test
    void notAssignedIgnoresOtherTraineesLists() {
        Trainee john = persistTrainee("John", "Smith", "John.Smith");
        Trainee maria = persistTrainee("Maria", "Garcia", "Maria.Garcia");
        Trainer anna = persistTrainer("Anna", "Lee", "Anna.Lee", "Yoga");
        Trainer david = persistTrainer("David", "Brown", "David.Brown", "Heavy lifting");
        john.getTrainers().add(anna);
        maria.getTrainers().add(david);
        flushAndClear();

        assertEquals(List.of("David.Brown"), usernames(trainerDao.findNotAssignedToTrainee("John.Smith")));
    }

    @Test
    void notAssignedReturnsAllActiveTrainersWhenListIsEmptyAndSortsByUsername() {
        persistTrainee("John", "Smith", "John.Smith");
        persistTrainer("David", "Brown", "David.Brown", "Heavy lifting");
        persistTrainer("Anna", "Lee", "Anna.Lee", "Yoga");
        flushAndClear();

        assertEquals(List.of("Anna.Lee", "David.Brown"),
                usernames(trainerDao.findNotAssignedToTrainee("John.Smith")));
    }
}
