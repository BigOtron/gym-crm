package security;

import io.gymcrm.security.LoginAttemptService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginAttemptServiceTest {

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-11-04T10:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private final MutableClock clock = new MutableClock();
    private final LoginAttemptService attempts = new LoginAttemptService(3, Duration.ofMinutes(5), clock);

    private void fail(int times) {
        for (int i = 0; i < times; i++) {
            attempts.recordFailure("John.Smith");
        }
    }

    @Test
    void twoFailuresDoNotBlock() {
        fail(2);

        assertFalse(attempts.remainingBlock("John.Smith").isPresent());
    }

    @Test
    void theThirdFailureBlocksForFiveMinutes() {
        fail(3);

        assertEquals(Optional.of(Duration.ofMinutes(5)), attempts.remainingBlock("John.Smith"));
    }

    @Test
    void theBlockCountsDown() {
        fail(3);
        clock.advance(Duration.ofMinutes(4));

        assertEquals(Optional.of(Duration.ofMinutes(1)), attempts.remainingBlock("John.Smith"));
    }

    @Test
    void theBlockEndsAfterFiveMinutes() {
        fail(3);
        clock.advance(Duration.ofMinutes(5));

        assertTrue(attempts.remainingBlock("John.Smith").isEmpty());
    }

    @Test
    void successResetsTheCount() {
        fail(2);
        attempts.recordSuccess("John.Smith");
        fail(2);

        assertTrue(attempts.remainingBlock("John.Smith").isEmpty());
    }

    @Test
    void oldFailuresAreForgotten() {
        fail(2);
        clock.advance(Duration.ofMinutes(6));
        fail(1);

        assertTrue(attempts.remainingBlock("John.Smith").isEmpty(), "failures spread over time don't add up");
    }

    @Test
    void afterTheBlockEndsANewSeriesStartsFromZero() {
        fail(3);
        clock.advance(Duration.ofMinutes(6));
        fail(1);

        assertTrue(attempts.remainingBlock("John.Smith").isEmpty());
    }

    @Test
    void usernamesAreCountedSeparately() {
        fail(3);

        assertTrue(attempts.remainingBlock("Anna.Lee").isEmpty());
    }

    @Test
    void clearEndsEveryBlock() {
        fail(3);
        attempts.clear();

        assertTrue(attempts.remainingBlock("John.Smith").isEmpty());
    }
}
