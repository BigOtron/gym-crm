package facade;

import io.gymcrm.dto.CreatedProfile;
import io.gymcrm.dto.Credentials;
import io.gymcrm.dto.IssuedToken;
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
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The facade only delegates; who may do what is enforced by the services.
 */
@ExtendWith(MockitoExtension.class)
class GymFacadeTest {

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
    void registrationsAndLogin() {
        var traineeRegistration = new TraineeRegistration("John", "Smith", null, null);
        var trainerRegistration = new TrainerRegistration("Anna", "Lee", 6L);
        var token = new IssuedToken("jwt", 3600);
        var createdTrainee = new CreatedProfile<>(new Trainee(), "pw1", token);
        var createdTrainer = new CreatedProfile<>(new Trainer(), "pw2", token);
        var credentials = new Credentials("John.Smith", "secret");
        when(traineeService.create(traineeRegistration)).thenReturn(createdTrainee);
        when(trainerService.create(trainerRegistration)).thenReturn(createdTrainer);
        when(authenticationService.login(credentials)).thenReturn(token);

        assertSame(createdTrainee, facade.createTrainee(traineeRegistration));
        assertSame(createdTrainer, facade.createTrainer(trainerRegistration));
        assertSame(token, facade.login(credentials));
    }

    @Test
    void changePassword() {
        facade.changePassword("John.Smith", "old", "new");

        verify(authenticationService).changePassword("John.Smith", "old", "new");
    }

    @Test
    void traineeOperations() {
        var update = new TraineeUpdate("John", "Smith", null, null, true);
        Trainee trainee = new Trainee();
        List<Trainer> trainers = List.of(new Trainer());
        when(traineeService.getByUsername("John.Smith")).thenReturn(trainee);
        when(traineeService.update("John.Smith", update)).thenReturn(trainee);
        when(traineeService.updateTrainers("John.Smith", List.of("Anna.Lee"))).thenReturn(trainers);
        when(trainerService.getNotAssignedToTrainee("John.Smith")).thenReturn(trainers);

        assertSame(trainee, facade.getTraineeByUsername("John.Smith"));
        assertSame(trainee, facade.updateTrainee("John.Smith", update));
        facade.setTraineeActive("John.Smith", false);
        facade.deleteTrainee("John.Smith");
        assertSame(trainers, facade.updateTraineeTrainers("John.Smith", List.of("Anna.Lee")));
        assertSame(trainers, facade.getTrainersNotAssignedToTrainee("John.Smith"));

        verify(traineeService).setActive("John.Smith", false);
        verify(traineeService).delete("John.Smith");
    }

    @Test
    void trainerOperations() {
        var update = new TrainerUpdate("Anna", "Lee", true);
        Trainer trainer = new Trainer();
        when(trainerService.getByUsername("Anna.Lee")).thenReturn(trainer);
        when(trainerService.update("Anna.Lee", update)).thenReturn(trainer);

        assertSame(trainer, facade.getTrainerByUsername("Anna.Lee"));
        assertSame(trainer, facade.updateTrainer("Anna.Lee", update));
        facade.setTrainerActive("Anna.Lee", false);

        verify(trainerService).setActive("Anna.Lee", false);
    }

    @Test
    void trainingOperations() {
        var newTraining = new NewTraining("John.Smith", "Anna.Lee", "Yoga", LocalDate.now(), 60);
        var traineeCriteria = new TraineeTrainingCriteria(null, null, "Anna", null);
        var trainerCriteria = new TrainerTrainingCriteria(null, null, "John");
        Training training = new Training();
        List<Training> trainings = List.of(training);
        List<TrainingType> types = List.of();
        when(trainingService.create(newTraining)).thenReturn(training);
        when(trainingService.getTraineeTrainings("John.Smith", traineeCriteria)).thenReturn(trainings);
        when(trainingService.getTrainerTrainings("Anna.Lee", trainerCriteria)).thenReturn(trainings);
        when(trainingService.getTrainingTypes()).thenReturn(types);

        assertSame(training, facade.addTraining(newTraining));
        assertSame(trainings, facade.getTraineeTrainings("John.Smith", traineeCriteria));
        assertSame(trainings, facade.getTrainerTrainings("Anna.Lee", trainerCriteria));
        assertSame(types, facade.getTrainingTypes());
    }

    @Test
    void serviceErrorsReachTheCaller() {
        doThrow(new AccessDeniedException("not yours")).when(traineeService).delete("John.Smith");
        when(authenticationService.login(new Credentials("x", "y"))).thenThrow(new AuthenticationException("bad"));

        assertThrows(AccessDeniedException.class, () -> facade.deleteTrainee("John.Smith"));
        assertThrows(AuthenticationException.class, () -> facade.login(new Credentials("x", "y")));
    }
}
