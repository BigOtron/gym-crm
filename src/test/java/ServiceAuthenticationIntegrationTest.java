import io.gymcrm.config.AppConfig;
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
import io.gymcrm.exceptions.AuthenticationException;
import io.gymcrm.services.AuthenticationService;
import io.gymcrm.services.TraineeService;
import io.gymcrm.services.TrainerService;
import io.gymcrm.services.TrainingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringJUnitConfig(AppConfig.class)
class ServiceAuthenticationIntegrationTest {

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
    private Credentials john;
    private Credentials anna;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        for (String table : new String[]{"training", "trainee_trainer", "trainee", "trainer", "users"}) {
            jdbc.update("delete from " + table);
        }
        Trainee trainee = traineeService.create(new TraineeRegistration("John", "Smith", null, "Tashkent"));
        Trainer trainer = trainerService.create(new TrainerRegistration("Anna", "Lee", "Yoga"));
        john = new Credentials(trainee.getUser().getUsername(), trainee.getUser().getPassword());
        anna = new Credentials(trainer.getUser().getUsername(), trainer.getUser().getPassword());
        trainingService.create(john, new NewTraining(john.username(), anna.username(), "Yoga 1",
                LocalDate.of(2026, 9, 1), 60));
    }

    private List<Map<String, Object>> database() {
        return List.of(
                Map.of("users", jdbc.queryForList("select * from users order by id")),
                Map.of("trainee", jdbc.queryForList("select * from trainee order by id")),
                Map.of("trainer", jdbc.queryForList("select * from trainer order by id")),
                Map.of("training", jdbc.queryForList("select * from training order by id")),
                Map.of("links", jdbc.queryForList("select * from trainee_trainer order by trainee_id, trainer_id")));
    }

    private void assertRejectedWithoutChanges(Executable operation) {
        List<Map<String, Object>> before = database();
        assertThrows(AuthenticationException.class, operation);
        assertEquals(before, database(), "the database must not change");
    }

    @Test
    void wrongPasswordIsRejectedByEveryTraineeOperation() {
        Credentials wrong = new Credentials(john.username(), "wrong");

        assertRejectedWithoutChanges(() -> traineeService.getByUsername(wrong, john.username()));
        assertRejectedWithoutChanges(() -> traineeService.changePassword(wrong, "hacked"));
        assertRejectedWithoutChanges(() -> traineeService.update(wrong, new TraineeUpdate("Hacked", "Smith", null, null)));
        assertRejectedWithoutChanges(() -> traineeService.toggleActive(wrong));
        assertRejectedWithoutChanges(() -> traineeService.delete(wrong));
        assertRejectedWithoutChanges(() -> traineeService.updateTrainers(wrong, List.of()));
    }

    @Test
    void wrongPasswordIsRejectedByEveryTrainerOperation() {
        Credentials wrong = new Credentials(anna.username(), "wrong");

        assertRejectedWithoutChanges(() -> trainerService.getByUsername(wrong, anna.username()));
        assertRejectedWithoutChanges(() -> trainerService.changePassword(wrong, "hacked"));
        assertRejectedWithoutChanges(() -> trainerService.update(wrong, new TrainerUpdate("Hacked", "Lee", "Fitness")));
        assertRejectedWithoutChanges(() -> trainerService.toggleActive(wrong));
        assertRejectedWithoutChanges(() -> trainerService.getNotAssignedToTrainee(wrong, john.username()));
    }

    @Test
    void wrongPasswordIsRejectedByEveryTrainingOperation() {
        Credentials wrong = new Credentials(john.username(), "wrong");

        assertRejectedWithoutChanges(() -> trainingService.create(wrong, new NewTraining(john.username(),
                anna.username(), "Yoga 2", LocalDate.of(2026, 9, 2), 60)));
        assertRejectedWithoutChanges(() -> trainingService.getTraineeTrainings(wrong, john.username(),
                TraineeTrainingCriteria.none()));
        assertRejectedWithoutChanges(() -> trainingService.getTrainerTrainings(wrong, anna.username(),
                TrainerTrainingCriteria.none()));
    }

    @Test
    void missingOrIncompleteCredentialsAreRejected() {
        assertRejectedWithoutChanges(() -> traineeService.delete(null));
        assertRejectedWithoutChanges(() -> traineeService.delete(new Credentials(john.username(), null)));
        assertRejectedWithoutChanges(() -> traineeService.delete(new Credentials(null, john.password())));
        assertRejectedWithoutChanges(() -> trainerService.getByUsername(null, anna.username()));
    }

    @Test
    void unknownUserIsRejected() {
        assertRejectedWithoutChanges(() -> traineeService.getByUsername(new Credentials("Nobody", "pw"), john.username()));
    }

    @Test
    void roleSpecificOperationsRejectTheOtherRole() {
        assertRejectedWithoutChanges(() -> traineeService.toggleActive(anna));
        assertRejectedWithoutChanges(() -> traineeService.delete(anna));
        assertRejectedWithoutChanges(() -> trainerService.toggleActive(john));
        assertRejectedWithoutChanges(() -> trainerService.changePassword(john, "hacked"));
    }

    @Test
    void outsiderCantAddSomeoneElsesTraining() {
        Trainer david = trainerService.create(new TrainerRegistration("David", "Brown", "Fitness"));
        Credentials davidCredentials = new Credentials(david.getUser().getUsername(), david.getUser().getPassword());

        assertRejectedWithoutChanges(() -> trainingService.create(davidCredentials, new NewTraining(john.username(),
                anna.username(), "Yoga 2", LocalDate.of(2026, 9, 2), 60)));
    }

    @Test
    void oldPasswordStopsWorkingAfterAChange() {
        traineeService.changePassword(john, "newPassword");

        assertRejectedWithoutChanges(() -> traineeService.getByUsername(john, john.username()));
        assertTrue(authenticationService.traineeCredentialsMatch(new Credentials(john.username(), "newPassword")));
    }

    @Test
    void deactivatedUsersCanStillAuthenticate() {
        traineeService.toggleActive(john);

        assertEquals(true, traineeService.toggleActive(john));
    }
}
