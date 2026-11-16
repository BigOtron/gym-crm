package web.controller;

import io.gymcrm.exceptions.LoginBlockedException;
import io.gymcrm.metrics.GymMetrics;
import io.gymcrm.web.error.GlobalExceptionHandler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(new GymMetrics(new SimpleMeterRegistry()));

    @Test
    void unexpectedErrorsHideTheirDetails() {
        ResponseEntity<ProblemDetail> response =
                handler.handleUnexpected(new IllegalStateException("connection to db-host:5432 refused"));

        assertEquals(500, response.getStatusCode().value());
        assertEquals("Unexpected error, please try again later", response.getBody().getDetail());
    }

    @Test
    void blockedLoginSaysWhenToRetryRoundedUp() {
        ResponseEntity<ProblemDetail> response =
                handler.handleLoginBlocked(new LoginBlockedException(Duration.ofMillis(299_001)));

        assertEquals(429, response.getStatusCode().value());
        assertEquals("300", response.getHeaders().getFirst("Retry-After"));
    }
}
