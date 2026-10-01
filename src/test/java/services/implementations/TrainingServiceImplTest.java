package services.implementations;

import io.gymcrm.dao.TraineeDao;
import io.gymcrm.dao.TrainerDao;
import io.gymcrm.dao.TrainingDao;
import io.gymcrm.dto.NewTraining;
import io.gymcrm.dto.TraineeTrainingCriteria;
import io.gymcrm.dto.TrainerTrainingCriteria;
import io.gymcrm.entities.Trainee;
import io.gymcrm.entities.Trainer;
import io.gymcrm.entities.Training;
import io.gymcrm.exceptions.NotFoundException;
import io.gymcrm.services.implementations.TrainingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static support.TestData.YOGA;
import static support.TestData.trainee;
import static support.TestData.trainer;

@ExtendWith(MockitoExtension.class)
class TrainingServiceImplTest {

    private static final LocalDate DATE = LocalDate.of(2026, 9, 20);

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

    private static NewTraining newTraining(Integer duration) {
        return new NewTraining("John.Smith", "Anna.Lee", "Evening yoga", DATE, duration);
    }

    @Test
    void createUsesTrainerSpecializationAndLinksTrainer() {
        Trainee trainee = trainee("John.Smith", "pw");
        Trainer trainer = trainer("Anna.Lee", "pw", YOGA);
        when(traineeDao.findByUsername("John.Smith")).thenReturn(Optional.of(trainee));
        when(trainerDao.findByUsername("Anna.Lee")).thenReturn(Optional.of(trainer));
        when(trainingDao.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Training created = service.create(newTraining(60));

        assertSame(trainee, created.getTrainee());
        assertSame(trainer, created.getTrainer());
        assertSame(YOGA, created.getTrainingType());
        assertEquals(60, created.getTrainingDuration());
        assertEquals(DATE, created.getTrainingDate());
        assertTrue(trainee.getTrainers().contains(trainer));
    }

    @Test
    void createRejectsUnknownTrainee() {
        when(traineeDao.findByUsername("John.Smith")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.create(newTraining(60)));
        verifyNoInteractions(trainerDao);
        verify(trainingDao, never()).save(any());
    }

    @Test
    void createRejectsUnknownTrainer() {
        when(traineeDao.findByUsername("John.Smith")).thenReturn(Optional.of(trainee("John.Smith", "pw")));
        when(trainerDao.findByUsername("Anna.Lee")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.create(newTraining(60)));
        verify(trainingDao, never()).save(any());
    }

    @Test
    void createRejectsInvalidFields() {
        assertThrows(IllegalArgumentException.class, () -> service.create(newTraining(0)));
        assertThrows(IllegalArgumentException.class, () -> service.create(newTraining(-5)));
        assertThrows(IllegalArgumentException.class, () -> service.create(newTraining(null)));
        assertThrows(IllegalArgumentException.class,
                () -> service.create(new NewTraining("John.Smith", "Anna.Lee", " ", DATE, 60)));
        assertThrows(IllegalArgumentException.class,
                () -> service.create(new NewTraining("John.Smith", "Anna.Lee", "Yoga", null, 60)));
        verifyNoInteractions(traineeDao, trainerDao, trainingDao);
    }

    @Test
    void traineeTrainingsPassCriteriaToDao() {
        var criteria = new TraineeTrainingCriteria(DATE, null, "Anna", "Yoga");
        when(traineeDao.findByUsername("John.Smith")).thenReturn(Optional.of(trainee("John.Smith", "pw")));
        when(trainingDao.findTraineeTrainings("John.Smith", criteria)).thenReturn(List.of());

        assertEquals(List.of(), service.getTraineeTrainings("John.Smith", criteria));
    }

    @Test
    void nullCriteriaMeansNoFilter() {
        when(trainerDao.findByUsername("Anna.Lee")).thenReturn(Optional.of(trainer("Anna.Lee", "pw", YOGA)));

        service.getTrainerTrainings("Anna.Lee", null);

        verify(trainingDao).findTrainerTrainings("Anna.Lee", TrainerTrainingCriteria.none());
    }

    @Test
    void trainingsOfUnknownUserThrow() {
        when(traineeDao.findByUsername("Nobody")).thenReturn(Optional.empty());
        when(trainerDao.findByUsername("Nobody")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getTraineeTrainings("Nobody", null));
        assertThrows(NotFoundException.class, () -> service.getTrainerTrainings("Nobody", null));
        verifyNoInteractions(trainingDao);
    }
}
