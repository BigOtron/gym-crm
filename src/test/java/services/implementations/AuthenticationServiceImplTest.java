package services.implementations;

import io.gymcrm.dao.TraineeDao;
import io.gymcrm.dao.TrainerDao;
import io.gymcrm.dao.UserDao;
import io.gymcrm.dto.Credentials;
import io.gymcrm.exceptions.AuthenticationException;
import io.gymcrm.services.implementations.AuthenticationServiceImpl;
import io.gymcrm.metrics.GymMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static support.TestData.YOGA;
import static support.TestData.trainee;
import static support.TestData.trainer;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {

    @Mock
    private UserDao userDao;

    @Mock
    private TraineeDao traineeDao;

    @Mock
    private TrainerDao trainerDao;

    private AuthenticationServiceImpl service;

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

    @BeforeEach
    void setUp() {
        service = new AuthenticationServiceImpl();
        service.setMetrics(new GymMetrics(registry));
        service.setUserDao(userDao);
        service.setTraineeDao(traineeDao);
        service.setTrainerDao(trainerDao);
    }

    @Test
    void traineeCredentialsMatchWithRightPassword() {
        when(traineeDao.findByUsername("John.Smith")).thenReturn(Optional.of(trainee("John.Smith", "secret")));

        assertTrue(service.traineeCredentialsMatch(new Credentials("John.Smith", "secret")));
        assertFalse(service.traineeCredentialsMatch(new Credentials("John.Smith", "wrong")));
    }

    @Test
    void traineeCredentialsDontMatchUnknownUser() {
        when(traineeDao.findByUsername("Nobody")).thenReturn(Optional.empty());

        assertFalse(service.traineeCredentialsMatch(new Credentials("Nobody", "secret")));
    }

    @Test
    void incompleteCredentialsNeverMatchAndSkipTheDatabase() {
        assertFalse(service.traineeCredentialsMatch(null));
        assertFalse(service.traineeCredentialsMatch(new Credentials("John.Smith", null)));
        assertFalse(service.trainerCredentialsMatch(new Credentials(null, "secret")));
        verifyNoInteractions(traineeDao, trainerDao);
    }

    @Test
    void trainerCredentialsMatch() {
        when(trainerDao.findByUsername("Anna.Lee")).thenReturn(Optional.of(trainer("Anna.Lee", "pw", YOGA)));

        assertTrue(service.trainerCredentialsMatch(new Credentials("Anna.Lee", "pw")));
    }

    @Test
    void authenticateTraineeThrowsOnMismatch() {
        when(traineeDao.findByUsername("John.Smith")).thenReturn(Optional.of(trainee("John.Smith", "secret")));

        assertDoesNotThrow(() -> service.authenticateTrainee(new Credentials("John.Smith", "secret")));
        assertThrows(AuthenticationException.class,
                () -> service.authenticateTrainee(new Credentials("John.Smith", "wrong")));
    }

    @Test
    void authenticateTrainerThrowsForTrainee() {
        when(trainerDao.findByUsername("John.Smith")).thenReturn(Optional.empty());

        assertThrows(AuthenticationException.class,
                () -> service.authenticateTrainer(new Credentials("John.Smith", "secret")));
    }

    @Test
    void authenticateAnyUser() {
        when(userDao.findByUsername("Anna.Lee"))
                .thenReturn(Optional.of(trainer("Anna.Lee", "pw", YOGA).getUser()));
        when(userDao.findByUsername("Nobody")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> service.authenticate(new Credentials("Anna.Lee", "pw")));
        assertThrows(AuthenticationException.class, () -> service.authenticate(new Credentials("Anna.Lee", "x")));
        assertThrows(AuthenticationException.class, () -> service.authenticate(new Credentials("Nobody", "pw")));
        assertThrows(AuthenticationException.class, () -> service.authenticate(null));
    }

    @Test
    void passwordComparisonIsExact() {
        when(traineeDao.findByUsername("John.Smith")).thenReturn(Optional.of(trainee("John.Smith", "Secret")));

        assertFalse(service.traineeCredentialsMatch(new Credentials("John.Smith", "secret")));
        assertFalse(service.traineeCredentialsMatch(new Credentials("John.Smith", "Secret ")));
        assertFalse(service.traineeCredentialsMatch(new Credentials("John.Smith", "")));
        assertTrue(service.traineeCredentialsMatch(new Credentials("John.Smith", "Secret")));
    }

    @Test
    void authenticateTraineeRejectsATrainerWithTheRightPassword() {
        when(traineeDao.findByUsername("Anna.Lee")).thenReturn(Optional.empty());

        assertThrows(AuthenticationException.class,
                () -> service.authenticateTrainee(new Credentials("Anna.Lee", "pw")));
    }

    @Test
    void authenticateTrainerRejectsWrongPassword() {
        when(trainerDao.findByUsername("Anna.Lee")).thenReturn(Optional.of(trainer("Anna.Lee", "pw", YOGA)));

        assertThrows(AuthenticationException.class,
                () -> service.authenticateTrainer(new Credentials("Anna.Lee", "wrong")));
    }

    @Test
    void incompleteCredentialsAreRejectedWithoutQueries() {
        assertThrows(AuthenticationException.class, () -> service.authenticateTrainee(null));
        assertThrows(AuthenticationException.class, () -> service.authenticateTrainer(new Credentials(null, "pw")));
        assertThrows(AuthenticationException.class, () -> service.authenticate(new Credentials("Anna.Lee", null)));
        verifyNoInteractions(userDao, traineeDao, trainerDao);
    }

    @Test
    void failureMessageDoesNotRevealWhetherTheUserExists() {
        when(userDao.findByUsername("Nobody")).thenReturn(Optional.empty());
        when(userDao.findByUsername("Anna.Lee")).thenReturn(Optional.of(trainer("Anna.Lee", "pw", YOGA).getUser()));

        String unknownUser = assertThrows(AuthenticationException.class,
                () -> service.authenticate(new Credentials("Nobody", "pw"))).getMessage();
        String wrongPassword = assertThrows(AuthenticationException.class,
                () -> service.authenticate(new Credentials("Anna.Lee", "wrong"))).getMessage();

        assertEquals(unknownUser, wrongPassword);
    }

    private double count(String name, String tag, String value) {
        return registry.get(name).tag(tag, value).counter().count();
    }

    @Test
    void successfulLoginIsCounted() {
        when(userDao.findByUsername("Anna.Lee")).thenReturn(Optional.of(trainer("Anna.Lee", "pw", YOGA).getUser()));

        service.login(new Credentials("Anna.Lee", "pw"));

        assertEquals(1, count(GymMetrics.LOGINS, "result", "success"));
        assertEquals(0, count(GymMetrics.LOGINS, "result", "failure"));
    }

    @Test
    void failedLoginIsCountedAndStillRejected() {
        when(userDao.findByUsername("Anna.Lee")).thenReturn(Optional.of(trainer("Anna.Lee", "pw", YOGA).getUser()));

        assertThrows(AuthenticationException.class, () -> service.login(new Credentials("Anna.Lee", "wrong")));

        assertEquals(1, count(GymMetrics.LOGINS, "result", "failure"));
        assertEquals(0, count(GymMetrics.LOGINS, "result", "success"));
    }

    @Test
    void everyRejectionIsCountedWithTheRequiredRole() {
        when(traineeDao.findByUsername("John.Smith")).thenReturn(Optional.empty());

        assertThrows(AuthenticationException.class, () -> service.authenticateTrainee(new Credentials("John.Smith", "x")));
        assertThrows(AuthenticationException.class, () -> service.authenticateTrainer(null));

        assertEquals(1, count(GymMetrics.AUTHENTICATION_FAILURES, "role", "trainee"));
        assertEquals(1, count(GymMetrics.AUTHENTICATION_FAILURES, "role", "trainer"));
    }

    @Test
    void credentialChecksThatOnlyAnswerYesOrNoAreNotCounted() {
        when(traineeDao.findByUsername("John.Smith")).thenReturn(Optional.empty());

        assertFalse(service.traineeCredentialsMatch(new Credentials("John.Smith", "x")));

        assertTrue(registry.find(GymMetrics.AUTHENTICATION_FAILURES).counters().isEmpty());
    }
}
