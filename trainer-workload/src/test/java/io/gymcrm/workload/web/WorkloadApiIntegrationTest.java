package io.gymcrm.workload.web;

import io.gymcrm.workload.model.ActionType;
import io.gymcrm.workload.service.TrainerWorkloadService;
import io.gymcrm.workload.service.WorkloadUpdate;
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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The whole REST API with its security, called the way Gym CRM calls it. */
@SpringBootTest
@AutoConfigureMockMvc
class WorkloadApiIntegrationTest {

    private static final String READ = TestTokens.bearer("workload.read");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TrainerWorkloadService service;

    /** Every test uses its own trainer, so the shared in-memory data doesn't matter. */
    private final String trainer = "Trainer-" + UUID.randomUUID();

    /** Trainings arrive through the queue (see WorkloadMessagingIntegrationTest); here they are applied directly. */
    private void report(String date, int minutes, ActionType action) {
        service.apply(new WorkloadUpdate(trainer, "Anna", "Lee", true, LocalDate.parse(date), minutes, action));
    }

    private ResultActions readWorkload(String token) throws Exception {
        return mvc.perform(get("/api/trainer-workloads/" + trainer).header(HttpHeaders.AUTHORIZATION, token));
    }

    @Test
    void readTheSummaryOfAddedAndDeletedTrainings() throws Exception {
        report("2026-11-03", 60, ActionType.ADD);
        report("2026-11-20", 45, ActionType.ADD);
        report("2027-01-10", 30, ActionType.ADD);
        report("2026-11-03", 60, ActionType.DELETE);

        readWorkload(READ)
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
        readWorkload(READ).andExpect(status().isNotFound());
    }

    @Test
    void withoutTokenIsUnauthorized() throws Exception {
        mvc.perform(get("/api/trainer-workloads/" + trainer))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.detail").value("Missing, invalid or expired token"));
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
            readWorkload(token).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void needsTheReadScope() throws Exception {
        readWorkload(TestTokens.bearer("workload.write"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("The token does not allow this operation"));
    }

    @Test
    void trainingsCanNoLongerBeReportedOverRest() throws Exception {
        mvc.perform(post("/api/trainer-workloads")
                        .header(HttpHeaders.AUTHORIZATION, TestTokens.bearer("workload.read", "workload.write"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
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
