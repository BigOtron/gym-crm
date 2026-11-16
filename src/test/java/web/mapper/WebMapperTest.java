package web.mapper;

import io.gymcrm.dto.TrainerUpdate;
import io.gymcrm.entities.Trainee;
import io.gymcrm.entities.Trainer;
import io.gymcrm.entities.Training;
import io.gymcrm.entities.User;
import io.gymcrm.web.dto.CredentialsResponse;
import io.gymcrm.web.dto.TraineeProfileResponse;
import io.gymcrm.web.dto.TraineeSummary;
import io.gymcrm.web.dto.TraineeTrainingResponse;
import io.gymcrm.web.dto.TrainerProfileResponse;
import io.gymcrm.web.dto.TrainerSummary;
import io.gymcrm.web.dto.TrainerTrainingResponse;
import io.gymcrm.web.dto.TrainerUpdateRequest;
import io.gymcrm.web.dto.TrainingTypeResponse;
import io.gymcrm.web.mapper.WebMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static support.TestData.FITNESS;
import static support.TestData.YOGA;
import static support.TestData.trainee;
import static support.TestData.trainer;

class WebMapperTest {

    private static final TrainingTypeResponse YOGA_RESPONSE = new TrainingTypeResponse(6L, "Yoga");

    private static Trainee john() {
        Trainee john = trainee("John.Smith", "secret");
        john.setDateOfBirth(LocalDate.of(1995, 4, 12));
        john.setAddress("Tashkent");
        return john;
    }

    private static Trainer namedTrainer(String username, String first, String last) {
        Trainer trainer = trainer(username, "pw", YOGA);
        trainer.getUser().setFirstName(first);
        trainer.getUser().setLastName(last);
        return trainer;
    }

    @Test
    void credentials() {
        User user = new User("John", "Smith", "John.Smith", "secret", true);

        assertEquals(new CredentialsResponse("John.Smith", "secret"), WebMapper.toCredentials(user));
    }

    @Test
    void traineeProfileWithTrainersSortedByUsername() {
        Trainee john = john();
        john.getTrainers().add(namedTrainer("Zed.Last", "Zed", "Last"));
        john.getTrainers().add(namedTrainer("Anna.Lee", "Anna", "Lee"));

        TraineeProfileResponse profile = WebMapper.toProfile(john);

        assertEquals(new TraineeProfileResponse("John.Smith", "John", "Smith", LocalDate.of(1995, 4, 12),
                "Tashkent", true, List.of(
                new TrainerSummary("Anna.Lee", "Anna", "Lee", YOGA_RESPONSE),
                new TrainerSummary("Zed.Last", "Zed", "Last", YOGA_RESPONSE))), profile);
    }

    @Test
    void trainerProfileWithTrainees() {
        Trainer anna = namedTrainer("Anna.Lee", "Anna", "Lee");
        anna.getUser().setActive(false);
        anna.getTrainees().add(john());

        TrainerProfileResponse profile = WebMapper.toProfile(anna);

        assertEquals(new TrainerProfileResponse("Anna.Lee", "Anna", "Lee", YOGA_RESPONSE, false,
                List.of(new TraineeSummary("John.Smith", "John", "Smith"))), profile);
    }

    @Test
    void trainingsShowTheOtherPersonsFullName() {
        Training training = new Training();
        training.setTrainee(john());
        training.setTrainer(namedTrainer("Anna.Lee", "Anna", "Lee"));
        training.setTrainingName("Evening yoga");
        training.setTrainingType(YOGA);
        training.setTrainingDate(LocalDate.of(2026, 9, 20));
        training.setTrainingDuration(60);

        assertEquals(List.of(new TraineeTrainingResponse("Evening yoga", LocalDate.of(2026, 9, 20), "Yoga", 60,
                "Anna Lee")), WebMapper.toTraineeTrainings(List.of(training)));
        assertEquals(List.of(new TrainerTrainingResponse("Evening yoga", LocalDate.of(2026, 9, 20), "Yoga", 60,
                "John Smith")), WebMapper.toTrainerTrainings(List.of(training)));
    }

    @Test
    void trainingTypes() {
        assertEquals(List.of(new TrainingTypeResponse(1L, "Fitness"), YOGA_RESPONSE),
                WebMapper.toTrainingTypes(List.of(FITNESS, YOGA)));
    }

    @Test
    void trainerUpdateDropsReadOnlySpecialization() {
        assertEquals(new TrainerUpdate("Ann", "Lee", false),
                WebMapper.toUpdate(new TrainerUpdateRequest("Ann", "Lee", 1L, false)));
    }
}
