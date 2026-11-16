package io.gymcrm.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Brute-force protection: after too many failed logins in a row, a username is blocked for a while.
 * Unknown usernames are counted the same way, so the behaviour doesn't reveal which usernames exist.
 * Kept in memory: lost on restart and not shared between instances.
 */
@Slf4j
@Service
public class LoginAttemptService {

    private record Attempts(int failures, Instant lastFailure) {
    }

    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final int maxFailures;
    private final Duration blockDuration;
    private final Clock clock;

    public LoginAttemptService(@Value("${gymcrm.security.login.max-failures}") int maxFailures,
                               @Value("${gymcrm.security.login.block-duration}") Duration blockDuration,
                               Clock clock) {
        this.maxFailures = maxFailures;
        this.blockDuration = blockDuration;
        this.clock = clock;
    }

    /** How long the username stays blocked, or empty if it may try to log in. */
    public Optional<Duration> remainingBlock(String username) {
        Attempts current = attempts.get(username);
        if (current == null || current.failures() < maxFailures) {
            return Optional.empty();
        }
        Duration remaining = Duration.between(clock.instant(), current.lastFailure().plus(blockDuration));
        return remaining.isNegative() || remaining.isZero() ? Optional.empty() : Optional.of(remaining);
    }

    public void recordFailure(String username) {
        Instant now = clock.instant();
        // Entries older than the block duration are forgotten: a slow series of failures doesn't block,
        // and made-up usernames don't fill the memory.
        attempts.values().removeIf(entry -> entry.lastFailure().plus(blockDuration).isBefore(now));
        Attempts updated = attempts.merge(username, new Attempts(1, now),
                (old, ignored) -> new Attempts(old.failures() + 1, now));
        if (updated.failures() >= maxFailures) {
            log.warn("Username {} blocked for {} after {} failed logins", username, blockDuration, updated.failures());
        }
    }

    public void recordSuccess(String username) {
        attempts.remove(username);
    }

    /** Forgets everything; the block ends for all usernames. */
    public void clear() {
        attempts.clear();
    }
}
