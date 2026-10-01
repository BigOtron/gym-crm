package services.implementations;

import io.gymcrm.dao.TraineeDao;
import io.gymcrm.dao.TrainerDao;
import io.gymcrm.dao.TrainingTypeDao;
import io.gymcrm.dto.TrainerRegistration;
import io.gymcrm.dto.TrainerUpdate;
import io.gymcrm.entities.Trainer;
import io.gymcrm.exceptions.NotFoundException;
import io.gymcrm.services.implementations.TrainerServiceImpl;
import io.gymcrm.util.PasswordGenerator;
import io.gymcrm.util.UsernameGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static support.TestData.FITNESS;
import static support.TestData.YOGA;
import static support.TestData.trainee;
import static support.TestData.trainer;

@ExtendWith(MockitoExtension.class)
class TrainerServiceImplTest {

    @Mock
    private TrainerDao trainerDao;

    @Mock
    private TraineeDao traineeDao;

    @Mock
    private TrainingTypeDao trainingTypeDao;

    @Mock
    private UsernameGenerator usernameGenerator;

    @Mock
    private PasswordGenerator passwordGenerator;

    private TrainerServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TrainerServiceImpl();
        service.setTrainerDao(trainerDao);
        service.setTraineeDao(traineeDao);
        service.setTrainingTypeDao(trainingTypeDao);
        service.setUsernameGenerator(usernameGenerator);
        service.setPasswordGenerator(passwordGenerator);
    }

    private Trainer givenTrainer() {
        Trainer trainer = trainer("Anna.Lee", "secret", YOGA);
        when(trainerDao.findByUsername("Anna.Lee")).thenReturn(Optional.of(trainer));
        return trainer;
    }

    @Test
    void createGeneratesCredentialsAndSetsSpecialization() {
        when(trainingTypeDao.findByName("yoga")).thenReturn(Optional.of(YOGA));
        when(usernameGenerator.generate("Anna", "Lee")).thenReturn("Anna.Lee");
        when(passwordGenerator.generate()).thenReturn("bbbbbbbbbb");
        when(trainerDao.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Trainer created = service.create(new TrainerRegistration("Anna", "Lee", "yoga"));

        assertEquals("Anna.Lee", created.getUser().getUsername());
        assertEquals("bbbbbbbbbb", created.getUser().getPassword());
        assertTrue(created.getUser().isActive());
        assertSame(YOGA, created.getSpecialization());
    }

    @Test
    void createRejectsMissingFields() {
        assertThrows(IllegalArgumentException.class,
                () -> service.create(new TrainerRegistration("Anna", "Lee", " ")));
        assertThrows(IllegalArgumentException.class,
                () -> service.create(new TrainerRegistration("", "Lee", "Yoga")));
        verifyNoInteractions(trainerDao, usernameGenerator);
    }

    @Test
    void createRejectsUnknownSpecialization() {
        when(trainingTypeDao.findByName("Boxing")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.create(new TrainerRegistration("Anna", "Lee", "Boxing")));
        verify(trainerDao, never()).save(any());
    }

    @Test
    void getByUsername() {
        Trainer trainer = givenTrainer();
        when(trainerDao.findByUsername("Nobody")).thenReturn(Optional.empty());

        assertSame(trainer, service.getByUsername("Anna.Lee"));
        assertThrows(NotFoundException.class, () -> service.getByUsername("Nobody"));
    }

    @Test
    void changePassword() {
        Trainer trainer = givenTrainer();

        service.changePassword("Anna.Lee", "newPassword");

        assertEquals("newPassword", trainer.getUser().getPassword());
    }

    @Test
    void updateChangesNamesAndSpecialization() {
        Trainer trainer = givenTrainer();
        when(trainingTypeDao.findByName("Fitness")).thenReturn(Optional.of(FITNESS));

        service.update("Anna.Lee", new TrainerUpdate("Ann", "Leigh", "Fitness"));

        assertEquals("Ann", trainer.getUser().getFirstName());
        assertEquals("Leigh", trainer.getUser().getLastName());
        assertEquals("Anna.Lee", trainer.getUser().getUsername());
        assertSame(FITNESS, trainer.getSpecialization());
    }

    @Test
    void toggleActiveFlipsTheFlag() {
        givenTrainer();

        assertFalse(service.toggleActive("Anna.Lee"));
        assertTrue(service.toggleActive("Anna.Lee"));
    }

    @Test
    void notAssignedTrainersRequireExistingTrainee() {
        when(traineeDao.findByUsername("Nobody")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getNotAssignedToTrainee("Nobody"));
        verify(trainerDao, never()).findNotAssignedToTrainee(any());
    }

    @Test
    void notAssignedTrainers() {
        Trainer trainer = trainer("Anna.Lee", "pw", YOGA);
        when(traineeDao.findByUsername("John.Smith")).thenReturn(Optional.of(trainee("John.Smith", "pw")));
        when(trainerDao.findNotAssignedToTrainee("John.Smith")).thenReturn(List.of(trainer));

        assertEquals(List.of(trainer), service.getNotAssignedToTrainee("John.Smith"));
    }
}
