package web.controller.unit;

import io.gymcrm.dto.TraineeRegistration;
import io.gymcrm.dto.TraineeTrainingCriteria;
import io.gymcrm.dto.TraineeUpdate;
import io.gymcrm.entities.Trainee;
import io.gymcrm.entities.Training;
import io.gymcrm.exceptions.AuthenticationException;
import io.gymcrm.exceptions.ConflictException;
import io.gymcrm.exceptions.ForbiddenException;
import io.gymcrm.exceptions.NotFoundException;
import io.gymcrm.facade.GymFacade;
import io.gymcrm.web.controller.TraineeController;
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

import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static support.ControllerTestSupport.JOHN;
import static support.ControllerTestSupport.basic;
import static support.ControllerTestSupport.mockMvc;
import static support.TestData.FITNESS;
import static support.TestData.YOGA;
import static support.TestData.trainee;
import static support.TestData.trainer;

@ExtendWith(MockitoExtension.class)
class TraineeControllerTest {

    @Mock
    private GymFacade facade;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = mockMvc(new TraineeController(facade));
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.header("Authorization", basic(JOHN)).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static Trainee john() {
        Trainee john = trainee("John.Smith", "secret");
        john.setDateOfBirth(LocalDate.of(1995, 4, 12));
        john.setAddress("Tashkent");
        john.getTrainers().add(trainer("Anna.Lee", "pw", YOGA));
        return john;
    }

    // 1. Registration

    @Test
    void register() throws Exception {
        when(facade.createTrainee(new TraineeRegistration("John", "Smith", LocalDate.of(1995, 4, 12), null)))
                .thenReturn(trainee("John.Smith", "generated1"));

        mvc.perform(post("/api/trainees").contentType(MediaType.APPLICATION_JSON).content("""
                        {"firstName":"John","lastName":"Smith","dateOfBirth":"1995-04-12"}"""))
                .andExpect(status().isCreated())
                .andExpect(content().json("""
                        {"username":"John.Smith","password":"generated1"}""", true));
    }

    @Test
    void registerWithInvalidFields() throws Exception {
        mvc.perform(post("/api/trainees").contentType(MediaType.APPLICATION_JSON).content("""
                        {"firstName":"John","dateOfBirth":"2999-01-01"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.lastName").value("must not be blank"))
                .andExpect(jsonPath("$.errors.dateOfBirth").value("must be a past date"));

        verifyNoInteractions(facade);
    }

    @Test
    void registerWithWrongDateFormat() throws Exception {
        mvc.perform(post("/api/trainees").contentType(MediaType.APPLICATION_JSON).content("""
                        {"firstName":"John","lastName":"Smith","dateOfBirth":"12.04.1995"}"""))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(facade);
    }

    // 5. Get profile

    @Test
    void getProfile() throws Exception {
        when(facade.getTraineeByUsername(JOHN, "John.Smith")).thenReturn(john());

        mvc.perform(get("/api/trainees/John.Smith").header("Authorization", basic(JOHN)))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"username":"John.Smith","firstName":"John","lastName":"Smith","dateOfBirth":"1995-04-12",
                         "address":"Tashkent","isActive":true,
                         "trainers":[{"username":"Anna.Lee","firstName":"Anna","lastName":"Lee",
                                      "specialization":{"id":6,"trainingType":"Yoga"}}]}""", true));
    }

    @Test
    void getUnknownProfile() throws Exception {
        when(facade.getTraineeByUsername(JOHN, "Nobody")).thenThrow(new NotFoundException("Trainee not found: Nobody"));

        mvc.perform(get("/api/trainees/Nobody").header("Authorization", basic(JOHN)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Trainee not found: Nobody"));
    }

    @Test
    void getProfileWithoutCredentials() throws Exception {
        mvc.perform(get("/api/trainees/John.Smith"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(facade);
    }

    @Test
    void getProfileWithWrongPassword() throws Exception {
        when(facade.getTraineeByUsername(any(), any())).thenThrow(new AuthenticationException("Invalid username or password"));

        mvc.perform(get("/api/trainees/John.Smith").header("Authorization", basic(JOHN)))
                .andExpect(status().isUnauthorized());
    }

    // 6. Update

    @Test
    void update() throws Exception {
        when(facade.updateTrainee(JOHN, "John.Smith", new TraineeUpdate("Johnny", "Smith", null, "Samarkand", false)))
                .thenReturn(john());

        mvc.perform(json(put("/api/trainees/John.Smith"), """
                        {"firstName":"Johnny","lastName":"Smith","address":"Samarkand","isActive":false}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("John.Smith"))
                .andExpect(jsonPath("$.trainers[0].username").value("Anna.Lee"));
    }

    @Test
    void updateWithoutIsActive() throws Exception {
        mvc.perform(json(put("/api/trainees/John.Smith"), """
                        {"firstName":"Johnny","lastName":"Smith"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.isActive").value("must not be null"));

        verifyNoInteractions(facade);
    }

    @Test
    void updateSomeoneElsesProfile() throws Exception {
        when(facade.updateTrainee(any(), any(), any()))
                .thenThrow(new ForbiddenException("You can only change your own profile"));

        mvc.perform(json(put("/api/trainees/Maria.Garcia"), """
                        {"firstName":"Maria","lastName":"Garcia","isActive":true}"""))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("You can only change your own profile"));
    }

    // 7. Delete

    @Test
    void deleteOwnProfile() throws Exception {
        mvc.perform(delete("/api/trainees/John.Smith").header("Authorization", basic(JOHN)))
                .andExpect(status().isOk());

        verify(facade).deleteTrainee(JOHN, "John.Smith");
    }

    @Test
    void deleteSomeoneElsesProfile() throws Exception {
        doThrow(new ForbiddenException("You can only change your own profile"))
                .when(facade).deleteTrainee(JOHN, "Maria.Garcia");

        mvc.perform(delete("/api/trainees/Maria.Garcia").header("Authorization", basic(JOHN)))
                .andExpect(status().isForbidden());
    }

    // 10. Available trainers

    @Test
    void availableTrainers() throws Exception {
        when(facade.getTrainersNotAssignedToTrainee(JOHN, "John.Smith"))
                .thenReturn(List.of(trainer("Anna.Lee", "pw", YOGA), trainer("David.Brown", "pw", FITNESS)));

        mvc.perform(get("/api/trainees/John.Smith/available-trainers").header("Authorization", basic(JOHN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].username", contains("Anna.Lee", "David.Brown")))
                .andExpect(jsonPath("$[1].specialization.trainingType").value("Fitness"));
    }

    @Test
    void availableTrainersOfUnknownTrainee() throws Exception {
        when(facade.getTrainersNotAssignedToTrainee(JOHN, "Nobody"))
                .thenThrow(new NotFoundException("Trainee not found: Nobody"));

        mvc.perform(get("/api/trainees/Nobody/available-trainers").header("Authorization", basic(JOHN)))
                .andExpect(status().isNotFound());
    }

    // 11. Update trainers list

    @Test
    void updateTrainers() throws Exception {
        when(facade.updateTraineeTrainers(JOHN, "John.Smith", List.of("Anna.Lee")))
                .thenReturn(List.of(trainer("Anna.Lee", "pw", YOGA)));

        mvc.perform(json(put("/api/trainees/John.Smith/trainers"), """
                        {"trainerUsernames":["Anna.Lee"]}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].username", contains("Anna.Lee")));
    }

    @Test
    void updateTrainersWithEmptyListRemovesAll() throws Exception {
        when(facade.updateTraineeTrainers(JOHN, "John.Smith", List.of())).thenReturn(List.of());

        mvc.perform(json(put("/api/trainees/John.Smith/trainers"), """
                        {"trainerUsernames":[]}"""))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void updateTrainersWithoutListOrWithBlankName() throws Exception {
        mvc.perform(json(put("/api/trainees/John.Smith/trainers"), "{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(json(put("/api/trainees/John.Smith/trainers"), """
                        {"trainerUsernames":["Anna.Lee"," "]}"""))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(facade);
    }

    @Test
    void updateTrainersWithUnknownTrainer() throws Exception {
        when(facade.updateTraineeTrainers(any(), any(), any()))
                .thenThrow(new NotFoundException("Trainers not found: [Nobody]"));

        mvc.perform(json(put("/api/trainees/John.Smith/trainers"), """
                        {"trainerUsernames":["Nobody"]}"""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Trainers not found: [Nobody]"));
    }

    // 12. Trainings

    @Test
    void trainingsWithAllFilters() throws Exception {
        Training training = new Training();
        training.setTrainee(john());
        training.setTrainer(trainer("Anna.Lee", "pw", YOGA));
        training.setTrainingName("Evening yoga");
        training.setTrainingType(YOGA);
        training.setTrainingDate(LocalDate.of(2026, 9, 20));
        training.setTrainingDuration(60);
        var criteria = new TraineeTrainingCriteria(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), "Anna", "Yoga");
        when(facade.getTraineeTrainings(JOHN, "John.Smith", criteria)).thenReturn(List.of(training));

        mvc.perform(get("/api/trainees/John.Smith/trainings").header("Authorization", basic(JOHN))
                        .param("from", "2026-09-01").param("to", "2026-09-30")
                        .param("trainerName", "Anna").param("trainingType", "Yoga"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [{"trainingName":"Evening yoga","trainingDate":"2026-09-20","trainingType":"Yoga",
                          "trainingDuration":60,"trainerName":"Anna Lee"}]""", true));
    }

    @Test
    void trainingsWithoutFilters() throws Exception {
        when(facade.getTraineeTrainings(JOHN, "John.Smith", TraineeTrainingCriteria.none())).thenReturn(List.of());

        mvc.perform(get("/api/trainees/John.Smith/trainings").header("Authorization", basic(JOHN)))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void trainingsWithInvalidFilters() throws Exception {
        mvc.perform(get("/api/trainees/John.Smith/trainings").header("Authorization", basic(JOHN))
                        .param("from", "2026-09-30").param("to", "2026-09-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.periodValid").value("'from' must not be after 'to'"));
        mvc.perform(get("/api/trainees/John.Smith/trainings").header("Authorization", basic(JOHN))
                        .param("to", "soon"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.to").value("invalid value 'soon'"));

        verifyNoInteractions(facade);
    }

    // 15. Status

    @Test
    void deactivate() throws Exception {
        mvc.perform(json(patch("/api/trainees/John.Smith/status"), """
                        {"isActive":false}"""))
                .andExpect(status().isOk());

        verify(facade).setTraineeActive(JOHN, "John.Smith", false);
    }

    @Test
    void activateWhenAlreadyActive() throws Exception {
        doThrow(new ConflictException("Trainee John.Smith is already active"))
                .when(facade).setTraineeActive(JOHN, "John.Smith", true);

        mvc.perform(json(patch("/api/trainees/John.Smith/status"), """
                        {"isActive":true}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Trainee John.Smith is already active"));
    }

    @Test
    void statusWithoutOrWithInvalidIsActive() throws Exception {
        mvc.perform(json(patch("/api/trainees/John.Smith/status"), "{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(json(patch("/api/trainees/John.Smith/status"), """
                        {"isActive":"maybe"}"""))
                .andExpect(status().isBadRequest());

        verify(facade, org.mockito.Mockito.never()).setTraineeActive(any(), any(), anyBoolean());
    }
}
