package services.implementations;

import io.gymcrm.dao.TraineeDao;
import io.gymcrm.dao.TrainerDao;
import io.gymcrm.dao.TrainingDao;
import io.gymcrm.dao.TrainingTypeDao;
import io.gymcrm.dto.NewTraining;
import io.gymcrm.dto.TraineeTrainingCriteria;
import io.gymcrm.dto.TrainerTrainingCriteria;
import io.gymcrm.entities.Trainee;
import io.gymcrm.entities.Trainer;
import io.gymcrm.entities.Training;
import io.gymcrm.exceptions.NotFoundException;
import io.gymcrm.services.implementations.TrainingServiceImpl;
import io.gymcrm.metrics.GymMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import static support.TestData.FITNESS;
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

    @Mock
    private TrainingTypeDao trainingTypeDao;

    private TrainingServiceImpl service;

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

    @BeforeEach
    void setUp() {
        service = new TrainingServiceImpl();
        service.setMetrics(new GymMetrics(registry));
        service.setTrainingDao(trainingDao);
        service.setTraineeDao(traineeDao);
        service.setTrainerDao(trainerDao);
        service.setTrainingTypeDao(trainingTypeDao);
    }

    private static NewTraining newTraining(Integer duration) {
        return new NewTraining("John.Smith", "Anna.Lee", "Evening yoga", DATE, duration);
    }

    @Nested
    class Create {

        private Trainee john;
        private Trainer anna;

        private void givenPeople() {
            john = trainee("John.Smith", "pw");
            anna = trainer("Anna.Lee", "pw", YOGA);
            when(traineeDao.findByUsername("John.Smith")).thenReturn(Optional.of(john));
            when(trainerDao.findByUsername("Anna.Lee")).thenReturn(Optional.of(anna));
        }

        @Test
        void traineeAddsATraining() {
            givenPeople();
            when(trainingDao.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Training created = service.create(newTraining(60));

            assertSame(john, created.getTrainee());
            assertSame(anna, created.getTrainer());
            assertSame(YOGA, created.getTrainingType());
            assertEquals("Evening yoga", created.getTrainingName());
            assertEquals(DATE, created.getTrainingDate());
            assertEquals(60, created.getTrainingDuration());
            assertTrue(john.getTrainers().contains(anna), "the trainer is added to the trainee's list");
            assertEquals(1, registry.get(GymMetrics.TRAININGS_ADDED).tag("type", "Yoga").counter().count());
            assertEquals(60, registry.get(GymMetrics.TRAINING_DURATION).tag("type", "Yoga").summary().totalAmount());
        }

        @Test
        void trainerAddsATraining() {
            givenPeople();
            when(trainingDao.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertSame(anna, service.create(newTraining(60)).getTrainer());
        }

        @Test
        void unknownTrainee() {
            when(traineeDao.findByUsername("John.Smith")).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> service.create(newTraining(60)));
            verifyNoInteractions(trainerDao);
            verify(trainingDao, never()).save(any());
        }

        @Test
        void unknownTrainer() {
            when(traineeDao.findByUsername("John.Smith")).thenReturn(Optional.of(trainee("John.Smith", "pw")));
            when(trainerDao.findByUsername("Anna.Lee")).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> service.create(newTraining(60)));
            verify(trainingDao, never()).save(any());
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1, -60})
        void rejectsNonPositiveDuration(int duration) {
            assertThrows(IllegalArgumentException.class, () -> service.create(newTraining(duration)));
            verifyNoInteractions(traineeDao, trainerDao, trainingDao);
        }

        @Test
        void rejectsMissingFields() {
            assertThrows(IllegalArgumentException.class, () -> service.create(newTraining(null)));
            assertThrows(IllegalArgumentException.class,
                    () -> service.create(new NewTraining(null, "Anna.Lee", "Yoga", DATE, 60)));
            assertThrows(IllegalArgumentException.class,
                    () -> service.create(new NewTraining("John.Smith", " ", "Yoga", DATE, 60)));
            assertThrows(IllegalArgumentException.class,
                    () -> service.create(new NewTraining("John.Smith", "Anna.Lee", "", DATE, 60)));
            assertThrows(IllegalArgumentException.class,
                    () -> service.create(new NewTraining("John.Smith", "Anna.Lee", "Yoga", null, 60)));
            assertThrows(NullPointerException.class, () -> service.create(null));
            verifyNoInteractions(traineeDao, trainerDao, trainingDao);
        }
    }

    @Nested
    class TraineeTrainings {

        @Test
        void passesTheCriteriaToTheDao() {
            var criteria = new TraineeTrainingCriteria(DATE, null, "Anna", "Yoga");
            List<Training> trainings = List.of(new Training());
            when(traineeDao.findByUsername("John.Smith")).thenReturn(Optional.of(trainee("John.Smith", "pw")));
            when(trainingDao.findTraineeTrainings("John.Smith", criteria)).thenReturn(trainings);

            assertSame(trainings, service.getTraineeTrainings("John.Smith", criteria));
        }

        @Test
        void nullCriteriaMeansNoFilter() {
            when(traineeDao.findByUsername("John.Smith")).thenReturn(Optional.of(trainee("John.Smith", "pw")));

            service.getTraineeTrainings("John.Smith", null);

            verify(trainingDao).findTraineeTrainings("John.Smith", TraineeTrainingCriteria.none());
        }

        @Test
        void unknownTrainee() {
            when(traineeDao.findByUsername("Nobody")).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> service.getTraineeTrainings("Nobody", null));
            verifyNoInteractions(trainingDao);
        }
    }

    @Nested
    class TrainerTrainings {

        @Test
        void passesTheCriteriaToTheDao() {
            var criteria = new TrainerTrainingCriteria(null, DATE, "Smith");
            when(trainerDao.findByUsername("Anna.Lee")).thenReturn(Optional.of(trainer("Anna.Lee", "pw", YOGA)));
            when(trainingDao.findTrainerTrainings("Anna.Lee", criteria)).thenReturn(List.of());

            assertTrue(service.getTrainerTrainings("Anna.Lee", criteria).isEmpty());
        }

        @Test
        void unknownTrainer() {
            when(trainerDao.findByUsername("Nobody")).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> service.getTrainerTrainings("Nobody", null));
            verifyNoInteractions(trainingDao);
        }
    }

    @Nested
    class TrainingTypes {

        @Test
        void listsAllTypes() {
            when(trainingTypeDao.findAll()).thenReturn(List.of(FITNESS, YOGA));

            assertEquals(List.of(FITNESS, YOGA), service.getTrainingTypes());
        }
    }
}
