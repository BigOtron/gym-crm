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
import org.springframework.test.web.servlet.ResultActions;
import support.FakeWorkloadService;
import support.WebTestBase;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Reading workloads with the real REST client: load balancer, service token, transaction id, timeouts and
 * circuit breaker. The workload service is a fake, found under its Eureka name through Spring Cloud's
 * simple discovery (Eureka itself is off in tests). Reporting trainings goes through the message queue
 * instead, see WorkloadMessagingIntegrationTest.
 */
class TrainerWorkloadIntegrationTest extends WebTestBase {

    private static final String SECRET = "test-only-workload-secret-with-32-bytes-at-least";
    private static final FakeWorkloadService SERVICE = new FakeWorkloadService();

    private static final String WORKLOAD = """
            {"trainerUsername":"Anna.Lee","trainerFirstName":"Anna","trainerLastName":"Lee",
             "trainerStatus":"ACTIVE","years":[{"year":2026,"months":[{"month":11,"trainingSummaryDuration":60}]}]}""";

    @DynamicPropertySource
    static void workloadService(DynamicPropertyRegistry registry) {
        registry.add("gymcrm.workload.enabled", () -> "true");
        registry.add("spring.activemq.broker-url", () -> WorkloadMessagingIntegrationTest.BROKER);
        registry.add("spring.cloud.discovery.client.simple.instances.trainer-workload[0].uri", SERVICE::url);
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
        SERVICE.reset();
        // find, not circuitBreaker(...): that would create it here with default settings instead of ours.
        circuitBreakers.getCircuitBreakerRegistry().find("trainer-workload").ifPresent(CircuitBreaker::reset);
    }

    @AfterAll
    static void stopWorkloadService() {
        SERVICE.close();
    }

    private ResultActions readAnnasWorkload() throws Exception {
        return mvc.perform(get("/api/trainers/Anna.Lee/workload").header("Authorization", anna));
    }

    @Test
    void readsWithAReadOnlyServiceTokenAndTheTransactionId() throws Exception {
        SERVICE.respond(200, WORKLOAD);

        String transactionId = mvc.perform(get("/api/trainers/Anna.Lee/workload")
                        .header("Authorization", anna)
                        .header("X-Transaction-Id", "tx-read-workload"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader("X-Transaction-Id");

        FakeWorkloadService.Request request = SERVICE.requests().get(0);
        assertEquals(transactionId, request.transactionId());
        Jwt token = decode(request.authorization());
        assertEquals("gym-crm", token.getClaimAsString("iss"));
        assertEquals(List.of("trainer-workload"), token.getAudience());
        assertEquals("workload.read", token.getClaimAsString("scope"));
    }

    @Test
    void timeoutReturns503() throws Exception {
        SERVICE.delay(1000);

        readAnnasWorkload()
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail")
                        .value("The trainer workload service is unavailable, please try again later"));
    }

    @Test
    void rejectionIsA502AndDoesNotOpenTheCircuit() throws Exception {
        SERVICE.respond(401, """
                {"status":401,"detail":"Missing, invalid or expired token"}""");
        for (int i = 0; i < 3; i++) {
            readAnnasWorkload()
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.detail").value("The trainer workload service rejected the request"));
        }

        // Three failures would have opened the circuit (it opens after 2); rejections don't count.
        SERVICE.respond(200, WORKLOAD);
        readAnnasWorkload().andExpect(status().isOk());
        assertEquals(4, SERVICE.requests().size());
    }

    @Test
    void circuitOpensAfterRepeatedFailuresAndThenFailsFast() throws Exception {
        SERVICE.respond(500, "");
        for (int i = 0; i < 2; i++) {
            readAnnasWorkload().andExpect(status().isServiceUnavailable());
        }
        assertEquals(2, SERVICE.requests().size());

        SERVICE.respond(200, WORKLOAD);
        readAnnasWorkload().andExpect(status().isServiceUnavailable());

        // Open circuit: the call was refused without reaching the workload service.
        assertEquals(2, SERVICE.requests().size());
    }

    @Test
    void trainerSeesTheirWorkload() throws Exception {
        SERVICE.respond(200, WORKLOAD);

        readAnnasWorkload()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainerUsername").value("Anna.Lee"))
                .andExpect(jsonPath("$.trainerStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.years[0].year").value(2026))
                .andExpect(jsonPath("$.years[0].months[0].month").value(11))
                .andExpect(jsonPath("$.years[0].months[0].trainingSummaryDuration").value(60));

        FakeWorkloadService.Request request = SERVICE.requests().get(0);
        assertEquals("GET", request.method());
        assertEquals("/api/trainer-workloads/Anna.Lee", request.path());
    }

    @Test
    void deactivatedTrainerIsShownAsInactiveAlthoughTheWorkloadServiceDoesNotKnowYet() throws Exception {
        SERVICE.respond(200, WORKLOAD);
        mvc.perform(json(patch("/api/trainers/Anna.Lee/status"), anna, """
                {"isActive":false}""")).andExpect(status().isOk());

        readAnnasWorkload()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainerStatus").value("INACTIVE"))
                .andExpect(jsonPath("$.years[0].months[0].trainingSummaryDuration").value(60));
    }

    @Test
    void trainerWithoutReportedTrainingsHasAnEmptyWorkload() throws Exception {
        SERVICE.respond(404, """
                {"status":404,"detail":"No workload found for trainer Anna.Lee"}""");

        readAnnasWorkload()
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

        assertTrue(SERVICE.requests().isEmpty());
    }

    private static Jwt decode(String authorization) {
        assertTrue(authorization.startsWith("Bearer "), authorization);
        return NimbusJwtDecoder.withSecretKey(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256).build()
                .decode(authorization.substring("Bearer ".length()));
    }
}
