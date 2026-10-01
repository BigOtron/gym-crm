package services.implementations;

import io.gymcrm.dao.TraineeDao;
import io.gymcrm.dao.TrainerDao;
import io.gymcrm.dao.UserDao;
import io.gymcrm.dto.Credentials;
import io.gymcrm.exceptions.AuthenticationException;
import io.gymcrm.services.implementations.AuthenticationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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

    @BeforeEach
    void setUp() {
        service = new AuthenticationServiceImpl();
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
}
