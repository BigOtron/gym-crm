package io.gymcrm.integration.workload;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Creates the service token Gym CRM sends to the Trainer Workload service. It is signed with its own
 * secret, not the one for user tokens: the workload service can check these tokens, but can't use that
 * secret to create tokens that log in to Gym CRM as a user.
 */
public class WorkloadTokenProvider {

    static final String ISSUER = "gym-crm";
    static final String AUDIENCE = "trainer-workload";
    /** Only reads go over REST now; changes go through the message queue. */
    static final String SCOPES = "workload.read";

    private static final int MIN_SECRET_BYTES = 32;

    private final JwtEncoder encoder;
    private final Duration lifetime;
    private final Clock clock;

    public WorkloadTokenProvider(String secret, Duration lifetime, Clock clock) {
        byte[] bytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("gymcrm.workload.jwt.secret must be at least " + MIN_SECRET_BYTES
                    + " bytes long for HS256");
        }
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(bytes, "HmacSHA256")));
        this.lifetime = lifetime;
        this.clock = clock;
    }

    /** A new short-lived token for the next call. */
    public String token() {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(ISSUER)
                .audience(List.of(AUDIENCE))
                .issuedAt(now)
                .expiresAt(now.plus(lifetime))
                .claim("scope", SCOPES)
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
