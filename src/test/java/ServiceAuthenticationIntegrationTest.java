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
import io.gymcrm.security.Role;
import io.gymcrm.services.AuthenticationService;
import io.gymcrm.services.TraineeService;
import io.gymcrm.services.TrainerService;
import io.gymcrm.services.TrainingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static support.SecurityTestSupport.loginAs;
import static support.SecurityTestSupport.logout;

/**
 * Calls the services directly, not through HTTP or the facade, to show that the rules can't be skipped:
 * the @PreAuthorize rules reject anonymous callers, the wrong role and other users' profiles, and in
 * all those cases the database stays unchanged.
 */
@SpringBootTest(classes = Application.class)
class ServiceAuthenticationIntegrationTest {

    private static final String JOHN = "John.Smith";
    private static final String ANNA = "Anna.Lee";

    @Autowired
    private TraineeService traineeService;

    @Autowired
    private TrainerService trainerService;

    @Autowired
    private TrainingService trainingService;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;
    private String johnPassword;

    @BeforeEach
    void setUp() {
        logout();
        jdbc = new JdbcTemplate(dataSource);
        for (String table : new String[]{"training", "trainee_trainer", "trainee", "trainer", "users"}) {
            jdbc.update("delete from " + table);
        }
        johnPassword = traineeService.create(new TraineeRegistration("John", "Smith", null, "Tashkent")).password();
        trainerService.create(new TrainerRegistration("Anna", "Lee", 6L));
        traineeService.create(new TraineeRegistration("Maria", "Garcia", null, null));
        trainerService.create(new TrainerRegistration("David", "Brown", 1L));
        loginAs(JOHN, Role.TRAINEE);
        trainingService.create(new NewTraining(JOHN, ANNA, "Yoga 1", LocalDate.of(2026, 9, 1), 60));
        logout();
    }

    @AfterEach
    void tearDown() {
        logout();
    }

    /** A snapshot of everything that could be changed, to compare before and after. */
    private List<Map<String, Object>> database() {
        return List.of(
                Map.of("users", jdbc.queryForList("select * from users order by id")),
                Map.of("trainee", jdbc.queryForList("select * from trainee order by id")),
                Map.of("trainer", jdbc.queryForList("select * from trainer order by id")),
                Map.of("training", jdbc.queryForList("select * from training order by id")),
                Map.of("links", jdbc.queryForList("select * from trainee_trainer order by trainee_id, trainer_id")));
    }

    private void assertRejectedWithoutChanges(Class<? extends Throwable> expected, Executable operation) {
        List<Map<String, Object>> before = database();
        assertThrows(expected, operation);
        assertEquals(before, database(), "the database must not change");
    }

    private void assertAnonymousRejected(Executable operation) {
        assertRejectedWithoutChanges(AuthenticationCredentialsNotFoundException.class, operation);
    }

    private void assertForbidden(Executable operation) {
        assertRejectedWithoutChanges(AccessDeniedException.class, operation);
    }

    @Test
    void anonymousCallersAreRejectedByEveryTraineeOperation() {
        assertAnonymousRejected(() -> traineeService.getByUsername(JOHN));
        assertAnonymousRejected(() -> traineeService.update(JOHN, new TraineeUpdate("Hacked", "Smith", null, null, true)));
        assertAnonymousRejected(() -> traineeService.setActive(JOHN, false));
        assertAnonymousRejected(() -> traineeService.delete(JOHN));
        assertAnonymousRejected(() -> traineeService.updateTrainers(JOHN, List.of()));
    }

    @Test
    void anonymousCallersAreRejectedByEveryTrainerOperation() {
        assertAnonymousRejected(() -> trainerService.getByUsername(ANNA));
        assertAnonymousRejected(() -> trainerService.update(ANNA, new TrainerUpdate("Hacked", "Lee", true)));
        assertAnonymousRejected(() -> trainerService.setActive(ANNA, false));
        assertAnonymousRejected(() -> trainerService.getNotAssignedToTrainee(JOHN));
    }

    @Test
    void anonymousCallersAreRejectedByEveryTrainingOperation() {
        assertAnonymousRejected(() -> trainingService.create(new NewTraining(JOHN, ANNA, "Yoga 2", LocalDate.of(2026, 9, 2), 60)));
        assertAnonymousRejected(() -> trainingService.getTraineeTrainings(JOHN, TraineeTrainingCriteria.none()));
        assertAnonymousRejected(() -> trainingService.getTrainerTrainings(ANNA, TrainerTrainingCriteria.none()));
        assertAnonymousRejected(() -> trainingService.getTrainingTypes());
    }

    @Test
    void anonymousCallersCantChangePasswords() {
        assertAnonymousRejected(() -> authenticationService.changePassword(JOHN, johnPassword, "hacked"));
    }

    @Test
    void registrationNeedsNoLogin() {
        CreatedProfile<Trainee> trainee = traineeService.create(new TraineeRegistration("New", "Trainee", null, null));
        CreatedProfile<Trainer> trainer = trainerService.create(new TrainerRegistration("New", "Trainer", 1L));

        assertNotNull(trainee.token());
        assertNotNull(trainer.token());
    }

    @Test
    void someoneElsesProfileCantBeChanged() {
        loginAs("Maria.Garcia", Role.TRAINEE);
        assertForbidden(() -> traineeService.delete(JOHN));
        assertForbidden(() -> traineeService.update(JOHN, new TraineeUpdate("Hacked", "Smith", null, null, true)));
        assertForbidden(() -> traineeService.setActive(JOHN, false));
        assertForbidden(() -> traineeService.updateTrainers(JOHN, List.of()));
        assertForbidden(() -> authenticationService.changePassword(JOHN, johnPassword, "hacked"));

        loginAs("David.Brown", Role.TRAINER);
        assertForbidden(() -> trainerService.update(ANNA, new TrainerUpdate("Hacked", "Lee", true)));
        assertForbidden(() -> trainerService.setActive(ANNA, false));
    }

    @Test
    void roleSpecificOperationsRejectTheOtherRole() {
        // A token for the same username but the wrong role: a trainer can't use trainee operations and vice versa.
        loginAs(ANNA, Role.TRAINER);
        assertForbidden(() -> traineeService.setActive(ANNA, false));
        assertForbidden(() -> traineeService.delete(ANNA));

        loginAs(JOHN, Role.TRAINEE);
        assertForbidden(() -> trainerService.setActive(JOHN, false));
        assertForbidden(() -> trainerService.update(JOHN, new TrainerUpdate("Hacked", "Lee", true)));
    }

    @Test
    void anyLoggedInUserCanRead() {
        loginAs("Maria.Garcia", Role.TRAINEE);

        assertEquals(JOHN, traineeService.getByUsername(JOHN).getUser().getUsername());
        assertEquals(ANNA, trainerService.getByUsername(ANNA).getUser().getUsername());
        assertEquals(6, trainingService.getTrainingTypes().size());
    }

    @Test
    void onlyItsTraineeOrTrainerCanAddATraining() {
        NewTraining training = new NewTraining(JOHN, ANNA, "Yoga 2", LocalDate.of(2026, 9, 2), 60);

        loginAs("David.Brown", Role.TRAINER);
        assertForbidden(() -> trainingService.create(training));

        loginAs(ANNA, Role.TRAINER);
        assertEquals("Yoga 2", trainingService.create(training).getTrainingName());
    }

    @Test
    void nullTrainingIsForbiddenNotACrashInTheRule() {
        loginAs(JOHN, Role.TRAINEE);

        assertForbidden(() -> trainingService.create(null));
    }

    @Test
    void deactivatedUsersCanStillUseTheirAccount() {
        // Needed so that they can activate themselves again.
        loginAs(JOHN, Role.TRAINEE);
        traineeService.setActive(JOHN, false);
        traineeService.setActive(JOHN, true);

        assertTrue(traineeService.getByUsername(JOHN).getUser().isActive());
    }
}
