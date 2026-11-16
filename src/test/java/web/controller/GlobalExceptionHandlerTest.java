package web.controller;

import io.gymcrm.web.error.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void unexpectedErrorsHideTheirDetails() {
        ResponseEntity<ProblemDetail> response =
                handler.handleUnexpected(new IllegalStateException("connection to db-host:5432 refused"));

        assertEquals(500, response.getStatusCode().value());
        assertEquals("Unexpected error, please try again later", response.getBody().getDetail());
    }
}
