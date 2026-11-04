package io.gymcrm.exceptions;

import java.time.Duration;

/**
 * Too many failed logins: the username is blocked for a while, even with the right password.
 */
public class LoginBlockedException extends RuntimeException {

    private final Duration retryAfter;

    public LoginBlockedException(Duration retryAfter) {
        super("Too many failed logins. Try again later");
        this.retryAfter = retryAfter;
    }

    public Duration getRetryAfter() {
        return retryAfter;
    }
}
