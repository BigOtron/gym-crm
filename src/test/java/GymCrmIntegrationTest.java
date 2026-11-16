import io.gymcrm.Application;
import io.gymcrm.dto.CreatedProfile;
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
import io.gymcrm.exceptions.ConflictException;
import io.gymcrm.exceptions.NotFoundException;
import io.gymcrm.facade.GymFacade;
import io.gymcrm.security.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static support.SecurityTestSupport.loginAs;
import static support.SecurityTestSupport.logout;

/**
 * Runs the whole application against an in-memory H2 database. There is no test-wide transaction,
 * so every facade call commits on its own, just like in the real application.
 * Each call runs as the user set with loginAs; ServiceAuthenticationIntegrationTest covers the rules.
 */
@SpringBootTest(classes = Application.class)
class GymCrmIntegrationTest {

    private static final long YOGA = 6L;
    private static final long HEAVY_LIFTING = 3L;
    private static final String JOHN = "John.Smith";
    private static final String ANNA = "Anna.Lee";
    private static final String DAVID = "David.Brown";

    @Autowired
    private GymFacade facade;

    @Autowired
    private DataSource dataSource;

    private String johnPassword;

    @BeforeEach
    void setUp() {
        JdbcTemplate jdbc = jdbc();
        for (String table : new String[]{"training", "trainee_trainer", "trainee", "trainer", "users"}) {
            jdbc.update("delete from " + table);
        }
        CreatedProfile<Trainee> john = facade.createTrainee(
                new TraineeRegistration("John", "Smith", LocalDate.of(1995, 4, 12), "Tashkent"));
        johnPassword = john.password();
        facade.createTrainer(new TrainerRegistration("Anna", "Lee", YOGA));
        facade.createTrainer(new TrainerRegistration("David", "Brown", HEAVY_LIFTING));
        loginAs(JOHN, Role.TRAINEE);
    }

    @AfterEach
    void tearDown() {
        logout();
    }

    private JdbcTemplate jdbc() {
        return new JdbcTemplate(dataSource);
    }

    private Training addTraining(String trainer, String name, LocalDate date) {
        return facade.addTraining(new NewTraining(JOHN, trainer, name, date, 60));
    }

    @Test
    void createProfilesGeneratesUsernamesPasswordsAndTokens() {
        assertEquals(10, johnPassword.length());

        CreatedProfile<Trainee> namesake = facade.createTrainee(new TraineeRegistration("John", "Smith", null, null));
        CreatedProfile<Trainer> trainerNamesake = facade.createTrainer(new TrainerRegistration("John", "Smith", YOGA));

        assertEquals("John.Smith1", namesake.profile().getUser().getUsername());
        assertEquals("John.Smith2", trainerNamesake.profile().getUser().getUsername());
        assertTrue(namesake.profile().getUser().isActive());
        assertNotNull(namesake.token().token());
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
    void trainingTypes() {
        List<String> types = facade.getTrainingTypes().stream().map(TrainingType::getTrainingTypeName).toList();

        assertEquals(List.of("Fitness", "Running", "Heavy lifting", "Pull ups", "Push ups", "Yoga"), types);
    }

    @Test
    void selectProfilesByUsername() {
        Trainee trainee = facade.getTraineeByUsername(JOHN);
        Trainer trainer = facade.getTrainerByUsername(ANNA);

        assertEquals("Tashkent", trainee.getAddress());
        assertEquals("Yoga", trainer.getSpecialization().getTrainingTypeName());
        assertThrows(NotFoundException.class, () -> facade.getTraineeByUsername("Nobody.Here"));
    }

    @Test
    void updateProfiles() {
        facade.updateTrainee(JOHN, new TraineeUpdate("Johnny", "Smith", LocalDate.of(1995, 4, 13), "Samarkand", true));
        loginAs(ANNA, Role.TRAINER);
        facade.updateTrainer(ANNA, new TrainerUpdate("Ann", "Lee", false));

        Trainee trainee = facade.getTraineeByUsername(JOHN);
        Trainer trainer = facade.getTrainerByUsername(ANNA);
        assertEquals("Johnny", trainee.getUser().getFirstName());
        assertEquals("Samarkand", trainee.getAddress());
        assertEquals(JOHN, trainee.getUser().getUsername());
        assertEquals("Ann", trainer.getUser().getFirstName());
        assertFalse(trainer.getUser().isActive());
        assertEquals("Yoga", trainer.getSpecialization().getTrainingTypeName());

        loginAs(JOHN, Role.TRAINEE);
        assertThrows(IllegalArgumentException.class,
                () -> facade.updateTrainee(JOHN, new TraineeUpdate(null, "Smith", null, null, true)));
    }

    @Test
    void setActiveIsNotIdempotent() {
        facade.setTraineeActive(JOHN, false);
        assertFalse(facade.getTraineeByUsername(JOHN).getUser().isActive());

        assertThrows(ConflictException.class, () -> facade.setTraineeActive(JOHN, false));

        facade.setTraineeActive(JOHN, true);
        loginAs(ANNA, Role.TRAINER);
        facade.setTrainerActive(ANNA, false);
        assertFalse(facade.getTrainerByUsername(ANNA).getUser().isActive());
    }

    @Test
    void deleteTraineeCascadesToTrainings() {
        addTraining(ANNA, "Yoga 1", LocalDate.of(2026, 9, 1));

        facade.deleteTrainee(JOHN);

        assertThrows(NotFoundException.class, () -> facade.getTraineeByUsername(JOHN));
        assertTrue(facade.getTrainerTrainings(ANNA, TrainerTrainingCriteria.none()).isEmpty());
        assertNotNull(facade.getTrainerByUsername(ANNA));
    }

    @Test
    void addTrainingTakesTypeFromTrainerAndAssignsTrainer() {
        Training training = addTraining(ANNA, "Yoga 1", LocalDate.of(2026, 9, 1));

        assertNotNull(training.getId());
        assertEquals("Yoga", training.getTrainingType().getTrainingTypeName());
        assertEquals(1, facade.getTraineeByUsername(JOHN).getTrainers().size());
    }

    @Test
    void addTrainingValidatesInput() {
        assertThrows(IllegalArgumentException.class,
                () -> facade.addTraining(new NewTraining(JOHN, ANNA, "Yoga", LocalDate.now(), 0)));
        assertThrows(NotFoundException.class,
                () -> facade.addTraining(new NewTraining(JOHN, "Nobody.Here", "Yoga", LocalDate.now(), 30)));
    }

    @Test
    void traineeTrainingsByCriteria() {
        addTraining(ANNA, "Yoga 1", LocalDate.of(2026, 9, 1));
        addTraining(ANNA, "Yoga 2", LocalDate.of(2026, 9, 15));
        addTraining(DAVID, "Lifting", LocalDate.of(2026, 9, 20));

        assertEquals(3, trainingsOfJohn(TraineeTrainingCriteria.none()));
        assertEquals(2, trainingsOfJohn(new TraineeTrainingCriteria(LocalDate.of(2026, 9, 10), null, null, null)));
        assertEquals(1, trainingsOfJohn(new TraineeTrainingCriteria(null, LocalDate.of(2026, 9, 1), null, null)));
        assertEquals(2, trainingsOfJohn(new TraineeTrainingCriteria(null, null, "anna", null)));
        assertEquals(2, trainingsOfJohn(new TraineeTrainingCriteria(null, null, "Anna Lee", null)));
        assertEquals(1, trainingsOfJohn(new TraineeTrainingCriteria(null, null, null, "heavy lifting")));
        assertEquals(1, trainingsOfJohn(new TraineeTrainingCriteria(
                LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 16), "Lee", "Yoga")));

        // Fetched associations are usable after the transaction has ended.
        Training first = facade.getTraineeTrainings(JOHN, TraineeTrainingCriteria.none()).get(0);
        assertEquals("Yoga 1", first.getTrainingName());
        assertEquals("Anna", first.getTrainer().getUser().getFirstName());
    }

    private int trainingsOfJohn(TraineeTrainingCriteria criteria) {
        return facade.getTraineeTrainings(JOHN, criteria).size();
    }

    @Test
    void trainerTrainingsByCriteria() {
        facade.createTrainee(new TraineeRegistration("Maria", "Garcia", null, null));
        addTraining(ANNA, "Yoga 1", LocalDate.of(2026, 9, 1));
        loginAs("Maria.Garcia", Role.TRAINEE);
        facade.addTraining(new NewTraining("Maria.Garcia", ANNA, "Yoga 2", LocalDate.of(2026, 9, 15), 60));

        assertEquals(2, facade.getTrainerTrainings(ANNA, TrainerTrainingCriteria.none()).size());
        assertEquals(1, facade.getTrainerTrainings(ANNA, new TrainerTrainingCriteria(null, null, "garcia")).size());
        assertEquals(1, facade.getTrainerTrainings(ANNA,
                new TrainerTrainingCriteria(LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 30), null)).size());
    }

    @Test
    void trainersNotAssignedToTrainee() {
        facade.createTrainer(new TrainerRegistration("Old", "Coach", YOGA));
        loginAs("Old.Coach", Role.TRAINER);
        facade.setTrainerActive("Old.Coach", false);
        loginAs(JOHN, Role.TRAINEE);
        addTraining(ANNA, "Yoga 1", LocalDate.of(2026, 9, 1));

        List<Trainer> free = facade.getTrainersNotAssignedToTrainee(JOHN);

        assertEquals(List.of(DAVID), free.stream().map(t -> t.getUser().getUsername()).toList());
    }

    @Test
    void updateTraineeTrainersReplacesTheList() {
        addTraining(ANNA, "Yoga 1", LocalDate.of(2026, 9, 1));

        List<Trainer> trainers = facade.updateTraineeTrainers(JOHN, List.of(DAVID));

        assertEquals(1, trainers.size());
        Trainee trainee = facade.getTraineeByUsername(JOHN);
        assertEquals(List.of(DAVID), trainee.getTrainers().stream().map(t -> t.getUser().getUsername()).toList());
        assertThrows(NotFoundException.class, () -> facade.updateTraineeTrainers(JOHN, List.of("Nobody.Here")));
        // Removing a trainer from the list doesn't remove past trainings.
        assertEquals(1, facade.getTraineeTrainings(JOHN, TraineeTrainingCriteria.none()).size());
    }

    @Test
    void passwordsAreStoredAsSaltedHashes() {
        String stored = jdbc().queryForObject("select password from users where username = ?", String.class, JOHN);

        assertTrue(stored.startsWith("$2a$"), "a BCrypt hash: " + stored);
        assertFalse(stored.contains(johnPassword));
    }

    @Test
    void equalPasswordsGetDifferentHashesBecauseOfTheSalt() {
        facade.changePassword(JOHN, johnPassword, "samePassword1");
        CreatedProfile<Trainee> maria = facade.createTrainee(new TraineeRegistration("Maria", "Garcia", null, null));
        loginAs("Maria.Garcia", Role.TRAINEE);
        facade.changePassword("Maria.Garcia", maria.password(), "samePassword1");

        List<String> hashes = jdbc().queryForList("select password from users where username in (?, ?)",
                String.class, JOHN, "Maria.Garcia");

        assertEquals(2, hashes.size());
        assertNotEquals(hashes.get(0), hashes.get(1));
    }
}
