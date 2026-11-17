package io.gymcrm.workload.web.error;

import io.gymcrm.workload.service.TrainerNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Map;
import java.util.TreeMap;

/** Every error becomes an RFC 9457 problem detail; nothing internal (stack traces, class names) leaks out. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(TrainerNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(TrainerNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException e) {
        return problem(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /** No token, or a token that isn't valid (from the security filters, see SecurityErrorHandler). */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthentication(AuthenticationException e) {
        log.warn("401 Unauthorized: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Missing, invalid or expired token"));
    }

    /** A valid token without the scope this endpoint needs. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException e) {
        return problem(HttpStatus.FORBIDDEN, "The token does not allow this operation");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception e) {
        log.error("Unexpected error", e);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error, please try again later");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> errors = new TreeMap<>();
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            errors.merge(error.getField(), String.valueOf(error.getDefaultMessage()), (a, b) -> a + "; " + b);
        }
        ProblemDetail body = e.getBody();
        body.setDetail("Validation failed");
        body.setProperty("errors", errors);
        log.warn("Validation failed: {}", errors);
        return handleExceptionInternal(e, body, headers, status, request);
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
        if (status.is4xxClientError()) {
            log.warn("{} {}: {}", status.value(), status.getReasonPhrase(), detail);
        }
        return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
    }
}
