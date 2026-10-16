package web.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import io.gymcrm.web.logging.RestCallLoggingFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import support.LogCapture;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestCallLoggingFilterTest {

    private final RestCallLoggingFilter filter = new RestCallLoggingFilter();

    private static MockHttpServletRequest post(String uri, String body) {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", uri);
        request.setContentType("application/json");
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        return request;
    }

    /** Runs the filter with a "controller" that reads the body and answers with the given status and body. */
    private List<ILoggingEvent> run(MockHttpServletRequest request, MockHttpServletResponse response,
                                    int status, String responseBody) throws Exception {
        try (LogCapture logs = new LogCapture(RestCallLoggingFilter.class.getName())) {
            filter.doFilter(request, response, (req, res) -> {
                req.getInputStream().readAllBytes();
                ((jakarta.servlet.http.HttpServletResponse) res).setStatus(status);
                res.getWriter().write(responseBody);
            });
            return logs.events();
        }
    }

    @Test
    void logsEndpointRequestStatusAndResponse() throws Exception {
        MockHttpServletRequest request = post("/api/trainees/John.Smith", "{\"firstName\":\"John\"}");
        request.setQueryString("dry=true");

        List<ILoggingEvent> events = run(request, new MockHttpServletResponse(), 200, "{\"ok\":true}");

        assertEquals("Request PUT /api/trainees/John.Smith?dry=true", events.get(0).getFormattedMessage());
        String response = events.get(1).getFormattedMessage();
        assertTrue(response.startsWith("Response PUT /api/trainees/John.Smith?dry=true 200 in "), response);
        assertTrue(response.endsWith("request body: {\"firstName\":\"John\"}, response body: {\"ok\":true}"), response);
        assertEquals(Level.INFO, events.get(1).getLevel());
    }

    @Test
    void responseBodyStillReachesTheClient() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        run(post("/api/x", "{}"), response, 200, "{\"ok\":true}");

        assertEquals("{\"ok\":true}", response.getContentAsString());
    }

    @Test
    void masksPasswordsInBothDirections() throws Exception {
        List<ILoggingEvent> events = run(post("/api/auth/password",
                        "{\"username\":\"John.Smith\",\"oldPassword\":\"old-secret\",\"newPassword\":\"new\\\"secret\"}"),
                new MockHttpServletResponse(), 201, "{\"username\":\"John.Smith\",\"password\":\"generated1\"}");

        String line = events.get(1).getFormattedMessage();
        assertFalse(line.contains("secret"), line);
        assertFalse(line.contains("generated1"), line);
        assertTrue(line.contains("\"oldPassword\":\"***\""), line);
        assertTrue(line.contains("\"newPassword\":\"***\""), line);
        assertTrue(line.contains("\"password\":\"***\""), line);
        assertTrue(line.contains("\"username\":\"John.Smith\""), line);
    }

    @Test
    void neverLogsTheAuthorizationHeader() throws Exception {
        MockHttpServletRequest request = post("/api/x", "{}");
        request.addHeader("Authorization", "Basic Sm9objpzZWNyZXQ=");

        List<ILoggingEvent> events = run(request, new MockHttpServletResponse(), 200, "");

        events.forEach(e -> assertFalse(e.getFormattedMessage().contains("Sm9objpzZWNyZXQ="), e.getFormattedMessage()));
    }

    @Test
    void errorStatusesAreLoggedAsWarningsAndErrors() throws Exception {
        assertEquals(Level.WARN, run(post("/api/x", "{}"), new MockHttpServletResponse(), 404, "").get(1).getLevel());
        assertEquals(Level.ERROR, run(post("/api/x", "{}"), new MockHttpServletResponse(), 500, "").get(1).getLevel());
    }

    @Test
    void longBodiesAreTruncated() throws Exception {
        List<ILoggingEvent> events = run(post("/api/x", "x".repeat(5000)), new MockHttpServletResponse(), 200, "");

        String line = events.get(1).getFormattedMessage();
        assertTrue(line.contains("x".repeat(2000) + "...(truncated)"), "should be cut at 2000 characters");
        assertFalse(line.contains("x".repeat(2001)));
    }

    @Test
    void emptyBodies() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/training-types");

        List<ILoggingEvent> events = run(request, new MockHttpServletResponse(), 200, "");

        assertTrue(events.get(1).getFormattedMessage().endsWith("request body: <empty>, response body: <empty>"));
    }
}
