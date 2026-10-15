package web.dto;

import io.gymcrm.web.dto.ActiveStatusRequest;
import io.gymcrm.web.dto.ChangePasswordRequest;
import io.gymcrm.web.dto.NewTrainingRequest;
import io.gymcrm.web.dto.TraineeRegistrationRequest;
import io.gymcrm.web.dto.TraineeTrainingsQuery;
import io.gymcrm.web.dto.TraineeUpdateRequest;
import io.gymcrm.web.dto.TrainerRegistrationRequest;
import io.gymcrm.web.dto.TrainerTrainingsQuery;
import io.gymcrm.web.dto.TrainerUpdateRequest;
import io.gymcrm.web.dto.UpdateTrainersRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RequestValidationTest {

    private static final LocalDate PAST = LocalDate.of(1995, 4, 12);

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    /** Names of the invalid fields, e.g. {@code [firstName, isActive]}. */
    private static Set<String> invalidFields(Object request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    private static Set<String> set(String... fields) {
        return Set.copyOf(Arrays.asList(fields));
    }

    @Test
    void traineeRegistration() {
        assertEquals(set(), invalidFields(new TraineeRegistrationRequest("John", "Smith", null, null)));
        assertEquals(set(), invalidFields(new TraineeRegistrationRequest("John", "Smith", PAST, "Tashkent")));
        assertEquals(set("firstName", "lastName"), invalidFields(new TraineeRegistrationRequest(" ", null, null, null)));
        assertEquals(set("dateOfBirth"),
                invalidFields(new TraineeRegistrationRequest("John", "Smith", LocalDate.now().plusDays(1), null)));
        assertEquals(set("firstName"), invalidFields(new TraineeRegistrationRequest("J".repeat(101), "Smith", null, null)));
    }

    @Test
    void trainerRegistration() {
        assertEquals(set(), invalidFields(new TrainerRegistrationRequest("Anna", "Lee", 6L)));
        assertEquals(set("specializationId"), invalidFields(new TrainerRegistrationRequest("Anna", "Lee", null)));
    }

    @Test
    void changePassword() {
        assertEquals(set(), invalidFields(new ChangePasswordRequest("John.Smith", "old", "new")));
        assertEquals(set("username", "oldPassword", "newPassword"),
                invalidFields(new ChangePasswordRequest(null, "", " ")));
    }

    @Test
    void changePasswordToStringHidesPasswords() {
        String text = new ChangePasswordRequest("John.Smith", "old-secret", "new-secret").toString();

        assertEquals("ChangePasswordRequest[username=John.Smith]", text);
    }

    @Test
    void traineeUpdateRequiresNamesAndIsActive() {
        assertEquals(set(), invalidFields(new TraineeUpdateRequest("John", "Smith", null, null, false)));
        assertEquals(set("firstName", "isActive"), invalidFields(new TraineeUpdateRequest("", "Smith", PAST, null, null)));
    }

    @Test
    void trainerUpdateRequiresNamesAndIsActiveButNotSpecialization() {
        assertEquals(set(), invalidFields(new TrainerUpdateRequest("Anna", "Lee", null, true)));
        assertEquals(set("lastName", "isActive"), invalidFields(new TrainerUpdateRequest("Anna", null, 6L, null)));
    }

    @Test
    void activeStatus() {
        assertEquals(set(), invalidFields(new ActiveStatusRequest(false)));
        assertEquals(set("isActive"), invalidFields(new ActiveStatusRequest(null)));
    }

    @Test
    void updateTrainersAllowsEmptyListButNotNullOrBlankNames() {
        assertEquals(set(), invalidFields(new UpdateTrainersRequest(List.of())));
        assertEquals(set("trainerUsernames"), invalidFields(new UpdateTrainersRequest(null)));
        assertEquals(set("trainerUsernames[1].<list element>"),
                invalidFields(new UpdateTrainersRequest(List.of("Anna.Lee", " "))));
    }

    @Test
    void newTraining() {
        assertEquals(set(), invalidFields(new NewTrainingRequest("John.Smith", "Anna.Lee", "Yoga", PAST, 60)));
        assertEquals(set("trainingDuration"),
                invalidFields(new NewTrainingRequest("John.Smith", "Anna.Lee", "Yoga", PAST, 0)));
        assertEquals(set("traineeUsername", "trainerUsername", "trainingName", "trainingDate", "trainingDuration"),
                invalidFields(new NewTrainingRequest(null, "", " ", null, null)));
    }

    @Test
    void trainingQueriesRejectReversedPeriod() {
        LocalDate sep1 = LocalDate.of(2026, 9, 1);
        LocalDate sep30 = LocalDate.of(2026, 9, 30);

        assertEquals(set(), invalidFields(new TraineeTrainingsQuery(null, null, null, null)));
        assertEquals(set(), invalidFields(new TraineeTrainingsQuery(sep1, sep1, null, null)));
        assertEquals(set("periodValid"), invalidFields(new TraineeTrainingsQuery(sep30, sep1, null, null)));
        assertEquals(set("periodValid"), invalidFields(new TrainerTrainingsQuery(sep30, sep1, null)));
    }
}
