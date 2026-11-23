package io.gymcrm.exceptions;

/**
 * Another service we depend on is up but rejected our request (HTTP 502). Unlike
 * {@link ServiceUnavailableException}, retrying won't help: something is misconfigured, e.g. the two services
 * don't share the same token secret.
 */
public class BadGatewayException extends RuntimeException {

    public BadGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
