package web.controller.unit;

import io.gymcrm.dto.NewTraining;
import io.gymcrm.exceptions.ForbiddenException;
import io.gymcrm.exceptions.NotFoundException;
import io.gymcrm.facade.GymFacade;
import io.gymcrm.web.controller.TrainingController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static support.ControllerTestSupport.JOHN;
import static support.ControllerTestSupport.basic;
import static support.ControllerTestSupport.mockMvc;

@ExtendWith(MockitoExtension.class)
class TrainingControllerTest {

    private static final String VALID = """
            {"traineeUsername":"John.Smith","trainerUsername":"Anna.Lee","trainingName":"Evening yoga",
             "trainingDate":"2026-09-20","trainingDuration":60}""";

    @Mock
    private GymFacade facade;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = mockMvc(new TrainingController(facade));
    }

    private static MockHttpServletRequestBuilder add(String body) {
        return post("/api/trainings").header("Authorization", basic(JOHN))
                .contentType(MediaType.APPLICATION_JSON).content(body);
    }

    @Test
    void addTraining() throws Exception {
        mvc.perform(add(VALID))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(facade).addTraining(JOHN, new NewTraining("John.Smith", "Anna.Lee", "Evening yoga",
                LocalDate.of(2026, 9, 20), 60));
    }

    @Test
    void addTrainingWithMissingFields() throws Exception {
        mvc.perform(add("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.traineeUsername").value("must not be blank"))
                .andExpect(jsonPath("$.errors.trainerUsername").value("must not be blank"))
                .andExpect(jsonPath("$.errors.trainingName").value("must not be blank"))
                .andExpect(jsonPath("$.errors.trainingDate").value("must not be null"))
                .andExpect(jsonPath("$.errors.trainingDuration").value("must not be null"));

        verifyNoInteractions(facade);
    }

    @Test
    void addTrainingWithNonPositiveDuration() throws Exception {
        mvc.perform(add(VALID.replace("\"trainingDuration\":60", "\"trainingDuration\":-5")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.trainingDuration").value("must be greater than 0"));

        verifyNoInteractions(facade);
    }

    @Test
    void addTrainingWithTextDuration() throws Exception {
        mvc.perform(add(VALID.replace("\"trainingDuration\":60", "\"trainingDuration\":\"an hour\"")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(facade);
    }

    @Test
    void addSomeoneElsesTraining() throws Exception {
        when(facade.addTraining(any(), any()))
                .thenThrow(new ForbiddenException("Only the trainee or the trainer can add a training"));

        mvc.perform(add(VALID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Only the trainee or the trainer can add a training"));
    }

    @Test
    void addTrainingWithUnknownTrainer() throws Exception {
        when(facade.addTraining(any(), any())).thenThrow(new NotFoundException("Trainer not found: Anna.Lee"));

        mvc.perform(add(VALID))
                .andExpect(status().isNotFound());
    }

    @Test
    void addTrainingWithoutCredentials() throws Exception {
        mvc.perform(post("/api/trainings").contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(facade);
    }
}
