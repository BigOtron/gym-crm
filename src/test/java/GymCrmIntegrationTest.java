import io.gymcrm.Application;
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
import io.gymcrm.exceptions.ConflictException;
import io.gymcrm.exceptions.ForbiddenException;
import io.gymcrm.exceptions.NotFoundException;
import io.gymcrm.facade.GymFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = Application.class)
class GymCrmIntegrationTest {

    private static final long YOGA = 6L;
    private static final long HEAVY_LIFTING = 3L;

    @Autowired
    private GymFacade facade;

    @Autowired
    private DataSource dataSource;

    private Credentials john;
    private Credentials anna;
    private Credentials david;

    @BeforeEach
    void setUp() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("delete from training");
        jdbc.update("delete from trainee_trainer");
        jdbc.update("delete from trainee");
        jdbc.update("delete from trainer");
        jdbc.update("delete from users");

        john = credentials(facade.createTrainee(
                new TraineeRegistration("John", "Smith", LocalDate.of(1995, 4, 12), "Tashkent")));
        anna = credentials(facade.createTrainer(new TrainerRegistration("Anna", "Lee", YOGA)));
        david = credentials(facade.createTrainer(new TrainerRegistration("David", "Brown", HEAVY_LIFTING)));
    }

    private static Credentials credentials(Trainee trainee) {
        return new Credentials(trainee.getUser().getUsername(), trainee.getUser().getPassword());
    }

    private static Credentials credentials(Trainer trainer) {
        return new Credentials(trainer.getUser().getUsername(), trainer.getUser().getPassword());
    }

    private Training addTraining(Credentials trainee, Credentials trainer, String name, LocalDate date) {
        return facade.addTraining(trainer, new NewTraining(trainee.username(), trainer.username(), name, date, 60));
    }

    @Test
    void createProfilesGeneratesUsernamesAndPasswords() {
        assertEquals("John.Smith", john.username());
        assertEquals(10, john.password().length());

        Trainee namesake = facade.createTrainee(new TraineeRegistration("John", "Smith", null, null));
        Trainer trainerNamesake = facade.createTrainer(new TrainerRegistration("John", "Smith", YOGA));

        assertEquals("John.Smith1", namesake.getUser().getUsername());
        assertEquals("John.Smith2", trainerNamesake.getUser().getUsername());
        assertTrue(namesake.getUser().isActive());
    }

    @Test
    void createRejectsMissingRequiredFields() {
        assertThrows(IllegalArgumentException.class,
                () -> facade.createTrainee(new TraineeRegistration(" ", "Smith", null, null)));
        assertThrows(IllegalArgumentException.class,
                () -> facade.createTrainer(new TrainerRegistration("Anna", "Lee", null)));
        assertThrows(NotFoundException.class,
                () -> facade.createTrainer(new TrainerRegistration("Anna", "Lee", 99L)));
    }

    @Test
    void credentialsMatching() {
        assertTrue(facade.traineeCredentialsMatch(john));
        assertTrue(facade.trainerCredentialsMatch(anna));
        assertFalse(facade.traineeCredentialsMatch(new Credentials(john.username(), "wrong")));
        assertFalse(facade.traineeCredentialsMatch(anna));
        assertFalse(facade.trainerCredentialsMatch(john));
    }

    @Test
    void everyOperationExceptCreateRequiresAuthentication() {
        Credentials bad = new Credentials(john.username(), "wrong");

        assertThrows(AuthenticationException.class, () -> facade.getTraineeByUsername(bad, john.username()));
        assertThrows(AuthenticationException.class, () -> facade.login(bad));
        assertThrows(AuthenticationException.class, () -> facade.changePassword(bad, "x"));
        assertThrows(AuthenticationException.class, () -> facade.setTraineeActive(bad, john.username(), false));
        assertThrows(AuthenticationException.class, () -> facade.deleteTrainee(bad, john.username()));
        assertThrows(AuthenticationException.class, () -> facade.getTrainingTypes(bad));
        // A trainer can't use trainee operations and vice versa, even on their own username.
        assertThrows(AuthenticationException.class, () -> facade.updateTraineeTrainers(anna, anna.username(), List.of()));
        assertThrows(AuthenticationException.class, () -> facade.setTrainerActive(john, john.username(), false));
    }

    @Test
    void changingSomeoneElsesProfileIsForbidden() {
        Credentials maria = credentials(facade.createTrainee(new TraineeRegistration("Maria", "Garcia", null, null)));

        assertThrows(ForbiddenException.class, () -> facade.deleteTrainee(maria, john.username()));
        assertThrows(ForbiddenException.class, () -> facade.setTrainerActive(david, anna.username(), false));
        assertNotNull(facade.getTraineeByUsername(maria, john.username()));
    }

    @Test
    void loginAndTrainingTypes() {
        facade.login(john);
        facade.login(anna);

        List<String> types = facade.getTrainingTypes(john).stream().map(TrainingType::getTrainingTypeName).toList();
        assertEquals(List.of("Fitness", "Running", "Heavy lifting", "Pull ups", "Push ups", "Yoga"), types);
    }

    @Test
    void selectProfilesByUsername() {
        Trainee trainee = facade.getTraineeByUsername(anna, john.username());
        Trainer trainer = facade.getTrainerByUsername(john, anna.username());

        assertEquals("Tashkent", trainee.getAddress());
        assertEquals("Yoga", trainer.getSpecialization().getTrainingTypeName());
        assertThrows(NotFoundException.class, () -> facade.getTraineeByUsername(john, "Nobody.Here"));
    }

    @Test
    void changePasswords() {
        facade.changePassword(john, "newTraineePass");
        facade.changePassword(anna, "newTrainerPass");

        assertFalse(facade.traineeCredentialsMatch(john));
        assertTrue(facade.traineeCredentialsMatch(new Credentials(john.username(), "newTraineePass")));
        assertTrue(facade.trainerCredentialsMatch(new Credentials(anna.username(), "newTrainerPass")));
    }

    @Test
    void updateProfiles() {
        facade.updateTrainee(john, john.username(),
                new TraineeUpdate("Johnny", "Smith", LocalDate.of(1995, 4, 13), "Samarkand", true));
        facade.updateTrainer(anna, anna.username(), new TrainerUpdate("Ann", "Lee", false));

        Trainee trainee = facade.getTraineeByUsername(john, john.username());
        Trainer trainer = facade.getTrainerByUsername(john, anna.username());
        assertEquals("Johnny", trainee.getUser().getFirstName());
        assertEquals("Samarkand", trainee.getAddress());
        assertEquals("John.Smith", trainee.getUser().getUsername());
        assertEquals("Ann", trainer.getUser().getFirstName());
        assertFalse(trainer.getUser().isActive());
        assertEquals("Yoga", trainer.getSpecialization().getTrainingTypeName());
        assertThrows(IllegalArgumentException.class,
                () -> facade.updateTrainee(john, john.username(), new TraineeUpdate(null, "Smith", null, null, true)));
    }

    @Test
    void setActiveIsNotIdempotent() {
        facade.setTraineeActive(john, john.username(), false);
        assertFalse(facade.getTraineeByUsername(john, john.username()).getUser().isActive());

        assertThrows(ConflictException.class, () -> facade.setTraineeActive(john, john.username(), false));

        facade.setTraineeActive(john, john.username(), true);
        facade.setTrainerActive(anna, anna.username(), false);
        assertFalse(facade.getTrainerByUsername(john, anna.username()).getUser().isActive());
    }

    @Test
    void deleteTraineeCascadesToTrainings() {
        addTraining(john, anna, "Yoga 1", LocalDate.of(2026, 9, 1));

        facade.deleteTrainee(john, john.username());

        assertThrows(NotFoundException.class, () -> facade.getTraineeByUsername(anna, john.username()));
        assertTrue(facade.getTrainerTrainings(anna, anna.username(), TrainerTrainingCriteria.none()).isEmpty());
        assertNotNull(facade.getTrainerByUsername(anna, anna.username()));
    }

    @Test
    void addTrainingTakesTypeFromTrainerAndAssignsTrainer() {
        Training training = addTraining(john, anna, "Yoga 1", LocalDate.of(2026, 9, 1));

        assertNotNull(training.getId());
        assertEquals("Yoga", training.getTrainingType().getTrainingTypeName());
        assertEquals(1, facade.getTraineeByUsername(john, john.username()).getTrainers().size());
    }

    @Test
    void addTrainingValidatesInput() {
        assertThrows(IllegalArgumentException.class, () -> facade.addTraining(anna,
                new NewTraining(john.username(), anna.username(), "Yoga", LocalDate.now(), 0)));
        assertThrows(NotFoundException.class, () -> facade.addTraining(anna,
                new NewTraining("Nobody.Here", anna.username(), "Yoga", LocalDate.now(), 30)));
        assertThrows(ForbiddenException.class, () -> facade.addTraining(david,
                new NewTraining(john.username(), anna.username(), "Yoga", LocalDate.now(), 30)));
    }

    @Test
    void traineeTrainingsByCriteria() {
        addTraining(john, anna, "Yoga 1", LocalDate.of(2026, 9, 1));
        addTraining(john, anna, "Yoga 2", LocalDate.of(2026, 9, 15));
        addTraining(john, david, "Lifting", LocalDate.of(2026, 9, 20));

        assertEquals(3, trainingsOfJohn(new TraineeTrainingCriteria(null, null, null, null)));
        assertEquals(2, trainingsOfJohn(new TraineeTrainingCriteria(LocalDate.of(2026, 9, 10), null, null, null)));
        assertEquals(1, trainingsOfJohn(new TraineeTrainingCriteria(null, LocalDate.of(2026, 9, 1), null, null)));
        assertEquals(2, trainingsOfJohn(new TraineeTrainingCriteria(null, null, "anna", null)));
        assertEquals(2, trainingsOfJohn(new TraineeTrainingCriteria(null, null, "Anna Lee", null)));
        assertEquals(1, trainingsOfJohn(new TraineeTrainingCriteria(null, null, null, "heavy lifting")));
        assertEquals(1, trainingsOfJohn(new TraineeTrainingCriteria(
                LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 16), "Lee", "Yoga")));

        // Fetched associations are usable after the transaction has ended.
        Training first = facade.getTraineeTrainings(john, john.username(), TraineeTrainingCriteria.none()).get(0);
        assertEquals("Yoga 1", first.getTrainingName());
        assertEquals("Anna", first.getTrainer().getUser().getFirstName());
    }

    private int trainingsOfJohn(TraineeTrainingCriteria criteria) {
        return facade.getTraineeTrainings(john, john.username(), criteria).size();
    }

    @Test
    void trainerTrainingsByCriteria() {
        Credentials maria = credentials(facade.createTrainee(
                new TraineeRegistration("Maria", "Garcia", null, null)));
        addTraining(john, anna, "Yoga 1", LocalDate.of(2026, 9, 1));
        addTraining(maria, anna, "Yoga 2", LocalDate.of(2026, 9, 15));

        assertEquals(2, facade.getTrainerTrainings(anna, anna.username(), TrainerTrainingCriteria.none()).size());
        assertEquals(1, facade.getTrainerTrainings(anna, anna.username(),
                new TrainerTrainingCriteria(null, null, "garcia")).size());
        assertEquals(1, facade.getTrainerTrainings(anna, anna.username(),
                new TrainerTrainingCriteria(LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 30), null)).size());
    }

    @Test
    void trainersNotAssignedToTrainee() {
        Credentials inactive = credentials(facade.createTrainer(new TrainerRegistration("Old", "Coach", YOGA)));
        facade.setTrainerActive(inactive, inactive.username(), false);
        addTraining(john, anna, "Yoga 1", LocalDate.of(2026, 9, 1));

        List<Trainer> free = facade.getTrainersNotAssignedToTrainee(john, john.username());

        assertEquals(List.of(david.username()), free.stream().map(t -> t.getUser().getUsername()).toList());
    }

    @Test
    void updateTraineeTrainersReplacesTheList() {
        addTraining(john, anna, "Yoga 1", LocalDate.of(2026, 9, 1));

        List<Trainer> trainers = facade.updateTraineeTrainers(john, john.username(), List.of(david.username()));

        assertEquals(1, trainers.size());
        Trainee trainee = facade.getTraineeByUsername(john, john.username());
        assertEquals(List.of(david.username()),
                trainee.getTrainers().stream().map(t -> t.getUser().getUsername()).toList());
        assertThrows(NotFoundException.class,
                () -> facade.updateTraineeTrainers(john, john.username(), List.of("Nobody.Here")));
        // Removing a trainer from the list doesn't remove past trainings.
        assertEquals(1, facade.getTraineeTrainings(john, john.username(), TraineeTrainingCriteria.none()).size());
    }
}
