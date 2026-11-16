package web.actuator;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import support.LogCapture;
import support.WebTestBase;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Calls the real actuator endpoints. The base class registers John (trainee), Anna (Yoga) and
 * David (Heavy lifting), so four of the six training types have no trainer.
 */
class ActuatorIntegrationTest extends WebTestBase {

    @Test
    void healthShowsOurChecksAlongsideSpringBootsOwn() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.db.status").value("UP"))
                .andExpect(jsonPath("$.components.trainingTypes.status").value("UP"))
                .andExpect(jsonPath("$.components.trainingTypes.details.count").value(6))
                .andExpect(jsonPath("$.components.databaseResponseTime.status").value("UP"))
                .andExpect(jsonPath("$.components.databaseResponseTime.details.maxResponseTimeMs").value(500));
    }

    @Test
    void missingTrainersGiveAWarningThatStillAnswers200() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WARNING"))
                .andExpect(jsonPath("$.components.trainerAvailability.status").value("WARNING"))
                .andExpect(jsonPath("$.components.trainerAvailability.details.withoutTrainer",
                        containsInAnyOrder("Fitness", "Running", "Pull ups", "Push ups")))
                .andExpect(jsonPath("$.components.trainerAvailability.details.activeTrainers.Yoga").value(1));
    }

    @Test
    void everythingUpOnceEveryTypeHasATrainer() throws Exception {
        for (long type : new long[]{1, 2, 4, 5}) {
            mvc.perform(post("/api/trainers").contentType(MediaType.APPLICATION_JSON).content("""
                            {"firstName":"Coach","lastName":"Number%d","specializationId":%d}""".formatted(type, type)))
                    .andExpect(status().isCreated());
        }

        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.trainerAvailability.status").value("UP"));
    }

    @Test
    void infoShowsTheApplicationAndBuild() throws Exception {
        mvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.app.name").value("Gym CRM"))
                .andExpect(jsonPath("$.build.version").value("1.0-SNAPSHOT"))
                .andExpect(jsonPath("$.java.version").exists());
    }

    @Test
    void metricsEndpointIsExposed() throws Exception {
        mvc.perform(get("/actuator/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names", hasItem("jvm.memory.used")));
    }

    @Test
    void actuatorCallsAreNotLoggedAsRestCalls() throws Exception {
        try (LogCapture logs = new LogCapture("io.gymcrm.web.logging.RestCallLoggingFilter")) {
            mvc.perform(get("/actuator/health")).andExpect(status().isOk());

            assertTrue(logs.messages().isEmpty(), logs.messages().toString());
        }
    }
}
