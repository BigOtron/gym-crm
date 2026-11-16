package services.implementations;

import io.gymcrm.dao.TraineeDao;
import io.gymcrm.dao.TrainerDao;
import io.gymcrm.dao.TrainingTypeDao;
import io.gymcrm.dto.CreatedProfile;
import io.gymcrm.dto.IssuedToken;
import io.gymcrm.dto.TrainerRegistration;
import io.gymcrm.dto.TrainerUpdate;
import io.gymcrm.entities.Trainer;
import io.gymcrm.exceptions.ConflictException;
import io.gymcrm.exceptions.NotFoundException;
import io.gymcrm.security.JwtTokenService;
import io.gymcrm.security.Role;
import io.gymcrm.services.implementations.TrainerServiceImpl;
import io.gymcrm.util.PasswordGenerator;
import io.gymcrm.util.UsernameGenerator;
import io.gymcrm.metrics.GymMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import support.TestData;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
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

    @Mock
    private JwtTokenService tokenService;

    private TrainerServiceImpl service;

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

    @BeforeEach
    void setUp() {
        service = new TrainerServiceImpl();
        service.setMetrics(new GymMetrics(registry));
        service.setPasswordEncoder(TestData.ENCODER);
        service.setTrainerDao(trainerDao);
        service.setTraineeDao(traineeDao);
        service.setTrainingTypeDao(trainingTypeDao);
        service.setUsernameGenerator(usernameGenerator);
        service.setPasswordGenerator(passwordGenerator);
        service.setTokenService(tokenService);
    }

    private Trainer givenAnna() {
        Trainer anna = trainer("Anna.Lee", "secret", YOGA);
        when(trainerDao.findByUsername("Anna.Lee")).thenReturn(Optional.of(anna));
        return anna;
    }

    @Nested
    class Create {

        @Test
        void generatesCredentialsAndSetsSpecialization() {
            when(trainingTypeDao.findById(6L)).thenReturn(Optional.of(YOGA));
            when(usernameGenerator.generate("Anna", "Lee")).thenReturn("Anna.Lee");
            when(passwordGenerator.generate()).thenReturn("bbbbbbbbbb");
            IssuedToken token = new IssuedToken("jwt", 3600);
            when(tokenService.issue("Anna.Lee", Role.TRAINER)).thenReturn(token);
            when(trainerDao.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CreatedProfile<Trainer> result = service.create(new TrainerRegistration("Anna", "Lee", 6L));
            Trainer created = result.profile();

            assertEquals("Anna.Lee", created.getUser().getUsername());
            assertEquals("bbbbbbbbbb", result.password(), "the generated password is returned once");
            assertSame(token, result.token(), "a token for immediate use");
            assertNotEquals("bbbbbbbbbb", created.getUser().getPassword(), "only a hash is stored");
            assertTrue(TestData.ENCODER.matches("bbbbbbbbbb", created.getUser().getPassword()));
            assertTrue(created.getUser().isActive());
            assertSame(YOGA, created.getSpecialization());
            assertEquals(1, registry.get(GymMetrics.REGISTRATIONS).tag("role", "trainer").counter().count());
            assertEquals(0, registry.get(GymMetrics.REGISTRATIONS).tag("role", "trainee").counter().count());
        }

        @Test
        void rejectsMissingFields() {
            assertThrows(IllegalArgumentException.class, () -> service.create(new TrainerRegistration("Anna", "Lee", null)));
            assertThrows(IllegalArgumentException.class, () -> service.create(new TrainerRegistration(null, "Lee", 6L)));
            assertThrows(IllegalArgumentException.class, () -> service.create(new TrainerRegistration("Anna", " ", 6L)));
            assertThrows(NullPointerException.class, () -> service.create(null));
            verifyNoInteractions(trainerDao, usernameGenerator, passwordGenerator);
        }

        @Test
        void rejectsUnknownSpecialization() {
            when(trainingTypeDao.findById(99L)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> service.create(new TrainerRegistration("Anna", "Lee", 99L)));
            verify(trainerDao, never()).save(any());
            verifyNoInteractions(usernameGenerator, passwordGenerator);
        }
    }

    @Nested
    class GetByUsername {

        @Test
        void anyAuthenticatedUserCanReadAProfile() {
            Trainer anna = givenAnna();
            assertSame(anna, service.getByUsername("Anna.Lee"));
        }

        @Test
        void unknownTrainer() {
            when(trainerDao.findByUsername("Nobody")).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> service.getByUsername("Nobody"));
        }
    }

    @Nested
    class Update {

        @Test
        void changesNamesAndActiveButNotSpecialization() {
            Trainer anna = givenAnna();

            service.update("Anna.Lee", new TrainerUpdate("Ann", "Leigh", false));

            assertEquals("Ann", anna.getUser().getFirstName());
            assertEquals("Leigh", anna.getUser().getLastName());
            assertFalse(anna.getUser().isActive());
            assertSame(YOGA, anna.getSpecialization());
            assertEquals("Anna.Lee", anna.getUser().getUsername());
            verifyNoInteractions(trainingTypeDao);
        }

        @Test
        void rejectsMissingRequiredFields() {
            assertThrows(IllegalArgumentException.class,
                    () -> service.update("Anna.Lee", new TrainerUpdate(null, "Lee", true)));
            assertThrows(IllegalArgumentException.class,
                    () -> service.update("Anna.Lee", new TrainerUpdate("Ann", "Lee", null)));
            assertThrows(NullPointerException.class, () -> service.update("Anna.Lee", null));
            verifyNoInteractions(trainerDao);
        }
    }

    @Nested
    class SetActive {

        @Test
        void changesTheFlag() {
            Trainer anna = givenAnna();

            service.setActive("Anna.Lee", false);

            assertFalse(anna.getUser().isActive());
        }

        @Test
        void sameStateIsAConflict() {
            Trainer anna = givenAnna();

            assertThrows(ConflictException.class, () -> service.setActive("Anna.Lee", true));
            assertTrue(anna.getUser().isActive());
        }
    }

    @Nested
    class NotAssignedToTrainee {

        @Test
        void returnsTheTrainers() {
            Trainer anna = trainer("Anna.Lee", "pw", YOGA);
            when(traineeDao.findByUsername("John.Smith")).thenReturn(Optional.of(trainee("John.Smith", "pw")));
            when(trainerDao.findNotAssignedToTrainee("John.Smith")).thenReturn(List.of(anna));

            assertEquals(List.of(anna), service.getNotAssignedToTrainee("John.Smith"));
        }

        @Test
        void unknownTrainee() {
            when(traineeDao.findByUsername("Nobody")).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> service.getNotAssignedToTrainee("Nobody"));
            verify(trainerDao, never()).findNotAssignedToTrainee(any());
        }
    }
}
