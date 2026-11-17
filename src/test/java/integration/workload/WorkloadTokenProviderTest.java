package integration.workload;

import io.gymcrm.integration.workload.WorkloadTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WorkloadTokenProviderTest {

    private static final String SECRET = "test-only-workload-secret-with-32-bytes-at-least";
    // JWT times have whole seconds; "now" so the decoder doesn't reject the token as expired.
    private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.SECONDS);

    @Test
    void createsAShortLivedServiceToken() {
        WorkloadTokenProvider provider = new WorkloadTokenProvider(SECRET, Duration.ofMinutes(5),
                Clock.fixed(NOW, ZoneOffset.UTC));

        Jwt jwt = NimbusJwtDecoder.withSecretKey(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256).build()
                .decode(provider.token());

        assertEquals("gym-crm", jwt.getClaimAsString("iss"));
        assertEquals("gym-crm", jwt.getSubject());
        assertEquals(List.of("trainer-workload"), jwt.getAudience());
        assertEquals("workload.write workload.read", jwt.getClaimAsString("scope"));
        assertEquals(NOW, jwt.getIssuedAt());
        assertEquals(NOW.plus(Duration.ofMinutes(5)), jwt.getExpiresAt());
    }

    @Test
    void rejectsShortOrMissingSecrets() {
        Clock clock = Clock.systemUTC();
        assertThrows(IllegalStateException.class, () -> new WorkloadTokenProvider("too-short", Duration.ofMinutes(5), clock));
        assertThrows(IllegalStateException.class, () -> new WorkloadTokenProvider(null, Duration.ofMinutes(5), clock));
    }
}
