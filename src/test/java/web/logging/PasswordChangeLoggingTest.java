package web.logging;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import support.LogCapture;
import support.WebTestBase;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A wrong old password must be logged as a failed login of the user, without claiming a role
 * (previously a trainee's failure was logged as "Authentication failed for trainer ...").
 */
class PasswordChangeLoggingTest extends WebTestBase {

    @Test
    void wrongOldPasswordOfTraineeIsNotLoggedAsTrainer() throws Exception {
        try (LogCapture logs = new LogCapture("io.gymcrm.services")) {
            mvc.perform(put("/api/auth/password").contentType(MediaType.APPLICATION_JSON).content("""
                            {"username":"John.Smith","oldPassword":"wrong","newPassword":"brandNew1"}"""))
                    .andExpect(status().isUnauthorized());

            List<String> failures = logs.messages().stream().filter(m -> m.startsWith("Authentication failed")).toList();
            assertEquals(List.of("Authentication failed for user John.Smith"), failures);
        }
    }

    @Test
    void successfulChangeLogsTheRightRole() throws Exception {
        try (LogCapture logs = new LogCapture("io.gymcrm.services")) {
            mvc.perform(put("/api/auth/password").contentType(MediaType.APPLICATION_JSON).content("""
                            {"username":"John.Smith","oldPassword":"%s","newPassword":"brandNew1"}""".formatted(passwordOf(john))))
                    .andExpect(status().isOk());

            assertTrue(logs.messages().contains("Trainee John.Smith changed password"), logs.messages().toString());
            assertTrue(logs.messages().stream().noneMatch(m -> m.startsWith("Authentication failed")), logs.messages().toString());
        }
    }
}
