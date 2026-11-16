package facade;

import io.gymcrm.dto.Credentials;
import io.gymcrm.dto.NewTraining;
import io.gymcrm.dto.TraineeRegistration;
import io.gymcrm.dto.TraineeTrainingCriteria;
import io.gymcrm.dto.TraineeUpdate;
import io.gymcrm.dto.TrainerRegistration;
import io.gymcrm.dto.TrainerTrainingCriteria;
import io.gymcrm.dto.TrainerUpdate;
import io.gymcrm.entities.Trainee;
import io.gymcrm.entities.Trainer;
import io.gymcrm.entities.Training;
import io.gymcrm.entities.TrainingType;
import io.gymcrm.exceptions.AuthenticationException;
import io.gymcrm.exceptions.ForbiddenException;
import io.gymcrm.facade.GymFacade;
import io.gymcrm.services.AuthenticationService;
import io.gymcrm.services.TraineeService;
import io.gymcrm.services.TrainerService;
import io.gymcrm.services.TrainingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The facade only delegates: authentication and the "own profile only" rule are the services' job
 * (see the service tests). The one exception is the role-independent password change.
 */
@ExtendWith(MockitoExtension.class)
class GymFacadeTest {

    private static final Credentials JOHN = new Credentials("John.Smith", "secret");
    private static final Credentials ANNA = new Credentials("Anna.Lee", "secret");

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private TraineeService traineeService;

    @Mock
    private TrainerService trainerService;

    @Mock
    private TrainingService trainingService;

    private GymFacade facade;

    @BeforeEach
    void setUp() {
        facade = new GymFacade(authenticationService, traineeService, trainerService, trainingService);
    }

    @Test
    void createProfiles() {
        var traineeRegistration = new TraineeRegistration("John", "Smith", null, null);
        var trainerRegistration = new TrainerRegistration("Anna", "Lee", 6L);
        Trainee trainee = new Trainee();
        Trainer trainer = new Trainer();
        when(traineeService.create(traineeRegistration)).thenReturn(trainee);
        when(trainerService.create(trainerRegistration)).thenReturn(trainer);

        assertSame(trainee, facade.createTrainee(traineeRegistration));
        assertSame(trainer, facade.createTrainer(trainerRegistration));
        verifyNoInteractions(authenticationService);
    }

    @Test
    void credentialsMatchingAndLogin() {
        when(authenticationService.traineeCredentialsMatch(JOHN)).thenReturn(true);
        when(authenticationService.trainerCredentialsMatch(JOHN)).thenReturn(false);

        assertTrue(facade.traineeCredentialsMatch(JOHN));
        assertFalse(facade.trainerCredentialsMatch(JOHN));
        facade.login(JOHN);
        verify(authenticationService).login(JOHN);
    }

    @Test
    void loginWithWrongCredentials() {
        doThrow(new AuthenticationException("bad")).when(authenticationService).login(JOHN);

        assertThrows(AuthenticationException.class, () -> facade.login(JOHN));
    }

    @Test
    void traineeOperationsPassTheCredentialsOn() {
        var update = new TraineeUpdate("John", "Smith", null, null, true);
        Trainee trainee = new Trainee();
        List<Trainer> trainers = List.of(new Trainer());
        when(traineeService.getByUsername(ANNA, "John.Smith")).thenReturn(trainee);
        when(traineeService.update(JOHN, "John.Smith", update)).thenReturn(trainee);
        when(traineeService.updateTrainers(JOHN, "John.Smith", List.of("Anna.Lee"))).thenReturn(trainers);
        when(trainerService.getNotAssignedToTrainee(JOHN, "John.Smith")).thenReturn(trainers);

        assertSame(trainee, facade.getTraineeByUsername(ANNA, "John.Smith"));
        assertSame(trainee, facade.updateTrainee(JOHN, "John.Smith", update));
        facade.setTraineeActive(JOHN, "John.Smith", false);
        facade.deleteTrainee(JOHN, "John.Smith");
        assertSame(trainers, facade.updateTraineeTrainers(JOHN, "John.Smith", List.of("Anna.Lee")));
        assertSame(trainers, facade.getTrainersNotAssignedToTrainee(JOHN, "John.Smith"));

        verify(traineeService).setActive(JOHN, "John.Smith", false);
        verify(traineeService).delete(JOHN, "John.Smith");
        verifyNoInteractions(authenticationService);
    }

    @Test
    void trainerOperationsPassTheCredentialsOn() {
        var update = new TrainerUpdate("Anna", "Lee", true);
        Trainer trainer = new Trainer();
        when(trainerService.getByUsername(JOHN, "Anna.Lee")).thenReturn(trainer);
        when(trainerService.update(ANNA, "Anna.Lee", update)).thenReturn(trainer);

        assertSame(trainer, facade.getTrainerByUsername(JOHN, "Anna.Lee"));
        assertSame(trainer, facade.updateTrainer(ANNA, "Anna.Lee", update));
        facade.setTrainerActive(ANNA, "Anna.Lee", false);

        verify(trainerService).setActive(ANNA, "Anna.Lee", false);
        verifyNoInteractions(authenticationService);
    }

    @Test
    void trainingOperationsPassTheCredentialsOn() {
        var newTraining = new NewTraining("John.Smith", "Anna.Lee", "Yoga", LocalDate.now(), 60);
        var traineeCriteria = new TraineeTrainingCriteria(null, null, "Anna", null);
        var trainerCriteria = new TrainerTrainingCriteria(null, null, "John");
        Training training = new Training();
        List<Training> trainings = List.of(training);
        List<TrainingType> types = List.of();
        when(trainingService.create(JOHN, newTraining)).thenReturn(training);
        when(trainingService.getTraineeTrainings(JOHN, "John.Smith", traineeCriteria)).thenReturn(trainings);
        when(trainingService.getTrainerTrainings(ANNA, "Anna.Lee", trainerCriteria)).thenReturn(trainings);
        when(trainingService.getTrainingTypes(JOHN)).thenReturn(types);

        assertSame(training, facade.addTraining(JOHN, newTraining));
        assertSame(trainings, facade.getTraineeTrainings(JOHN, "John.Smith", traineeCriteria));
        assertSame(trainings, facade.getTrainerTrainings(ANNA, "Anna.Lee", trainerCriteria));
        assertSame(types, facade.getTrainingTypes(JOHN));
        verifyNoInteractions(authenticationService);
    }

    @Test
    void serviceErrorsReachTheCaller() {
        doThrow(new ForbiddenException("not yours")).when(traineeService).delete(ANNA, "John.Smith");
        when(trainingService.getTrainingTypes(JOHN)).thenThrow(new AuthenticationException("bad"));

        assertThrows(ForbiddenException.class, () -> facade.deleteTrainee(ANNA, "John.Smith"));
        assertThrows(AuthenticationException.class, () -> facade.getTrainingTypes(JOHN));
    }

    // Password change: the only place where the facade decides something

    @Test
    void changePasswordOfTrainee() {
        when(authenticationService.traineeCredentialsMatch(JOHN)).thenReturn(true);

        facade.changePassword(JOHN, "newPassword");

        InOrder order = inOrder(authenticationService, traineeService);
        order.verify(authenticationService).authenticate(JOHN);
        order.verify(traineeService).changePassword(JOHN, "newPassword");
        verifyNoInteractions(trainerService);
    }

    @Test
    void changePasswordOfTrainer() {
        when(authenticationService.traineeCredentialsMatch(ANNA)).thenReturn(false);

        facade.changePassword(ANNA, "newPassword");

        InOrder order = inOrder(authenticationService, trainerService);
        order.verify(authenticationService).authenticate(ANNA);
        order.verify(trainerService).changePassword(ANNA, "newPassword");
        verifyNoInteractions(traineeService);
    }

    @Test
    void changePasswordWithWrongOldPasswordChangesNothing() {
        doThrow(new AuthenticationException("bad")).when(authenticationService).authenticate(JOHN);

        assertThrows(AuthenticationException.class, () -> facade.changePassword(JOHN, "newPassword"));
        verify(authenticationService, never()).traineeCredentialsMatch(any());
        verifyNoInteractions(traineeService, trainerService);
    }
}
