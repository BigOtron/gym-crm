package io.gymcrm.exceptions;

/** Another service we depend on is down, too slow, or its circuit breaker is open (HTTP 503). */
public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
