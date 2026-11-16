package security;

import io.gymcrm.security.TokenRevocationService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenRevocationServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-04T10:00:00Z");

    private final TokenRevocationService revocations =
            new TokenRevocationService(Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void onlyTheRevokedTokenIsRevoked() {
        revocations.revoke("token-1", NOW.plus(Duration.ofHours(1)));

        assertTrue(revocations.isRevoked("token-1"));
        assertFalse(revocations.isRevoked("token-2"));
    }

    @Test
    void expiredEntriesAreRemovedSoTheListDoesNotGrowForever() {
        revocations.revoke("expired", NOW.minus(Duration.ofMinutes(1)));
        revocations.revoke("current", NOW.plus(Duration.ofHours(1)));

        assertFalse(revocations.isRevoked("expired"));
        assertTrue(revocations.isRevoked("current"));
    }
}
