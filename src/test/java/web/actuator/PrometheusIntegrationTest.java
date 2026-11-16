package web.actuator;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import support.WebTestBase;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Reads /actuator/prometheus the way a Prometheus server does, after real API calls.
 * The Spring context (and its metric registry) is shared between test classes, so the counters
 * are compared before and after instead of expecting exact totals.
 */
class PrometheusIntegrationTest extends WebTestBase {

    private String scrape() throws Exception {
        return mvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    /** The value of one series, e.g. {@code gymcrm_logins_total{application="gym-crm",result="success"}}. */
    private static double value(String scrape, String name, String labels) {
        Matcher m = Pattern.compile("^" + Pattern.quote(name + "{" + labels + "}") + " (\\S+)$", Pattern.MULTILINE)
                .matcher(scrape);
        return m.find() ? Double.parseDouble(m.group(1)) : 0;
    }

    @Test
    void registrationsAreExported() throws Exception {
        String before = scrape();

        register("/api/trainees", """
                {"firstName":"Maria","lastName":"Garcia"}""");

        String after = scrape();
        String labels = "application=\"gym-crm\",role=\"trainee\"";
        assertEquals(1, value(after, "gymcrm_registrations_total", labels)
                - value(before, "gymcrm_registrations_total", labels));
    }

    @Test
    void loginsAreCountedBySuccessAndFailure() throws Exception {
        String before = scrape();

        mvc.perform(get("/api/auth/login").header("Authorization", john)).andExpect(status().isOk());
        mvc.perform(get("/api/auth/login").header("Authorization", basic("John.Smith", "wrong")))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/login").header("Authorization", basic("John.Smith", "wrong")))
                .andExpect(status().isUnauthorized());

        String after = scrape();
        String success = "application=\"gym-crm\",result=\"success\"";
        String failure = "application=\"gym-crm\",result=\"failure\"";
        assertEquals(1, value(after, "gymcrm_logins_total", success) - value(before, "gymcrm_logins_total", success));
        assertEquals(2, value(after, "gymcrm_logins_total", failure) - value(before, "gymcrm_logins_total", failure));
    }

    @Test
    void trainingsAndTheirDurationsAreExportedPerType() throws Exception {
        String before = scrape();

        addTraining(john, "Anna.Lee", "Yoga 1", "2026-09-01");
        addTraining(john, "Anna.Lee", "Yoga 2", "2026-09-02");

        String after = scrape();
        String yoga = "application=\"gym-crm\",type=\"Yoga\"";
        assertEquals(2, value(after, "gymcrm_trainings_added_total", yoga)
                - value(before, "gymcrm_trainings_added_total", yoga));
        assertEquals(120, value(after, "gymcrm_trainings_duration_minutes_sum", yoga)
                - value(before, "gymcrm_trainings_duration_minutes_sum", yoga));
    }

    @Test
    void activeUsersReflectTheDatabaseRightNow() throws Exception {
        // The base class registered John, Anna and David.
        String scrape = scrape();
        assertEquals(1, value(scrape, "gymcrm_users_active", "application=\"gym-crm\",role=\"trainee\""));
        assertEquals(2, value(scrape, "gymcrm_users_active", "application=\"gym-crm\",role=\"trainer\""));

        mvc.perform(json(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                        "/api/trainers/Anna.Lee/status"), anna, """
                        {"isActive":false}"""))
                .andExpect(status().isOk());

        assertEquals(1, value(scrape(), "gymcrm_users_active", "application=\"gym-crm\",role=\"trainer\""));
    }

    @Test
    void springBootsOwnMetricsAreThereToo() throws Exception {
        mvc.perform(post("/api/trainees").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        String scrape = scrape();
        assertTrue(scrape.contains("http_server_requests_seconds_count{"), "HTTP request timings");
        assertTrue(scrape.contains("hikaricp_connections_active{"), "connection pool");
        assertTrue(scrape.contains("jvm_memory_used_bytes{"), "JVM");
        assertTrue(scrape.contains("# HELP gymcrm_registrations_total Trainee and trainer profiles created"),
                "descriptions become HELP lines");
    }
}
