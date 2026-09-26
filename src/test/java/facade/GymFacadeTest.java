package facade;

import io.gymcrm.entities.Trainee;
import io.gymcrm.entities.Trainer;
import io.gymcrm.entities.Training;
import io.gymcrm.facade.GymFacade;
import io.gymcrm.services.TraineeService;
import io.gymcrm.services.TrainerService;
import io.gymcrm.services.TrainingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GymFacadeTest {

    @Mock
    private TraineeService traineeService;

    @Mock
    private TrainerService trainerService;

    @Mock
    private TrainingService trainingService;

    private GymFacade facade;

    @BeforeEach
    void setUp() {
        facade = new GymFacade(traineeService, trainerService, trainingService);
    }

    // Trainee

    @Test
    void createTraineeDelegates() {
        Trainee input = new Trainee();
        Trainee saved = new Trainee();
        when(traineeService.create(input)).thenReturn(saved);

        assertSame(saved, facade.createTrainee(input));
    }

    @Test
    void updateTraineeDelegates() {
        Trainee input = new Trainee();
        Trainee updated = new Trainee();
        when(traineeService.update(input)).thenReturn(updated);

        assertSame(updated, facade.updateTrainee(input));
    }

    @Test
    void deleteTraineeDelegates() {
        UUID id = UUID.randomUUID();

        facade.deleteTrainee(id);

        verify(traineeService).delete(id);
    }

    @Test
    void getTraineeByIdDelegates() {
        UUID id = UUID.randomUUID();
        Trainee trainee = new Trainee();
        when(traineeService.getById(id)).thenReturn(trainee);

        assertSame(trainee, facade.getTraineeById(id));
    }

    @Test
    void getTraineeByUsernameDelegates() {
        Trainee trainee = new Trainee();
        when(traineeService.getByUsername("John.Smith")).thenReturn(trainee);

        assertSame(trainee, facade.getTraineeByUsername("John.Smith"));
    }

    @Test
    void getAllTraineesDelegates() {
        List<Trainee> all = List.of(new Trainee());
        when(traineeService.getAll()).thenReturn(all);

        assertSame(all, facade.getAllTrainees());
    }

    // Trainer

    @Test
    void createTrainerDelegates() {
        Trainer input = new Trainer();
        Trainer saved = new Trainer();
        when(trainerService.create(input)).thenReturn(saved);

        assertSame(saved, facade.createTrainer(input));
    }

    @Test
    void updateTrainerDelegates() {
        Trainer input = new Trainer();
        Trainer updated = new Trainer();
        when(trainerService.update(input)).thenReturn(updated);

        assertSame(updated, facade.updateTrainer(input));
    }

    @Test
    void getTrainerByIdDelegates() {
        UUID id = UUID.randomUUID();
        Trainer trainer = new Trainer();
        when(trainerService.getById(id)).thenReturn(trainer);

        assertSame(trainer, facade.getTrainerById(id));
    }

    @Test
    void getTrainerByUsernameDelegates() {
        Trainer trainer = new Trainer();
        when(trainerService.getByUsername("Anna.Lee")).thenReturn(trainer);

        assertSame(trainer, facade.getTrainerByUsername("Anna.Lee"));
    }

    @Test
    void getAllTrainersDelegates() {
        List<Trainer> all = List.of(new Trainer());
        when(trainerService.getAll()).thenReturn(all);

        assertSame(all, facade.getAllTrainers());
    }

    // Training

    @Test
    void createTrainingDelegates() {
        Training input = new Training();
        Training saved = new Training();
        when(trainingService.create(input)).thenReturn(saved);

        assertSame(saved, facade.createTraining(input));
    }

    @Test
    void getTrainingByIdDelegates() {
        UUID id = UUID.randomUUID();
        Training training = new Training();
        when(trainingService.getById(id)).thenReturn(training);

        assertSame(training, facade.getTrainingById(id));
    }

    @Test
    void getAllTrainingsDelegates() {
        List<Training> all = List.of(new Training());
        when(trainingService.getAll()).thenReturn(all);

        assertSame(all, facade.getAllTrainings());
    }

    // Errors pass through unchanged

    @Test
    void serviceExceptionsPropagate() {
        UUID id = UUID.randomUUID();
        when(traineeService.getById(id)).thenThrow(new NoSuchElementException("not found"));

        assertThrows(NoSuchElementException.class, () -> facade.getTraineeById(id));
    }
}
