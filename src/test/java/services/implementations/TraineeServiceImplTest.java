package services.implementations;

import io.gymcrm.dao.TraineeDao;
import io.gymcrm.dao.TrainerDao;
import io.gymcrm.dto.TraineeRegistration;
import io.gymcrm.dto.TraineeUpdate;
import io.gymcrm.entities.Trainee;
import io.gymcrm.entities.Trainer;
import io.gymcrm.exceptions.NotFoundException;
import io.gymcrm.services.implementations.TraineeServiceImpl;
import io.gymcrm.util.PasswordGenerator;
import io.gymcrm.util.UsernameGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static support.TestData.FITNESS;
import static support.TestData.YOGA;
import static support.TestData.trainee;
import static support.TestData.trainer;

@ExtendWith(MockitoExtension.class)
class TraineeServiceImplTest {

    @Mock
    private TraineeDao traineeDao;

    @Mock
    private TrainerDao trainerDao;

    @Mock
    private UsernameGenerator usernameGenerator;

    @Mock
    private PasswordGenerator passwordGenerator;

    private TraineeServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TraineeServiceImpl();
        service.setTraineeDao(traineeDao);
        service.setTrainerDao(trainerDao);
        service.setUsernameGenerator(usernameGenerator);
        service.setPasswordGenerator(passwordGenerator);
    }

    private Trainee givenTrainee() {
        Trainee trainee = trainee("John.Smith", "secret");
        when(traineeDao.findByUsername("John.Smith")).thenReturn(Optional.of(trainee));
        return trainee;
    }

    @Test
    void createGeneratesCredentialsAndActivates() {
        when(usernameGenerator.generate("John", "Smith")).thenReturn("John.Smith");
        when(passwordGenerator.generate()).thenReturn("aaaaaaaaaa");
        when(traineeDao.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Trainee created = service.create(
                new TraineeRegistration(" John ", "Smith", LocalDate.of(1995, 4, 12), "Tashkent"));

        assertEquals("John", created.getUser().getFirstName());
        assertEquals("John.Smith", created.getUser().getUsername());
        assertEquals("aaaaaaaaaa", created.getUser().getPassword());
        assertTrue(created.getUser().isActive());
        assertEquals("Tashkent", created.getAddress());
    }

    @Test
    void createRejectsMissingNames() {
        assertThrows(IllegalArgumentException.class,
                () -> service.create(new TraineeRegistration(null, "Smith", null, null)));
        assertThrows(IllegalArgumentException.class,
                () -> service.create(new TraineeRegistration("John", " ", null, null)));
        assertThrows(NullPointerException.class, () -> service.create(null));
        verifyNoInteractions(traineeDao, usernameGenerator);
    }

    @Test
    void getByUsernameThrowsWhenMissing() {
        when(traineeDao.findByUsername("Nobody")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getByUsername("Nobody"));
    }

    @Test
    void getByUsernameReturnsTrainee() {
        Trainee trainee = givenTrainee();

        assertSame(trainee, service.getByUsername("John.Smith"));
    }

    @Test
    void changePasswordUpdatesUser() {
        Trainee trainee = givenTrainee();

        service.changePassword("John.Smith", "newPassword");

        assertEquals("newPassword", trainee.getUser().getPassword());
    }

    @Test
    void changePasswordRejectsBlank() {
        assertThrows(IllegalArgumentException.class, () -> service.changePassword("John.Smith", " "));
        verifyNoInteractions(traineeDao);
    }

    @Test
    void updateChangesFieldsButKeepsUsername() {
        Trainee trainee = givenTrainee();

        service.update("John.Smith", new TraineeUpdate("Johnny", "Smithson", LocalDate.of(2000, 1, 1), "Bukhara"));

        assertEquals("Johnny", trainee.getUser().getFirstName());
        assertEquals("Smithson", trainee.getUser().getLastName());
        assertEquals("John.Smith", trainee.getUser().getUsername());
        assertEquals(LocalDate.of(2000, 1, 1), trainee.getDateOfBirth());
        assertEquals("Bukhara", trainee.getAddress());
    }

    @Test
    void updateRejectsMissingNames() {
        assertThrows(IllegalArgumentException.class,
                () -> service.update("John.Smith", new TraineeUpdate("John", null, null, null)));
        verifyNoInteractions(traineeDao);
    }

    @Test
    void toggleActiveFlipsTheFlag() {
        Trainee trainee = givenTrainee();

        assertFalse(service.toggleActive("John.Smith"));
        assertFalse(trainee.getUser().isActive());
        assertTrue(service.toggleActive("John.Smith"));
    }

    @Test
    void deleteRemovesTrainee() {
        Trainee trainee = givenTrainee();

        service.deleteByUsername("John.Smith");

        verify(traineeDao).delete(trainee);
    }

    @Test
    void deleteUnknownTraineeThrows() {
        when(traineeDao.findByUsername("Nobody")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.deleteByUsername("Nobody"));
        verify(traineeDao, never()).delete(any());
    }

    @Test
    void updateTrainersReplacesTheSet() {
        Trainee trainee = givenTrainee();
        trainee.getTrainers().add(trainer("Old.Trainer", "pw", FITNESS));
        Trainer anna = trainer("Anna.Lee", "pw", YOGA);
        when(trainerDao.findByUsernames(Set.of("Anna.Lee"))).thenReturn(List.of(anna));

        List<Trainer> result = service.updateTrainers("John.Smith", List.of("Anna.Lee", "Anna.Lee"));

        assertEquals(List.of(anna), result);
        assertEquals(1, trainee.getTrainers().size());
        assertTrue(trainee.getTrainers().contains(anna));
    }

    @Test
    void updateTrainersRejectsUnknownTrainer() {
        Trainee trainee = givenTrainee();
        Trainer old = trainer("Old.Trainer", "pw", FITNESS);
        trainee.getTrainers().add(old);
        when(trainerDao.findByUsernames(anyCollection())).thenReturn(List.of());

        assertThrows(NotFoundException.class, () -> service.updateTrainers("John.Smith", List.of("Nobody")));
        assertTrue(trainee.getTrainers().contains(old));
    }
}
