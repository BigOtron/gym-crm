package services.implementations;

import io.gymcrm.dao.TrainerDao;
import io.gymcrm.entities.Trainer;
import io.gymcrm.entities.TrainingType;
import io.gymcrm.services.implementations.TrainerServiceImpl;
import io.gymcrm.util.PasswordGenerator;
import io.gymcrm.util.UsernameGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainerServiceImplTest {

    @Mock
    private TrainerDao trainerDao;

    @Mock
    private UsernameGenerator usernameGenerator;

    @Mock
    private PasswordGenerator passwordGenerator;

    private TrainerServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TrainerServiceImpl();
        service.setTrainerDao(trainerDao);
        service.setUsernameGenerator(usernameGenerator);
        service.setPasswordGenerator(passwordGenerator);
    }

    private static Trainer trainer(String firstName, String lastName, TrainingType... types) {
        Trainer trainer = new Trainer();
        trainer.setFirstName(firstName);
        trainer.setLastName(lastName);
        trainer.setSpecialization(Set.of(types));
        return trainer;
    }

    private static Trainer storedTrainer(UUID id) {
        Trainer trainer = trainer("Anna", "Lee", TrainingType.YOGA);
        trainer.setUserId(id);
        trainer.setUsername("Anna.Lee");
        trainer.setPassword("OrigPass01");
        trainer.setActive(true);
        return trainer;
    }

    // create

    @Test
    void createSetsGeneratedCredentialsAndActivates() {
        Trainer input = trainer("Anna", "Lee", TrainingType.YOGA);
        when(usernameGenerator.generate("Anna", "Lee")).thenReturn("Anna.Lee");
        when(passwordGenerator.generate()).thenReturn("Abc123Xyz9");
        when(trainerDao.create(any(Trainer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Trainer result = service.create(input);

        assertEquals("Anna.Lee", result.getUsername());
        assertEquals("Abc123Xyz9", result.getPassword());
        assertTrue(result.isActive());
        assertEquals(Set.of(TrainingType.YOGA), result.getSpecialization());
        verify(trainerDao).create(input);
    }

    @Test
    void createClearsIncomingId() {
        Trainer input = trainer("Anna", "Lee", TrainingType.YOGA);
        input.setUserId(UUID.randomUUID());
        when(usernameGenerator.generate("Anna", "Lee")).thenReturn("Anna.Lee");
        when(passwordGenerator.generate()).thenReturn("Abc123Xyz9");
        when(trainerDao.create(any(Trainer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Trainer result = service.create(input);

        assertNull(result.getUserId());
    }

    @Test
    void createWithoutSpecializationThrows() {
        Trainer input = trainer("Anna", "Lee");

        assertThrows(IllegalArgumentException.class, () -> service.create(input));
        verifyNoInteractions(trainerDao, usernameGenerator, passwordGenerator);
    }

    @Test
    void createWithBlankNameThrows() {
        Trainer input = trainer("Anna", " ", TrainingType.YOGA);

        assertThrows(IllegalArgumentException.class, () -> service.create(input));
        verifyNoInteractions(trainerDao, usernameGenerator, passwordGenerator);
    }

    @Test
    void createNullThrows() {
        assertThrows(NullPointerException.class, () -> service.create(null));
        verifyNoInteractions(trainerDao);
    }

    // update

    @Test
    void updateKeepsStoredUsernameAndPassword() {
        UUID id = UUID.randomUUID();
        when(trainerDao.findById(id)).thenReturn(Optional.of(storedTrainer(id)));
        when(trainerDao.update(any(Trainer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Trainer changes = trainer("Anna", "Lee", TrainingType.YOGA, TrainingType.FITNESS);
        changes.setUserId(id);
        changes.setUsername("Hacked.Name");
        changes.setPassword("NewPass999");

        Trainer result = service.update(changes);

        assertEquals("Anna.Lee", result.getUsername());
        assertEquals("OrigPass01", result.getPassword());
        assertEquals(Set.of(TrainingType.YOGA, TrainingType.FITNESS), result.getSpecialization());
        verify(trainerDao).update(changes);
    }

    @Test
    void updateUnknownTrainerThrows() {
        UUID id = UUID.randomUUID();
        when(trainerDao.findById(id)).thenReturn(Optional.empty());
        Trainer changes = trainer("Anna", "Lee", TrainingType.YOGA);
        changes.setUserId(id);

        assertThrows(NoSuchElementException.class, () -> service.update(changes));
        verify(trainerDao, never()).update(any());
    }

    @Test
    void updateWithoutSpecializationThrows() {
        Trainer changes = trainer("Anna", "Lee");
        changes.setUserId(UUID.randomUUID());

        assertThrows(IllegalArgumentException.class, () -> service.update(changes));
        verifyNoInteractions(trainerDao);
    }

    // select

    @Test
    void getByIdReturnsTrainer() {
        UUID id = UUID.randomUUID();
        Trainer stored = storedTrainer(id);
        when(trainerDao.findById(id)).thenReturn(Optional.of(stored));

        assertSame(stored, service.getById(id));
    }

    @Test
    void getByIdUnknownThrows() {
        UUID id = UUID.randomUUID();
        when(trainerDao.findById(id)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.getById(id));
    }

    @Test
    void getByUsernameReturnsTrainer() {
        Trainer stored = storedTrainer(UUID.randomUUID());
        when(trainerDao.findByUsername("Anna.Lee")).thenReturn(Optional.of(stored));

        assertSame(stored, service.getByUsername("Anna.Lee"));
    }

    @Test
    void getByUsernameUnknownThrows() {
        when(trainerDao.findByUsername("Nobody.Here")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.getByUsername("Nobody.Here"));
    }

    @Test
    void getAllReturnsDaoList() {
        List<Trainer> all = List.of(storedTrainer(UUID.randomUUID()));
        when(trainerDao.findAll()).thenReturn(all);

        assertSame(all, service.getAll());
    }
}
