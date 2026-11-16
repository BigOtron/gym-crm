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
import io.gymcrm.exceptions.NotFoundException;
import io.gymcrm.facade.GymFacade;
import io.gymcrm.services.AuthenticationService;
import io.gymcrm.services.TraineeService;
import io.gymcrm.services.TrainerService;
import io.gymcrm.services.TrainingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
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
    void createProfiles() {
        var traineeRegistration = new TraineeRegistration("John", "Smith", null, null);
        var trainerRegistration = new TrainerRegistration("Anna", "Lee", "Yoga");
        Trainee trainee = new Trainee();
        Trainer trainer = new Trainer();
        when(traineeService.create(traineeRegistration)).thenReturn(trainee);
        when(trainerService.create(trainerRegistration)).thenReturn(trainer);

        assertSame(trainee, facade.createTrainee(traineeRegistration));
        assertSame(trainer, facade.createTrainer(trainerRegistration));
        verifyNoInteractions(authenticationService);
    }

    @Test
    void credentialsMatching() {
        when(authenticationService.traineeCredentialsMatch(JOHN)).thenReturn(true);
        when(authenticationService.trainerCredentialsMatch(JOHN)).thenReturn(false);

        assertTrue(facade.traineeCredentialsMatch(JOHN));
        assertFalse(facade.trainerCredentialsMatch(JOHN));
    }

    @Test
    void traineeOperationsPassTheCredentialsOn() {
        var update = new TraineeUpdate("John", "Smith", null, null);
        Trainee trainee = new Trainee();
        List<Trainer> trainers = List.of(new Trainer());
        when(traineeService.getByUsername(ANNA, "John.Smith")).thenReturn(trainee);
        when(traineeService.update(JOHN, update)).thenReturn(trainee);
        when(traineeService.toggleActive(JOHN)).thenReturn(false);
        when(traineeService.updateTrainers(JOHN, List.of("Anna.Lee"))).thenReturn(trainers);

        assertSame(trainee, facade.getTraineeByUsername(ANNA, "John.Smith"));
        facade.changeTraineePassword(JOHN, "newPassword");
        assertSame(trainee, facade.updateTrainee(JOHN, update));
        assertFalse(facade.toggleTraineeActive(JOHN));
        facade.deleteTrainee(JOHN);
        assertSame(trainers, facade.updateTraineeTrainers(JOHN, List.of("Anna.Lee")));

        verify(traineeService).changePassword(JOHN, "newPassword");
        verify(traineeService).delete(JOHN);
        verifyNoInteractions(authenticationService);
    }

    @Test
    void trainerOperationsPassTheCredentialsOn() {
        var update = new TrainerUpdate("Anna", "Lee", "Yoga");
        Trainer trainer = new Trainer();
        List<Trainer> free = List.of(trainer);
        when(trainerService.getByUsername(JOHN, "Anna.Lee")).thenReturn(trainer);
        when(trainerService.update(ANNA, update)).thenReturn(trainer);
        when(trainerService.toggleActive(ANNA)).thenReturn(true);
        when(trainerService.getNotAssignedToTrainee(JOHN, "John.Smith")).thenReturn(free);

        assertSame(trainer, facade.getTrainerByUsername(JOHN, "Anna.Lee"));
        facade.changeTrainerPassword(ANNA, "newPassword");
        assertSame(trainer, facade.updateTrainer(ANNA, update));
        assertTrue(facade.toggleTrainerActive(ANNA));
        assertSame(free, facade.getTrainersNotAssignedToTrainee(JOHN, "John.Smith"));

        verify(trainerService).changePassword(ANNA, "newPassword");
        verifyNoInteractions(authenticationService);
    }

    @Test
    void trainingOperationsPassTheCredentialsOn() {
        var newTraining = new NewTraining("John.Smith", "Anna.Lee", "Yoga", LocalDate.now(), 60);
        var traineeCriteria = new TraineeTrainingCriteria(null, null, "Anna", null);
        var trainerCriteria = new TrainerTrainingCriteria(null, null, "John");
        Training training = new Training();
        List<Training> trainings = List.of(training);
        when(trainingService.create(JOHN, newTraining)).thenReturn(training);
        when(trainingService.getTraineeTrainings(JOHN, "John.Smith", traineeCriteria)).thenReturn(trainings);
        when(trainingService.getTrainerTrainings(ANNA, "Anna.Lee", trainerCriteria)).thenReturn(trainings);

        assertSame(training, facade.addTraining(JOHN, newTraining));
        assertSame(trainings, facade.getTraineeTrainings(JOHN, "John.Smith", traineeCriteria));
        assertSame(trainings, facade.getTrainerTrainings(ANNA, "Anna.Lee", trainerCriteria));
        verifyNoInteractions(authenticationService);
    }

    @Test
    void serviceErrorsReachTheCaller() {
        doThrow(new AuthenticationException("bad")).when(traineeService).delete(JOHN);
        when(trainerService.getByUsername(JOHN, "Nobody")).thenThrow(new NotFoundException("Trainer not found"));

        assertThrows(AuthenticationException.class, () -> facade.deleteTrainee(JOHN));
        assertThrows(NotFoundException.class, () -> facade.getTrainerByUsername(JOHN, "Nobody"));
    }
}
