package services.implementations;

import io.gymcrm.dao.TraineeDao;
import io.gymcrm.dao.TrainerDao;
import io.gymcrm.dao.TrainingTypeDao;
import io.gymcrm.dto.Credentials;
import io.gymcrm.dto.TrainerRegistration;
import io.gymcrm.dto.TrainerUpdate;
import io.gymcrm.entities.Trainer;
import io.gymcrm.exceptions.AuthenticationException;
import io.gymcrm.exceptions.ConflictException;
import io.gymcrm.exceptions.ForbiddenException;
import io.gymcrm.exceptions.NotFoundException;
import io.gymcrm.services.AuthenticationService;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static support.TestData.YOGA;
import static support.TestData.trainee;
import static support.TestData.trainer;

@ExtendWith(MockitoExtension.class)
class TrainerServiceImplTest {

    private static final Credentials ANNA = new Credentials("Anna.Lee", "secret");
    private static final Credentials DAVID = new Credentials("David.Brown", "secret");
    private static final AuthenticationException WRONG_PASSWORD = new AuthenticationException("Invalid username or password");

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
    private AuthenticationService authenticationService;

    private TrainerServiceImpl service;

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

    @BeforeEach
    void setUp() {
        service = new TrainerServiceImpl();
        service.setMetrics(new GymMetrics(registry));
        service.setTrainerDao(trainerDao);
        service.setTraineeDao(traineeDao);
        service.setTrainingTypeDao(trainingTypeDao);
        service.setUsernameGenerator(usernameGenerator);
        service.setPasswordGenerator(passwordGenerator);
        service.setAuthenticationService(authenticationService);
    }

    private Trainer givenAnna() {
        Trainer anna = trainer("Anna.Lee", "secret", YOGA);
        when(trainerDao.findByUsername("Anna.Lee")).thenReturn(Optional.of(anna));
        return anna;
    }

    private void givenWrongTrainerCredentials() {
        doThrow(WRONG_PASSWORD).when(authenticationService).authenticateTrainer(ANNA);
    }

    @Nested
    class Create {

        @Test
        void generatesCredentialsAndSetsSpecialization() {
            when(trainingTypeDao.findById(6L)).thenReturn(Optional.of(YOGA));
            when(usernameGenerator.generate("Anna", "Lee")).thenReturn("Anna.Lee");
            when(passwordGenerator.generate()).thenReturn("bbbbbbbbbb");
            when(trainerDao.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Trainer created = service.create(new TrainerRegistration("Anna", "Lee", 6L));

            assertEquals("Anna.Lee", created.getUser().getUsername());
            assertEquals("bbbbbbbbbb", created.getUser().getPassword());
            assertTrue(created.getUser().isActive());
            assertSame(YOGA, created.getSpecialization());
            verifyNoInteractions(authenticationService);
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
            Credentials john = new Credentials("John.Smith", "pw");

            assertSame(anna, service.getByUsername(john, "Anna.Lee"));
            verify(authenticationService).authenticate(john);
        }

        @Test
        void wrongCredentialsAreRejectedBeforeTheLookup() {
            doThrow(WRONG_PASSWORD).when(authenticationService).authenticate(ANNA);

            assertThrows(AuthenticationException.class, () -> service.getByUsername(ANNA, "Anna.Lee"));
            verifyNoInteractions(trainerDao);
        }

        @Test
        void unknownTrainer() {
            when(trainerDao.findByUsername("Nobody")).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> service.getByUsername(ANNA, "Nobody"));
        }
    }

    @Nested
    class ChangePassword {

        @Test
        void changesOwnPassword() {
            Trainer anna = givenAnna();

            service.changePassword(ANNA, "newPassword");

            assertEquals("newPassword", anna.getUser().getPassword());
            verify(authenticationService).authenticateTrainer(ANNA);
        }

        @Test
        void wrongOldPasswordChangesNothing() {
            givenWrongTrainerCredentials();

            assertThrows(AuthenticationException.class, () -> service.changePassword(ANNA, "newPassword"));
            verifyNoInteractions(trainerDao);
        }

        @Test
        void rejectsBlankNewPassword() {
            assertThrows(IllegalArgumentException.class, () -> service.changePassword(ANNA, " "));
            verifyNoInteractions(trainerDao);
        }
    }

    @Nested
    class Update {

        @Test
        void changesNamesAndActiveButNotSpecialization() {
            Trainer anna = givenAnna();

            service.update(ANNA, "Anna.Lee", new TrainerUpdate("Ann", "Leigh", false));

            assertEquals("Ann", anna.getUser().getFirstName());
            assertEquals("Leigh", anna.getUser().getLastName());
            assertFalse(anna.getUser().isActive());
            assertSame(YOGA, anna.getSpecialization());
            assertEquals("Anna.Lee", anna.getUser().getUsername());
            verifyNoInteractions(trainingTypeDao);
        }

        @Test
        void wrongCredentialsChangeNothing() {
            givenWrongTrainerCredentials();

            assertThrows(AuthenticationException.class,
                    () -> service.update(ANNA, "Anna.Lee", new TrainerUpdate("Ann", "Lee", true)));
            verifyNoInteractions(trainerDao);
        }

        @Test
        void someoneElsesProfileIsForbidden() {
            assertThrows(ForbiddenException.class,
                    () -> service.update(DAVID, "Anna.Lee", new TrainerUpdate("Ann", "Lee", true)));
            verifyNoInteractions(trainerDao);
        }

        @Test
        void rejectsMissingRequiredFields() {
            assertThrows(IllegalArgumentException.class,
                    () -> service.update(ANNA, "Anna.Lee", new TrainerUpdate(null, "Lee", true)));
            assertThrows(IllegalArgumentException.class,
                    () -> service.update(ANNA, "Anna.Lee", new TrainerUpdate("Ann", "Lee", null)));
            assertThrows(NullPointerException.class, () -> service.update(ANNA, "Anna.Lee", null));
            verifyNoInteractions(trainerDao);
        }
    }

    @Nested
    class SetActive {

        @Test
        void changesTheFlag() {
            Trainer anna = givenAnna();

            service.setActive(ANNA, "Anna.Lee", false);

            assertFalse(anna.getUser().isActive());
        }

        @Test
        void sameStateIsAConflict() {
            Trainer anna = givenAnna();

            assertThrows(ConflictException.class, () -> service.setActive(ANNA, "Anna.Lee", true));
            assertTrue(anna.getUser().isActive());
        }

        @Test
        void wrongCredentialsChangeNothing() {
            givenWrongTrainerCredentials();

            assertThrows(AuthenticationException.class, () -> service.setActive(ANNA, "Anna.Lee", false));
            verifyNoInteractions(trainerDao);
        }

        @Test
        void someoneElsesProfileIsForbidden() {
            assertThrows(ForbiddenException.class, () -> service.setActive(DAVID, "Anna.Lee", false));
            verifyNoInteractions(trainerDao);
        }

        @Test
        void traineeCredentialsCantChangeATrainer() {
            Credentials john = new Credentials("John.Smith", "pw");
            doThrow(WRONG_PASSWORD).when(authenticationService).authenticateTrainer(john);

            assertThrows(AuthenticationException.class, () -> service.setActive(john, "John.Smith", false));
            verifyNoInteractions(trainerDao);
        }
    }

    @Nested
    class NotAssignedToTrainee {

        @Test
        void returnsTheTrainers() {
            Trainer anna = trainer("Anna.Lee", "pw", YOGA);
            when(traineeDao.findByUsername("John.Smith")).thenReturn(Optional.of(trainee("John.Smith", "pw")));
            when(trainerDao.findNotAssignedToTrainee("John.Smith")).thenReturn(List.of(anna));

            assertEquals(List.of(anna), service.getNotAssignedToTrainee(ANNA, "John.Smith"));
            verify(authenticationService).authenticate(ANNA);
        }

        @Test
        void wrongCredentials() {
            doThrow(WRONG_PASSWORD).when(authenticationService).authenticate(ANNA);

            assertThrows(AuthenticationException.class, () -> service.getNotAssignedToTrainee(ANNA, "John.Smith"));
            verifyNoInteractions(traineeDao, trainerDao);
        }

        @Test
        void unknownTrainee() {
            when(traineeDao.findByUsername("Nobody")).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> service.getNotAssignedToTrainee(ANNA, "Nobody"));
            verify(trainerDao, never()).findNotAssignedToTrainee(any());
        }
    }
}
