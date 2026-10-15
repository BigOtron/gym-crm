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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
    void createProfilesDoNotAuthenticate() {
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
    void credentialsMatchingDelegates() {
        when(authenticationService.traineeCredentialsMatch(JOHN)).thenReturn(true);
        when(authenticationService.trainerCredentialsMatch(ANNA)).thenReturn(true);

        assertTrue(facade.traineeCredentialsMatch(JOHN));
        assertTrue(facade.trainerCredentialsMatch(ANNA));
    }

    @Test
    void selectsAuthenticateAnyUser() {
        facade.getTraineeByUsername(ANNA, "John.Smith");
        facade.getTrainerByUsername(JOHN, "Anna.Lee");

        verify(authenticationService).authenticate(ANNA);
        verify(authenticationService).authenticate(JOHN);
        verify(traineeService).getByUsername("John.Smith");
        verify(trainerService).getByUsername("Anna.Lee");
    }

    @Test
    void traineeOperationsAuthenticateTraineeFirst() {
        var update = new TraineeUpdate("John", "Smith", null, null, true);

        facade.updateTrainee(JOHN, "John.Smith", update);
        facade.setTraineeActive(JOHN, "John.Smith", false);
        facade.deleteTrainee(JOHN, "John.Smith");
        facade.updateTraineeTrainers(JOHN, "John.Smith", List.of("Anna.Lee"));

        InOrder order = inOrder(authenticationService, traineeService);
        order.verify(authenticationService).authenticateTrainee(JOHN);
        order.verify(traineeService).update("John.Smith", update);
        order.verify(authenticationService).authenticateTrainee(JOHN);
        order.verify(traineeService).setActive("John.Smith", false);
        order.verify(authenticationService).authenticateTrainee(JOHN);
        order.verify(traineeService).deleteByUsername("John.Smith");
        order.verify(authenticationService).authenticateTrainee(JOHN);
        order.verify(traineeService).updateTrainers("John.Smith", List.of("Anna.Lee"));
    }

    @Test
    void trainerOperationsAuthenticateTrainerFirst() {
        var update = new TrainerUpdate("Anna", "Lee", true);

        facade.updateTrainer(ANNA, "Anna.Lee", update);
        facade.setTrainerActive(ANNA, "Anna.Lee", false);

        InOrder order = inOrder(authenticationService, trainerService);
        order.verify(authenticationService).authenticateTrainer(ANNA);
        order.verify(trainerService).update("Anna.Lee", update);
        order.verify(authenticationService).authenticateTrainer(ANNA);
        order.verify(trainerService).setActive("Anna.Lee", false);
    }

    @Test
    void changingSomeoneElsesProfileIsForbidden() {
        var outsider = new Credentials("David.Brown", "secret");

        assertThrows(ForbiddenException.class,
                () -> facade.updateTrainee(outsider, "John.Smith", new TraineeUpdate("J", "S", null, null, true)));
        assertThrows(ForbiddenException.class, () -> facade.setTraineeActive(outsider, "John.Smith", false));
        assertThrows(ForbiddenException.class, () -> facade.deleteTrainee(outsider, "John.Smith"));
        assertThrows(ForbiddenException.class, () -> facade.updateTraineeTrainers(outsider, "John.Smith", List.of()));
        assertThrows(ForbiddenException.class,
                () -> facade.updateTrainer(outsider, "Anna.Lee", new TrainerUpdate("A", "L", true)));
        assertThrows(ForbiddenException.class, () -> facade.setTrainerActive(outsider, "Anna.Lee", false));
        verifyNoInteractions(traineeService, trainerService);
    }

    @Test
    void changePasswordOfTrainee() {
        when(authenticationService.traineeCredentialsMatch(JOHN)).thenReturn(true);

        facade.changePassword(JOHN, "newPassword");

        InOrder order = inOrder(authenticationService, traineeService);
        order.verify(authenticationService).authenticate(JOHN);
        order.verify(traineeService).changePassword("John.Smith", "newPassword");
        verifyNoInteractions(trainerService);
    }

    @Test
    void changePasswordOfTrainer() {
        when(authenticationService.traineeCredentialsMatch(ANNA)).thenReturn(false);

        facade.changePassword(ANNA, "newPassword");

        InOrder order = inOrder(authenticationService, trainerService);
        order.verify(authenticationService).authenticate(ANNA);
        order.verify(trainerService).changePassword("Anna.Lee", "newPassword");
        verify(authenticationService, never()).authenticateTrainer(any());
        verifyNoInteractions(traineeService);
    }

    @Test
    void changePasswordWithWrongOldPasswordFails() {
        doThrow(new AuthenticationException("bad")).when(authenticationService).authenticate(JOHN);

        assertThrows(AuthenticationException.class, () -> facade.changePassword(JOHN, "newPassword"));
        verify(authenticationService, never()).traineeCredentialsMatch(any());
        verify(authenticationService, never()).authenticateTrainer(any());
        verifyNoInteractions(traineeService, trainerService);
    }

    @Test
    void loginAndTrainingTypesAuthenticate() {
        facade.login(JOHN);
        facade.getTrainingTypes(JOHN);

        verify(authenticationService, times(2)).authenticate(JOHN);
        verify(trainingService).getTrainingTypes();
    }

    @Test
    void failedAuthenticationStopsTheOperation() {
        doThrow(new AuthenticationException("bad")).when(authenticationService).authenticateTrainee(JOHN);
        doThrow(new AuthenticationException("bad")).when(authenticationService).authenticate(JOHN);

        assertThrows(AuthenticationException.class, () -> facade.deleteTrainee(JOHN, "John.Smith"));
        assertThrows(AuthenticationException.class, () -> facade.getTraineeByUsername(JOHN, "John.Smith"));
        assertThrows(AuthenticationException.class,
                () -> facade.getTraineeTrainings(JOHN, "John.Smith", TraineeTrainingCriteria.none()));
        verifyNoInteractions(traineeService, trainingService);
    }

    @Test
    void trainingListsDelegate() {
        var traineeCriteria = new TraineeTrainingCriteria(LocalDate.MIN, null, "Anna", "Yoga");
        var trainerCriteria = new TrainerTrainingCriteria(null, LocalDate.MAX, "John");
        List<Training> trainings = List.of(new Training());
        when(trainingService.getTraineeTrainings("John.Smith", traineeCriteria)).thenReturn(trainings);
        when(trainingService.getTrainerTrainings("Anna.Lee", trainerCriteria)).thenReturn(trainings);

        assertSame(trainings, facade.getTraineeTrainings(JOHN, "John.Smith", traineeCriteria));
        assertSame(trainings, facade.getTrainerTrainings(ANNA, "Anna.Lee", trainerCriteria));
    }

    @Test
    void addTrainingByParticipant() {
        var newTraining = new NewTraining("John.Smith", "Anna.Lee", "Yoga", LocalDate.now(), 60);
        Training training = new Training();
        when(trainingService.create(newTraining)).thenReturn(training);

        assertSame(training, facade.addTraining(ANNA, newTraining));
        assertSame(training, facade.addTraining(JOHN, newTraining));
    }

    @Test
    void addTrainingByOutsiderIsRejected() {
        var newTraining = new NewTraining("John.Smith", "Anna.Lee", "Yoga", LocalDate.now(), 60);
        var outsider = new Credentials("David.Brown", "secret");

        assertThrows(ForbiddenException.class, () -> facade.addTraining(outsider, newTraining));
        verify(authenticationService).authenticate(outsider);
        verifyNoInteractions(trainingService);
    }

    @Test
    void notAssignedTrainersDelegates() {
        List<Trainer> trainers = List.of(new Trainer());
        when(trainerService.getNotAssignedToTrainee("John.Smith")).thenReturn(trainers);

        assertEquals(trainers, facade.getTrainersNotAssignedToTrainee(JOHN, "John.Smith"));
        verify(authenticationService).authenticate(JOHN);
        verify(trainerService).getNotAssignedToTrainee(any());
    }
}
