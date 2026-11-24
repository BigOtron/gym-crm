package integration.workload;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import support.WebTestBase;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** No broker at all: nothing listens on the configured port. */
class BrokerUnavailableIntegrationTest extends WebTestBase {

    @DynamicPropertySource
    static void noBroker(DynamicPropertyRegistry registry) {
        registry.add("gymcrm.workload.enabled", () -> "true");
        registry.add("spring.activemq.broker-url", () -> "tcp://localhost:1?connectionTimeout=500");
    }

    @Test
    void addingATrainingFailsWith503AndNothingIsSaved() throws Exception {
        mvc.perform(json(post("/api/trainings"), john, """
                        {"traineeUsername":"John.Smith","trainerUsername":"Anna.Lee","trainingName":"Evening yoga",
                         "trainingDate":"2026-11-20","trainingDuration":60}"""))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value("The message broker is unavailable, please try again later"));

        mvc.perform(get("/api/trainees/John.Smith/trainings").header("Authorization", john))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void everythingElseStillWorks() throws Exception {
        mvc.perform(get("/api/trainees/John.Smith").header("Authorization", john)).andExpect(status().isOk());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
