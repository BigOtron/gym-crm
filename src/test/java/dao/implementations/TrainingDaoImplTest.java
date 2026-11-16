package dao.implementations;

import io.gymcrm.dao.TrainingDao;
import io.gymcrm.dto.TraineeTrainingCriteria;
import io.gymcrm.dto.TrainerTrainingCriteria;
import io.gymcrm.entities.Trainee;
import io.gymcrm.entities.Trainer;
import io.gymcrm.entities.Training;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import support.DaoTestBase;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrainingDaoImplTest extends DaoTestBase {

    private static final LocalDate SEP_1 = LocalDate.of(2026, 9, 1);
    private static final LocalDate SEP_15 = LocalDate.of(2026, 9, 15);
    private static final LocalDate SEP_20 = LocalDate.of(2026, 9, 20);

    @Autowired
    private TrainingDao trainingDao;

    private Trainee john;
    private Trainee maria;
    private Trainer anna;
    private Trainer david;

    @BeforeEach
    void createPeople() {
        john = persistTrainee("John", "Smith", "John.Smith");
        maria = persistTrainee("Maria", "Garcia", "Maria.Garcia");
        anna = persistTrainer("Anna", "Lee", "Anna.Lee", "Yoga");
        david = persistTrainer("David", "Brown", "David.Brown", "Heavy lifting");
    }

    private static List<String> names(List<Training> trainings) {
        return trainings.stream().map(Training::getTrainingName).toList();
    }

    private List<String> johnsTrainings(TraineeTrainingCriteria criteria) {
        return names(trainingDao.findTraineeTrainings("John.Smith", criteria));
    }

    private List<String> annasTrainings(TrainerTrainingCriteria criteria) {
        return names(trainingDao.findTrainerTrainings("Anna.Lee", criteria));
    }

    private void givenTrainings() {
        persistTraining(john, anna, "Yoga 2", SEP_15);
        persistTraining(john, anna, "Yoga 1", SEP_1);
        persistTraining(john, david, "Lifting", SEP_20);
        persistTraining(maria, anna, "Maria yoga", SEP_15);
        flushAndClear();
    }

    @Test
    void saveAssignsId() {
        Training training = persistTraining(john, anna, "Yoga 1", SEP_1);

        trainingDao.save(training);
        entityManager.flush();

        assertNotNull(training.getId());
    }

    @Test
    void traineeTrainingsWithoutCriteriaAreSortedByDate() {
        givenTrainings();

        assertEquals(List.of("Yoga 1", "Yoga 2", "Lifting"), johnsTrainings(TraineeTrainingCriteria.none()));
    }

    @Test
    void traineeTrainingsByDateRangeIncludeBothEnds() {
        givenTrainings();

        assertEquals(List.of("Yoga 2", "Lifting"), johnsTrainings(new TraineeTrainingCriteria(SEP_15, null, null, null)));
        assertEquals(List.of("Yoga 1", "Yoga 2"), johnsTrainings(new TraineeTrainingCriteria(null, SEP_15, null, null)));
        assertEquals(List.of("Yoga 2"), johnsTrainings(new TraineeTrainingCriteria(SEP_15, SEP_15, null, null)));
    }

    @Test
    void traineeTrainingsByTrainerName() {
        givenTrainings();

        assertEquals(List.of("Yoga 1", "Yoga 2"), johnsTrainings(new TraineeTrainingCriteria(null, null, "anna", null)));
        assertEquals(List.of("Yoga 1", "Yoga 2"), johnsTrainings(new TraineeTrainingCriteria(null, null, "LEE", null)));
        assertEquals(List.of("Lifting"), johnsTrainings(new TraineeTrainingCriteria(null, null, " David Brown ", null)));
        assertTrue(johnsTrainings(new TraineeTrainingCriteria(null, null, "Nobody", null)).isEmpty());
    }

    @Test
    void blankTrainerNameIsIgnored() {
        givenTrainings();

        assertEquals(3, johnsTrainings(new TraineeTrainingCriteria(null, null, "  ", null)).size());
    }

    @Test
    void traineeTrainingsByTrainingType() {
        givenTrainings();

        assertEquals(List.of("Lifting"), johnsTrainings(new TraineeTrainingCriteria(null, null, null, "heavy lifting")));
        assertTrue(johnsTrainings(new TraineeTrainingCriteria(null, null, null, "Running")).isEmpty());
    }

    @Test
    void traineeTrainingsWithAllCriteria() {
        givenTrainings();

        assertEquals(List.of("Yoga 2"), johnsTrainings(new TraineeTrainingCriteria(SEP_15, SEP_20, "Anna", "Yoga")));
    }

    @Test
    void trainerTrainingsWithoutCriteria() {
        givenTrainings();

        List<String> trainings = annasTrainings(TrainerTrainingCriteria.none());

        // "Yoga 2" and "Maria yoga" are on the same day, so only the first position is fixed.
        assertEquals("Yoga 1", trainings.get(0));
        assertEquals(Set.of("Yoga 1", "Yoga 2", "Maria yoga"), Set.copyOf(trainings));
    }

    @Test
    void trainerTrainingsByTraineeNameAndDates() {
        givenTrainings();

        assertEquals(List.of("Maria yoga"), annasTrainings(new TrainerTrainingCriteria(null, null, "garcia")));
        assertEquals(List.of("Yoga 1"), annasTrainings(new TrainerTrainingCriteria(null, SEP_1, null)));
        assertEquals(List.of("Yoga 2"), annasTrainings(new TrainerTrainingCriteria(SEP_15, null, "John")));
    }

    @Test
    void trainingsOfUnknownUserAreEmpty() {
        givenTrainings();

        assertTrue(trainingDao.findTraineeTrainings("Nobody", TraineeTrainingCriteria.none()).isEmpty());
        assertTrue(trainingDao.findTrainerTrainings("Nobody", TrainerTrainingCriteria.none()).isEmpty());
    }

    @Test
    void associationsAreFetchedWithTheTrainings() {
        givenTrainings();

        Training training = trainingDao.findTraineeTrainings("John.Smith", TraineeTrainingCriteria.none()).get(0);

        // The test transaction would hide lazy loading, so check that nothing is left to load.
        assertTrue(Hibernate.isInitialized(training.getTrainee()));
        assertTrue(Hibernate.isInitialized(training.getTrainer()));
        assertTrue(Hibernate.isInitialized(training.getTrainer().getUser()));
        assertTrue(Hibernate.isInitialized(training.getTrainee().getUser()));
        assertEquals("Anna", training.getTrainer().getUser().getFirstName());
        assertEquals("Yoga", training.getTrainingType().getTrainingTypeName());
    }
}
