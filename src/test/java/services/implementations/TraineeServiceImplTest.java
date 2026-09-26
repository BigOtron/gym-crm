package services.implementations;

import io.gymcrm.dao.TraineeDao;
import io.gymcrm.entities.Trainee;
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
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraineeServiceImplTest {

    @Mock
    private TraineeDao traineeDao;

    @Mock
    private UsernameGenerator usernameGenerator;

    @Mock
    private PasswordGenerator passwordGenerator;

    private TraineeServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TraineeServiceImpl();
        service.setTraineeDao(traineeDao);
        service.setUsernameGenerator(usernameGenerator);
        service.setPasswordGenerator(passwordGenerator);
    }

    private static Trainee trainee(String firstName, String lastName) {
        Trainee trainee = new Trainee();
        trainee.setFirstName(firstName);
        trainee.setLastName(lastName);
        trainee.setDateOfBirth(LocalDate.of(1998, 3, 14));
        trainee.setAddress("Tashkent");
        return trainee;
    }

    private static Trainee storedTrainee(UUID id) {
        Trainee trainee = trainee("John", "Smith");
        trainee.setUserId(id);
        trainee.setUsername("John.Smith");
        trainee.setPassword("OrigPass01");
        trainee.setActive(true);
        return trainee;
    }

    // create

    @Test
    void createSetsGeneratedCredentialsAndActivates() {
        Trainee input = trainee("John", "Smith");
        when(usernameGenerator.generate("John", "Smith")).thenReturn("John.Smith");
        when(passwordGenerator.generate()).thenReturn("Abc123Xyz9");
        when(traineeDao.create(any(Trainee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Trainee result = service.create(input);

        assertEquals("John.Smith", result.getUsername());
        assertEquals("Abc123Xyz9", result.getPassword());
        assertTrue(result.isActive());
        verify(traineeDao).create(input);
    }

    @Test
    void createClearsIncomingId() {
        Trainee input = trainee("John", "Smith");
        input.setUserId(UUID.randomUUID());
        when(usernameGenerator.generate("John", "Smith")).thenReturn("John.Smith");
        when(passwordGenerator.generate()).thenReturn("Abc123Xyz9");
        when(traineeDao.create(any(Trainee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Trainee result = service.create(input);

        assertNull(result.getUserId());
    }

    @Test
    void createReturnsWhatDaoReturns() {
        Trainee input = trainee("John", "Smith");
        Trainee saved = storedTrainee(UUID.randomUUID());
        when(usernameGenerator.generate("John", "Smith")).thenReturn("John.Smith");
        when(passwordGenerator.generate()).thenReturn("Abc123Xyz9");
        when(traineeDao.create(input)).thenReturn(saved);

        assertSame(saved, service.create(input));
    }

    @Test
    void createWithBlankFirstNameThrows() {
        Trainee input = trainee("  ", "Smith");

        assertThrows(IllegalArgumentException.class, () -> service.create(input));
        verifyNoInteractions(traineeDao, usernameGenerator, passwordGenerator);
    }

    @Test
    void createWithMissingLastNameThrows() {
        Trainee input = trainee("John", null);

        assertThrows(IllegalArgumentException.class, () -> service.create(input));
        verifyNoInteractions(traineeDao, usernameGenerator, passwordGenerator);
    }

    @Test
    void createNullThrows() {
        assertThrows(NullPointerException.class, () -> service.create(null));
        verifyNoInteractions(traineeDao);
    }

    // update

    @Test
    void updateKeepsStoredUsernameAndPassword() {
        UUID id = UUID.randomUUID();
        when(traineeDao.findById(id)).thenReturn(Optional.of(storedTrainee(id)));
        when(traineeDao.update(any(Trainee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Trainee changes = trainee("John", "Smith");
        changes.setUserId(id);
        changes.setUsername("Hacked.Name");
        changes.setPassword("NewPass999");
        changes.setAddress("Samarkand");

        Trainee result = service.update(changes);

        assertEquals("John.Smith", result.getUsername());
        assertEquals("OrigPass01", result.getPassword());
        assertEquals("Samarkand", result.getAddress());
        verify(traineeDao).update(changes);
    }

    @Test
    void updateUnknownTraineeThrows() {
        UUID id = UUID.randomUUID();
        when(traineeDao.findById(id)).thenReturn(Optional.empty());
        Trainee changes = trainee("John", "Smith");
        changes.setUserId(id);

        assertThrows(NoSuchElementException.class, () -> service.update(changes));
        verify(traineeDao, never()).update(any());
    }

    @Test
    void updateWithBlankNameThrows() {
        Trainee changes = trainee("", "Smith");
        changes.setUserId(UUID.randomUUID());

        assertThrows(IllegalArgumentException.class, () -> service.update(changes));
        verifyNoInteractions(traineeDao);
    }

    // delete

    @Test
    void deleteDelegatesToDao() {
        UUID id = UUID.randomUUID();

        service.delete(id);

        verify(traineeDao).delete(id);
    }

    @Test
    void deleteUnknownTraineeThrows() {
        UUID id = UUID.randomUUID();
        doThrow(new NoSuchElementException("Trainee not found")).when(traineeDao).delete(id);

        assertThrows(NoSuchElementException.class, () -> service.delete(id));
    }

    // select

    @Test
    void getByIdReturnsTrainee() {
        UUID id = UUID.randomUUID();
        Trainee stored = storedTrainee(id);
        when(traineeDao.findById(id)).thenReturn(Optional.of(stored));

        assertSame(stored, service.getById(id));
    }

    @Test
    void getByIdUnknownThrows() {
        UUID id = UUID.randomUUID();
        when(traineeDao.findById(id)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.getById(id));
    }

    @Test
    void getByUsernameReturnsTrainee() {
        Trainee stored = storedTrainee(UUID.randomUUID());
        when(traineeDao.findByUsername("John.Smith")).thenReturn(Optional.of(stored));

        assertSame(stored, service.getByUsername("John.Smith"));
    }

    @Test
    void getByUsernameUnknownThrows() {
        when(traineeDao.findByUsername("Nobody.Here")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.getByUsername("Nobody.Here"));
    }

    @Test
    void getAllReturnsDaoList() {
        List<Trainee> all = List.of(storedTrainee(UUID.randomUUID()));
        when(traineeDao.findAll()).thenReturn(all);

        assertSame(all, service.getAll());
    }
}
