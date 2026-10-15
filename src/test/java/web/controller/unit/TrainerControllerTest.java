package web.controller.unit;

import io.gymcrm.dto.TrainerRegistration;
import io.gymcrm.dto.TrainerTrainingCriteria;
import io.gymcrm.dto.TrainerUpdate;
import io.gymcrm.entities.Trainer;
import io.gymcrm.entities.Training;
import io.gymcrm.exceptions.ConflictException;
import io.gymcrm.exceptions.ForbiddenException;
import io.gymcrm.exceptions.NotFoundException;
import io.gymcrm.facade.GymFacade;
import io.gymcrm.web.controller.TrainerController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static support.ControllerTestSupport.ANNA;
import static support.ControllerTestSupport.basic;
import static support.ControllerTestSupport.mockMvc;
import static support.TestData.YOGA;
import static support.TestData.trainee;
import static support.TestData.trainer;

@ExtendWith(MockitoExtension.class)
class TrainerControllerTest {

    @Mock
    private GymFacade facade;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = mockMvc(new TrainerController(facade));
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.header("Authorization", basic(ANNA)).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static Trainer anna() {
        Trainer anna = trainer("Anna.Lee", "secret", YOGA);
        anna.getTrainees().add(trainee("John.Smith", "pw"));
        return anna;
    }

    // 2. Registration

    @Test
    void register() throws Exception {
        when(facade.createTrainer(new TrainerRegistration("Anna", "Lee", 6L)))
                .thenReturn(trainer("Anna.Lee", "generated1", YOGA));

        mvc.perform(post("/api/trainers").contentType(MediaType.APPLICATION_JSON).content("""
                        {"firstName":"Anna","lastName":"Lee","specializationId":6}"""))
                .andExpect(status().isCreated())
                .andExpect(content().json("""
                        {"username":"Anna.Lee","password":"generated1"}""", true));
    }

    @Test
    void registerWithoutSpecialization() throws Exception {
        mvc.perform(post("/api/trainers").contentType(MediaType.APPLICATION_JSON).content("""
                        {"firstName":"Anna","lastName":"Lee"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.specializationId").value("must not be null"));

        verifyNoInteractions(facade);
    }

    @Test
    void registerWithUnknownSpecialization() throws Exception {
        when(facade.createTrainer(any())).thenThrow(new NotFoundException("Training type not found: 99"));

        mvc.perform(post("/api/trainers").contentType(MediaType.APPLICATION_JSON).content("""
                        {"firstName":"Anna","lastName":"Lee","specializationId":99}"""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Training type not found: 99"));
    }

    // 8. Get profile

    @Test
    void getProfile() throws Exception {
        when(facade.getTrainerByUsername(ANNA, "Anna.Lee")).thenReturn(anna());

        mvc.perform(get("/api/trainers/Anna.Lee").header("Authorization", basic(ANNA)))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"username":"Anna.Lee","firstName":"Anna","lastName":"Lee",
                         "specialization":{"id":6,"trainingType":"Yoga"},"isActive":true,
                         "trainees":[{"username":"John.Smith","firstName":"John","lastName":"Smith"}]}""", true));
    }

    @Test
    void getUnknownProfile() throws Exception {
        when(facade.getTrainerByUsername(ANNA, "Nobody")).thenThrow(new NotFoundException("Trainer not found: Nobody"));

        mvc.perform(get("/api/trainers/Nobody").header("Authorization", basic(ANNA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getProfileWithMalformedHeader() throws Exception {
        mvc.perform(get("/api/trainers/Anna.Lee").header("Authorization", "Basic not-base64!"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Malformed Basic authentication header"));

        verifyNoInteractions(facade);
    }

    // 9. Update

    @Test
    void updateIgnoresSpecialization() throws Exception {
        when(facade.updateTrainer(ANNA, "Anna.Lee", new TrainerUpdate("Ann", "Lee", true))).thenReturn(anna());

        mvc.perform(json(put("/api/trainers/Anna.Lee"), """
                        {"firstName":"Ann","lastName":"Lee","specializationId":1,"isActive":true}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.specialization.trainingType").value("Yoga"));
    }

    @Test
    void updateWithMissingNames() throws Exception {
        mvc.perform(json(put("/api/trainers/Anna.Lee"), """
                        {"isActive":true}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.firstName").value("must not be blank"))
                .andExpect(jsonPath("$.errors.lastName").value("must not be blank"));

        verifyNoInteractions(facade);
    }

    @Test
    void updateSomeoneElsesProfile() throws Exception {
        when(facade.updateTrainer(any(), any(), any())).thenThrow(new ForbiddenException("You can only change your own profile"));

        mvc.perform(json(put("/api/trainers/David.Brown"), """
                        {"firstName":"David","lastName":"Brown","isActive":true}"""))
                .andExpect(status().isForbidden());
    }

    // 13. Trainings

    @Test
    void trainingsWithFilters() throws Exception {
        Training training = new Training();
        training.setTrainee(trainee("John.Smith", "pw"));
        training.setTrainer(anna());
        training.setTrainingName("Evening yoga");
        training.setTrainingType(YOGA);
        training.setTrainingDate(LocalDate.of(2026, 9, 20));
        training.setTrainingDuration(45);
        var criteria = new TrainerTrainingCriteria(LocalDate.of(2026, 9, 1), null, "Smith");
        when(facade.getTrainerTrainings(ANNA, "Anna.Lee", criteria)).thenReturn(List.of(training));

        mvc.perform(get("/api/trainers/Anna.Lee/trainings").header("Authorization", basic(ANNA))
                        .param("from", "2026-09-01").param("traineeName", "Smith"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [{"trainingName":"Evening yoga","trainingDate":"2026-09-20","trainingType":"Yoga",
                          "trainingDuration":45,"traineeName":"John Smith"}]""", true));
    }

    @Test
    void trainingsWithReversedPeriod() throws Exception {
        mvc.perform(get("/api/trainers/Anna.Lee/trainings").header("Authorization", basic(ANNA))
                        .param("from", "2026-09-30").param("to", "2026-09-01"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(facade);
    }

    // 16. Status

    @Test
    void deactivate() throws Exception {
        mvc.perform(json(patch("/api/trainers/Anna.Lee/status"), """
                        {"isActive":false}"""))
                .andExpect(status().isOk());

        verify(facade).setTrainerActive(ANNA, "Anna.Lee", false);
    }

    @Test
    void deactivateWhenAlreadyInactive() throws Exception {
        doThrow(new ConflictException("Trainer Anna.Lee is already inactive"))
                .when(facade).setTrainerActive(ANNA, "Anna.Lee", false);

        mvc.perform(json(patch("/api/trainers/Anna.Lee/status"), """
                        {"isActive":false}"""))
                .andExpect(status().isConflict());
    }

    @Test
    void deactivateSomeoneElse() throws Exception {
        doThrow(new ForbiddenException("You can only change your own profile"))
                .when(facade).setTrainerActive(ANNA, "David.Brown", false);

        mvc.perform(json(patch("/api/trainers/David.Brown/status"), """
                        {"isActive":false}"""))
                .andExpect(status().isForbidden());
    }
}
