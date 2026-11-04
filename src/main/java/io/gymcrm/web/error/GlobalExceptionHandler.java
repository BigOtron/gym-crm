package io.gymcrm.web.error;

import io.gymcrm.exceptions.AuthenticationException;
import io.gymcrm.exceptions.ConflictException;
import io.gymcrm.exceptions.LoginBlockedException;
import io.gymcrm.exceptions.NotFoundException;
import io.gymcrm.metrics.GymMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Map;
import java.util.TreeMap;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private final GymMetrics metrics;

    public GlobalExceptionHandler(GymMetrics metrics) {
        this.metrics = metrics;
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthentication(AuthenticationException e) {
        return unauthorized(e.getMessage());
    }

    /** From Spring Security: no token, or a token that isn't valid (also see SecurityErrorHandler). */
    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleSecurityAuthentication(
            org.springframework.security.core.AuthenticationException e) {
        metrics.authenticationFailed("token");
        if (e instanceof OAuth2AuthenticationException) {
            return unauthorized("Invalid or expired token");
        }
        if (e instanceof InsufficientAuthenticationException
                || e instanceof AuthenticationCredentialsNotFoundException) {
            return unauthorized("Authentication required");
        }
        return unauthorized(e.getMessage());
    }

    /** From @PreAuthorize: logged in, but not allowed to do this. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException e) {
        return problem(HttpStatus.FORBIDDEN, "You are not allowed to do this");
    }

    @ExceptionHandler(LoginBlockedException.class)
    public ResponseEntity<ProblemDetail> handleLoginBlocked(LoginBlockedException e) {
        // Rounded up, so a client that waits exactly this long isn't refused again.
        long seconds = e.getRetryAfter().plusMillis(999).toSeconds();
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(seconds))
                .body(problemDetail(HttpStatus.TOO_MANY_REQUESTS, e.getMessage()));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(NotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ProblemDetail> handleConflict(ConflictException e) {
        return problem(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException e) {
        return problem(HttpStatus.BAD_REQUEST, e.getMessage());
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
            String message = error.isBindingFailure()
                    ? "invalid value '" + error.getRejectedValue() + "'"   // e.g. a date that isn't a date
                    : error.getDefaultMessage();
            errors.merge(error.getField(), message, (a, b) -> a + "; " + b);
        }
        ProblemDetail body = e.getBody();
        body.setDetail("Validation failed");
        body.setProperty("errors", errors);
        log.warn("Validation failed: {}", errors);
        return handleExceptionInternal(e, body, headers, status, request);
    }

    private static ResponseEntity<ProblemDetail> unauthorized(String detail) {
        // RFC 9110: a 401 must say which authentication scheme the client should use.
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .body(problemDetail(HttpStatus.UNAUTHORIZED, detail));
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
        return ResponseEntity.status(status).body(problemDetail(status, detail));
    }

    private static ProblemDetail problemDetail(HttpStatus status, String detail) {
        if (status.is4xxClientError()) {
            log.warn("{} {}: {}", status.value(), status.getReasonPhrase(), detail);
        }
        return ProblemDetail.forStatusAndDetail(status, detail);
    }
}
