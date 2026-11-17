package integration.workload;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import support.FakeWorkloadService;
import support.WebTestBase;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Gym CRM with the real workload client: load balancer, service token, transaction id, timeouts and
 * circuit breaker. The workload service is a fake, found under its Eureka name through Spring Cloud's
 * simple discovery (Eureka itself is off in tests).
 */
class TrainerWorkloadIntegrationTest extends WebTestBase {

    private static final String SECRET = "test-only-workload-secret-with-32-bytes-at-least";
    private static final FakeWorkloadService WORKLOAD = new FakeWorkloadService();

    private static final String TRAINING = """
            {"traineeUsername":"John.Smith","trainerUsername":"Anna.Lee","trainingName":"Evening yoga",
             "trainingDate":"2026-11-20","trainingDuration":60}""";

    @DynamicPropertySource
    static void workloadService(DynamicPropertyRegistry registry) {
        registry.add("gymcrm.workload.enabled", () -> "true");
        registry.add("spring.cloud.discovery.client.simple.instances.trainer-workload[0].uri", WORKLOAD::url);
        registry.add("gymcrm.workload.read-timeout", () -> "300ms");
        // Open after 2 failed calls in a row and stay open, so the tests can see it.
        registry.add("gymcrm.workload.circuit-breaker.sliding-window-size", () -> "2");
        registry.add("gymcrm.workload.circuit-breaker.minimum-number-of-calls", () -> "2");
        registry.add("gymcrm.workload.circuit-breaker.wait-duration-in-open-state", () -> "1h");
    }

    @Autowired
    private Resilience4JCircuitBreakerFactory circuitBreakers;

    @BeforeEach
    void resetWorkloadService() {
        WORKLOAD.reset();
        // find, not circuitBreaker(...): that would create it here with default settings instead of ours.
        circuitBreakers.getCircuitBreakerRegistry().find("trainer-workload").ifPresent(CircuitBreaker::reset);
    }

    @AfterAll
    static void stopWorkloadService() {
        WORKLOAD.close();
    }

    private int trainingsOfJohn() throws Exception {
        String body = mvc.perform(get("/api/trainees/John.Smith/trainings").header("Authorization", john))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return body.equals("[]") ? 0 : body.split("\"trainingName\"").length - 1;
    }

    @Test
    void addedTrainingIsReportedWithAServiceTokenAndTheTransactionId() throws Exception {
        String transactionId = mvc.perform(json(post("/api/trainings"), john, TRAINING)
                        .header("X-Transaction-Id", "tx-add-training"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader("X-Transaction-Id");

        List<FakeWorkloadService.Request> requests = WORKLOAD.requests();
        assertEquals(1, requests.size());
        FakeWorkloadService.Request request = requests.get(0);
        assertEquals("POST", request.method());
        assertEquals("/api/trainer-workloads", request.path());
        assertEquals(transactionId, request.transactionId());
        assertTrue(request.body().contains("\"trainerUsername\":\"Anna.Lee\""), request.body());
        assertTrue(request.body().contains("\"trainerFirstName\":\"Anna\""), request.body());
        assertTrue(request.body().contains("\"isActive\":true"), request.body());
        assertTrue(request.body().contains("\"trainingDate\":\"2026-11-20\""), request.body());
        assertTrue(request.body().contains("\"trainingDuration\":60"), request.body());
        assertTrue(request.body().contains("\"actionType\":\"ADD\""), request.body());

        Jwt token = decode(request.authorization());
        assertEquals("gym-crm", token.getClaimAsString("iss"));
        assertEquals(List.of("trainer-workload"), token.getAudience());
        assertEquals("workload.write workload.read", token.getClaimAsString("scope"));
    }

    @Test
    void deletingATraineeReportsEachOfTheirTrainings() throws Exception {
        addTraining(john, "Anna.Lee", "Yoga 1", "2026-11-20");
        addTraining(john, "David.Brown", "Lifting", "2026-12-01");
        WORKLOAD.reset();

        mvc.perform(delete("/api/trainees/John.Smith").header("Authorization", john))
                .andExpect(status().isOk());

        List<FakeWorkloadService.Request> requests = WORKLOAD.requests();
        assertEquals(2, requests.size());
        assertTrue(requests.stream().allMatch(r -> r.body().contains("\"actionType\":\"DELETE\"")));
        assertTrue(requests.stream().anyMatch(r -> r.body().contains("\"trainerUsername\":\"Anna.Lee\"")));
        assertTrue(requests.stream().anyMatch(r -> r.body().contains("\"trainerUsername\":\"David.Brown\"")));
    }

    @Test
    void timeoutReturns503AndTheTrainingIsNotSaved() throws Exception {
        WORKLOAD.delay(1000);

        mvc.perform(json(post("/api/trainings"), john, TRAINING))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail")
                        .value("The trainer workload service is unavailable, please try again later"));

        assertEquals(0, trainingsOfJohn());
    }

    @Test
    void errorFromTheWorkloadServiceKeepsTheTrainee() throws Exception {
        addTraining(john, "Anna.Lee", "Yoga 1", "2026-11-20");
        WORKLOAD.respond(500, "");

        mvc.perform(delete("/api/trainees/John.Smith").header("Authorization", john))
                .andExpect(status().isServiceUnavailable());

        assertEquals(1, trainingsOfJohn());
    }

    @Test
    void circuitOpensAfterRepeatedFailuresAndThenFailsFast() throws Exception {
        WORKLOAD.respond(500, "");
        for (int i = 0; i < 2; i++) {
            mvc.perform(json(post("/api/trainings"), john, TRAINING)).andExpect(status().isServiceUnavailable());
        }
        assertEquals(2, WORKLOAD.requests().size());

        WORKLOAD.respond(200, "");
        mvc.perform(json(post("/api/trainings"), john, TRAINING)).andExpect(status().isServiceUnavailable());

        // Open circuit: the call was refused without reaching the workload service.
        assertEquals(2, WORKLOAD.requests().size());
    }

    @Test
    void trainerSeesTheirWorkload() throws Exception {
        WORKLOAD.respond(200, """
                {"trainerUsername":"Anna.Lee","trainerFirstName":"Anna","trainerLastName":"Lee",
                 "trainerStatus":"ACTIVE","years":[{"year":2026,"months":[{"month":11,"trainingSummaryDuration":60}]}]}""");

        mvc.perform(get("/api/trainers/Anna.Lee/workload").header("Authorization", anna))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainerUsername").value("Anna.Lee"))
                .andExpect(jsonPath("$.trainerStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.years[0].year").value(2026))
                .andExpect(jsonPath("$.years[0].months[0].month").value(11))
                .andExpect(jsonPath("$.years[0].months[0].trainingSummaryDuration").value(60));

        FakeWorkloadService.Request request = WORKLOAD.requests().get(0);
        assertEquals("GET", request.method());
        assertEquals("/api/trainer-workloads/Anna.Lee", request.path());
    }

    @Test
    void trainerWithoutReportedTrainingsHasAnEmptyWorkload() throws Exception {
        WORKLOAD.respond(404, """
                {"status":404,"detail":"No workload found for trainer Anna.Lee"}""");

        mvc.perform(get("/api/trainers/Anna.Lee/workload").header("Authorization", anna))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainerFirstName").value("Anna"))
                .andExpect(jsonPath("$.years").isEmpty());
    }

    @Test
    void onlyTheTrainerSeesTheirWorkload() throws Exception {
        mvc.perform(get("/api/trainers/Anna.Lee/workload").header("Authorization", john))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/trainers/Anna.Lee/workload").header("Authorization", david))
                .andExpect(status().isForbidden());

        assertTrue(WORKLOAD.requests().isEmpty());
    }

    private static Jwt decode(String authorization) {
        assertTrue(authorization.startsWith("Bearer "), authorization);
        return NimbusJwtDecoder.withSecretKey(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256).build()
                .decode(authorization.substring("Bearer ".length()));
    }
}
