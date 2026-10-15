package services.implementations;

import io.gymcrm.dao.TraineeDao;
import io.gymcrm.dao.TrainerDao;
import io.gymcrm.dao.TrainingTypeDao;
import io.gymcrm.dto.Credentials;
import io.gymcrm.dto.TrainerRegistration;
import io.gymcrm.dto.TrainerUpdate;
import io.gymcrm.entities.Trainer;
import io.gymcrm.exceptions.AuthenticationException;
import io.gymcrm.exceptions.NotFoundException;
import io.gymcrm.services.AuthenticationService;
import io.gymcrm.services.implementations.TrainerServiceImpl;
import io.gymcrm.util.PasswordGenerator;
import io.gymcrm.util.UsernameGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
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
import static support.TestData.FITNESS;
import static support.TestData.YOGA;
import static support.TestData.trainee;
import static support.TestData.trainer;

@ExtendWith(MockitoExtension.class)
class TrainerServiceImplTest {

    private static final Credentials ANNA = new Credentials("Anna.Lee", "secret");
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

    @BeforeEach
    void setUp() {
        service = new TrainerServiceImpl();
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
            when(trainingTypeDao.findByName("Yoga")).thenReturn(Optional.of(YOGA));
            when(usernameGenerator.generate("Anna", "Lee")).thenReturn("Anna.Lee");
            when(passwordGenerator.generate()).thenReturn("bbbbbbbbbb");
            when(trainerDao.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Trainer created = service.create(new TrainerRegistration("Anna", "Lee", "Yoga"));

            assertEquals("Anna.Lee", created.getUser().getUsername());
            assertEquals("bbbbbbbbbb", created.getUser().getPassword());
            assertTrue(created.getUser().isActive());
            assertSame(YOGA, created.getSpecialization());
            verifyNoInteractions(authenticationService);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "   ")
        void rejectsMissingSpecialization(String specialization) {
            assertThrows(IllegalArgumentException.class,
                    () -> service.create(new TrainerRegistration("Anna", "Lee", specialization)));
            verifyNoInteractions(trainerDao, usernameGenerator, passwordGenerator);
        }

        @Test
        void rejectsMissingNames() {
            assertThrows(IllegalArgumentException.class, () -> service.create(new TrainerRegistration(null, "Lee", "Yoga")));
            assertThrows(IllegalArgumentException.class, () -> service.create(new TrainerRegistration("Anna", "", "Yoga")));
            verifyNoInteractions(trainerDao, usernameGenerator);
        }

        @Test
        void rejectsUnknownSpecialization() {
            when(trainingTypeDao.findByName("Boxing")).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> service.create(new TrainerRegistration("Anna", "Lee", "Boxing")));
            verify(trainerDao, never()).save(any());
            verifyNoInteractions(usernameGenerator, passwordGenerator);
        }

        @Test
        void rejectsNullRegistration() {
            assertThrows(NullPointerException.class, () -> service.create(null));
            verifyNoInteractions(trainerDao);
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
        void changesNamesAndSpecialization() {
            Trainer anna = givenAnna();
            when(trainingTypeDao.findByName("Fitness")).thenReturn(Optional.of(FITNESS));

            service.update(ANNA, new TrainerUpdate("Ann", "Leigh", "Fitness"));

            assertEquals("Ann", anna.getUser().getFirstName());
            assertEquals("Leigh", anna.getUser().getLastName());
            assertSame(FITNESS, anna.getSpecialization());
            assertEquals("Anna.Lee", anna.getUser().getUsername());
            verify(authenticationService).authenticateTrainer(ANNA);
        }

        @Test
        void wrongCredentialsChangeNothing() {
            givenWrongTrainerCredentials();

            assertThrows(AuthenticationException.class,
                    () -> service.update(ANNA, new TrainerUpdate("Ann", "Lee", "Fitness")));
            verifyNoInteractions(trainerDao, trainingTypeDao);
        }

        @Test
        void unknownSpecializationChangesNothing() {
            when(trainingTypeDao.findByName("Boxing")).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> service.update(ANNA, new TrainerUpdate("Ann", "Lee", "Boxing")));
            verifyNoInteractions(trainerDao);
        }

        @Test
        void rejectsMissingFields() {
            assertThrows(IllegalArgumentException.class, () -> service.update(ANNA, new TrainerUpdate(null, "Lee", "Yoga")));
            assertThrows(IllegalArgumentException.class, () -> service.update(ANNA, new TrainerUpdate("Ann", "Lee", null)));
            assertThrows(NullPointerException.class, () -> service.update(ANNA, null));
            verifyNoInteractions(trainerDao);
        }
    }

    @Nested
    class ToggleActive {

        @Test
        void flipsTheFlagEachTime() {
            Trainer anna = givenAnna();

            assertFalse(service.toggleActive(ANNA));
            assertFalse(anna.getUser().isActive());
            assertTrue(service.toggleActive(ANNA));
        }

        @Test
        void wrongCredentialsChangeNothing() {
            givenWrongTrainerCredentials();

            assertThrows(AuthenticationException.class, () -> service.toggleActive(ANNA));
            verifyNoInteractions(trainerDao);
        }

        @Test
        void traineeCredentialsCantToggleATrainer() {
            Credentials john = new Credentials("John.Smith", "pw");
            doThrow(WRONG_PASSWORD).when(authenticationService).authenticateTrainer(john);

            assertThrows(AuthenticationException.class, () -> service.toggleActive(john));
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
