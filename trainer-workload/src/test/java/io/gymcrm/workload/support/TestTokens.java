package io.gymcrm.workload.support;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

/** Creates tokens the way Gym CRM does, so the tests can also create wrong ones. */
public final class TestTokens {

    public static final String SECRET = "test-only-workload-secret-with-32-bytes-at-least";

    private TestTokens() {
    }

    /** "Bearer ..." for a valid Gym CRM service token with these scopes. */
    public static String bearer(String... scopes) {
        return bearer(SECRET, claims -> claims.claim("scope", String.join(" ", scopes)));
    }

    /** "Bearer ..." for a token signed with {@code secret}, starting from valid claims changed by {@code changes}. */
    public static String bearer(String secret, Consumer<JwtClaimsSet.Builder> changes) {
        Instant now = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer("gym-crm")
                .subject("gym-crm")
                .audience(List.of("trainer-workload"))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .claim("scope", "workload.write workload.read");
        changes.accept(claims);
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(
                new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256")));
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
                claims.build())).getTokenValue();
        return "Bearer " + token;
    }
}
