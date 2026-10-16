package web.controller.unit;

import io.gymcrm.exceptions.AuthenticationException;
import io.gymcrm.facade.GymFacade;
import io.gymcrm.web.controller.TrainingTypeController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static support.ControllerTestSupport.JOHN;
import static support.ControllerTestSupport.basic;
import static support.ControllerTestSupport.mockMvc;
import static support.TestData.FITNESS;
import static support.TestData.YOGA;

@ExtendWith(MockitoExtension.class)
class TrainingTypeControllerTest {

    @Mock
    private GymFacade facade;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = mockMvc(new TrainingTypeController(facade));
    }

    @Test
    void listTrainingTypes() throws Exception {
        when(facade.getTrainingTypes(JOHN)).thenReturn(List.of(FITNESS, YOGA));

        mvc.perform(get("/api/training-types").header("Authorization", basic(JOHN)))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [{"id":1,"trainingType":"Fitness"},{"id":6,"trainingType":"Yoga"}]""", true));
    }

    @Test
    void listTrainingTypesWithWrongPassword() throws Exception {
        when(facade.getTrainingTypes(JOHN)).thenThrow(new AuthenticationException("Invalid username or password"));

        mvc.perform(get("/api/training-types").header("Authorization", basic(JOHN)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listTrainingTypesWithoutCredentials() throws Exception {
        mvc.perform(get("/api/training-types"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(facade);
    }
}
