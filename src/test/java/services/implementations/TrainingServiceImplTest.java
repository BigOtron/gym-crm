package services.implementations;

import io.gymcrm.dao.TraineeDao;
import io.gymcrm.dao.TrainerDao;
import io.gymcrm.dao.TrainingDao;
import io.gymcrm.entities.Trainee;
import io.gymcrm.entities.Trainer;
import io.gymcrm.entities.Training;
import io.gymcrm.entities.TrainingType;
import io.gymcrm.services.implementations.TrainingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingServiceImplTest {

    private static final UUID TRAINEE_ID = UUID.randomUUID();
    private static final UUID TRAINER_ID = UUID.randomUUID();

    @Mock
    private TrainingDao trainingDao;

    @Mock
    private TraineeDao traineeDao;

    @Mock
    private TrainerDao trainerDao;

    private TrainingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TrainingServiceImpl();
        service.setTrainingDao(trainingDao);
        service.setTraineeDao(traineeDao);
        service.setTrainerDao(trainerDao);
    }

    private static Training training(TrainingType type) {
        Training training = new Training();
        training.setTraineeId(TRAINEE_ID);
        training.setTrainerId(TRAINER_ID);
        training.setTrainingName("Morning session");
        training.setTrainingType(type);
        training.setTrainingDate(LocalDate.of(2026, 9, 26));
        training.setTrainingDuration(Duration.ofMinutes(60));
        return training;
    }

    private static Trainee trainee() {
        Trainee trainee = new Trainee();
        trainee.setUserId(TRAINEE_ID);
        trainee.setUsername("John.Smith");
        return trainee;
    }

    private static Trainer trainer(TrainingType... types) {
        Trainer trainer = new Trainer();
        trainer.setUserId(TRAINER_ID);
        trainer.setUsername("Anna.Lee");
        for (TrainingType type : types) {
            trainer.addSpecialization(type);
        }
        return trainer;
    }

    // create

    @Test
    void createSavesValidTraining() {
        Training input = training(TrainingType.YOGA);
        when(traineeDao.findById(TRAINEE_ID)).thenReturn(Optional.of(trainee()));
        when(trainerDao.findById(TRAINER_ID)).thenReturn(Optional.of(trainer(TrainingType.YOGA)));
        when(trainingDao.create(any(Training.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Training result = service.create(input);

        assertSame(input, result);
        verify(trainingDao).create(input);
    }

    @Test
    void createClearsIncomingId() {
        Training input = training(TrainingType.YOGA);
        input.setTrainingId(UUID.randomUUID());
        when(traineeDao.findById(TRAINEE_ID)).thenReturn(Optional.of(trainee()));
        when(trainerDao.findById(TRAINER_ID)).thenReturn(Optional.of(trainer(TrainingType.YOGA)));
        when(trainingDao.create(any(Training.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Training result = service.create(input);

        assertNull(result.getTrainingId());
    }

    @Test
    void createWithUnknownTraineeThrows() {
        Training input = training(TrainingType.YOGA);
        when(traineeDao.findById(TRAINEE_ID)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.create(input));
        verify(trainingDao, never()).create(any());
    }

    @Test
    void createWithUnknownTrainerThrows() {
        Training input = training(TrainingType.YOGA);
        when(traineeDao.findById(TRAINEE_ID)).thenReturn(Optional.of(trainee()));
        when(trainerDao.findById(TRAINER_ID)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.create(input));
        verify(trainingDao, never()).create(any());
    }

    @Test
    void createWithTypeTrainerDoesNotTeachThrows() {
        Training input = training(TrainingType.YOGA);
        when(traineeDao.findById(TRAINEE_ID)).thenReturn(Optional.of(trainee()));
        when(trainerDao.findById(TRAINER_ID)).thenReturn(Optional.of(trainer(TrainingType.FITNESS)));

        assertThrows(IllegalArgumentException.class, () -> service.create(input));
        verify(trainingDao, never()).create(any());
    }

    @Test
    void createWithMissingNameThrows() {
        Training input = training(TrainingType.YOGA);
        input.setTrainingName(" ");

        assertThrows(IllegalArgumentException.class, () -> service.create(input));
        verifyNoInteractions(traineeDao, trainerDao, trainingDao);
    }

    @Test
    void createWithMissingDateThrows() {
        Training input = training(TrainingType.YOGA);
        input.setTrainingDate(null);

        assertThrows(IllegalArgumentException.class, () -> service.create(input));
        verifyNoInteractions(traineeDao, trainerDao, trainingDao);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -30})
    void createWithNonPositiveDurationThrows(long minutes) {
        Training input = training(TrainingType.YOGA);
        input.setTrainingDuration(Duration.ofMinutes(minutes));

        assertThrows(IllegalArgumentException.class, () -> service.create(input));
        verifyNoInteractions(traineeDao, trainerDao, trainingDao);
    }

    @Test
    void createNullThrows() {
        assertThrows(NullPointerException.class, () -> service.create(null));
        verifyNoInteractions(traineeDao, trainerDao, trainingDao);
    }

    // select

    @Test
    void getByIdReturnsTraining() {
        UUID id = UUID.randomUUID();
        Training stored = training(TrainingType.YOGA);
        stored.setTrainingId(id);
        when(trainingDao.findById(id)).thenReturn(Optional.of(stored));

        assertSame(stored, service.getById(id));
    }

    @Test
    void getByIdUnknownThrows() {
        UUID id = UUID.randomUUID();
        when(trainingDao.findById(id)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.getById(id));
    }

    @Test
    void getAllReturnsDaoList() {
        List<Training> all = List.of(training(TrainingType.YOGA));
        when(trainingDao.findAll()).thenReturn(all);

        assertSame(all, service.getAll());
    }
}
