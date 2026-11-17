package io.gymcrm.workload.web;

import io.gymcrm.workload.support.TestTokens;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The whole service with its security, called the way Gym CRM calls it. */
@SpringBootTest
@AutoConfigureMockMvc
class WorkloadApiIntegrationTest {

    private static final String WRITE = TestTokens.bearer("workload.write");
    private static final String READ = TestTokens.bearer("workload.read");

    @Autowired
    private MockMvc mvc;

    /** Every test uses its own trainer, so the shared in-memory data doesn't matter. */
    private final String trainer = "Trainer-" + UUID.randomUUID();

    private ResultActions report(String token, String date, int minutes, String action) throws Exception {
        return mvc.perform(post("/api/trainer-workloads")
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"trainerUsername":"%s","trainerFirstName":"Anna","trainerLastName":"Lee","isActive":true,
                         "trainingDate":"%s","trainingDuration":%d,"actionType":"%s"}"""
                        .formatted(trainer, date, minutes, action)));
    }

    @Test
    void addAndDeleteTrainingsThenReadTheSummary() throws Exception {
        report(WRITE, "2026-11-03", 60, "ADD").andExpect(status().isOk());
        report(WRITE, "2026-11-20", 45, "ADD").andExpect(status().isOk());
        report(WRITE, "2027-01-10", 30, "ADD").andExpect(status().isOk());
        report(WRITE, "2026-11-03", 60, "DELETE").andExpect(status().isOk());

        mvc.perform(get("/api/trainer-workloads/" + trainer).header(HttpHeaders.AUTHORIZATION, READ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainerUsername").value(trainer))
                .andExpect(jsonPath("$.trainerStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.years[0].year").value(2026))
                .andExpect(jsonPath("$.years[0].months[0].month").value(11))
                .andExpect(jsonPath("$.years[0].months[0].trainingSummaryDuration").value(45))
                .andExpect(jsonPath("$.years[1].year").value(2027))
                .andExpect(jsonPath("$.years[1].months[0].trainingSummaryDuration").value(30));

        mvc.perform(get("/api/trainer-workloads/{u}/years/2026/months/11", trainer).header(HttpHeaders.AUTHORIZATION, READ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainingSummaryDuration").value(45));
        mvc.perform(get("/api/trainer-workloads/{u}/years/2026/months/12", trainer).header(HttpHeaders.AUTHORIZATION, READ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainingSummaryDuration").value(0));
    }

    @Test
    void unknownTrainerIsNotFound() throws Exception {
        mvc.perform(get("/api/trainer-workloads/" + trainer).header(HttpHeaders.AUTHORIZATION, READ))
                .andExpect(status().isNotFound());
    }

    @Test
    void withoutTokenIsUnauthorized() throws Exception {
        mvc.perform(post("/api/trainer-workloads").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.detail").value("Missing, invalid or expired token"));
        mvc.perform(get("/api/trainer-workloads/" + trainer))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsTokensNotMadeByGymCrmForThisService() throws Exception {
        List<String> invalid = List.of(
                TestTokens.bearer("another-secret-that-is-also-32-bytes-long!!", claims -> { }),
                TestTokens.bearer(TestTokens.SECRET, claims -> claims.issuer("someone-else")),
                TestTokens.bearer(TestTokens.SECRET, claims -> claims.audience(List.of("another-service"))),
                TestTokens.bearer(TestTokens.SECRET, claims -> claims
                        .issuedAt(Instant.now().minusSeconds(3600)).expiresAt(Instant.now().minusSeconds(600))),
                "Bearer not-a-jwt");
        for (String token : invalid) {
            report(token, "2026-11-03", 60, "ADD").andExpect(status().isUnauthorized());
        }
    }

    @Test
    void needsTheRightScope() throws Exception {
        report(READ, "2026-11-03", 60, "ADD")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("The token does not allow this operation"));
        mvc.perform(get("/api/trainer-workloads/" + trainer).header(HttpHeaders.AUTHORIZATION, WRITE))
                .andExpect(status().isForbidden());
    }

    @Test
    void otherPathsAreClosed() throws Exception {
        mvc.perform(get("/api/anything").header(HttpHeaders.AUTHORIZATION, TestTokens.bearer("workload.read", "workload.write")))
                .andExpect(status().isForbidden());
    }

    @Test
    void reusesTheCallersTransactionId() throws Exception {
        mvc.perform(get("/api/trainer-workloads/" + trainer)
                        .header(HttpHeaders.AUTHORIZATION, READ)
                        .header("X-Transaction-Id", "tx-from-gym-crm"))
                .andExpect(header().string("X-Transaction-Id", "tx-from-gym-crm"));
    }

    @Test
    void healthIsPublic() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
