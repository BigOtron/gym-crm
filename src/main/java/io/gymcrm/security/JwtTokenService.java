package io.gymcrm.security;

import io.gymcrm.dto.IssuedToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Issues the signed tokens. Spring Security's resource server support verifies them on later requests.
 */
@Service
public class JwtTokenService {

    public static final String ISSUER = "gym-crm";
    public static final String ROLES_CLAIM = "roles";

    private final JwtEncoder encoder;
    private final Duration lifetime;
    private final Clock clock;

    @Autowired
    public JwtTokenService(JwtEncoder encoder,
                           @Value("${gymcrm.security.jwt.lifetime}") Duration lifetime,
                           Clock clock) {
        this.encoder = encoder;
        this.lifetime = lifetime;
        this.clock = clock;
    }

    public IssuedToken issue(String username, Role role) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .id(UUID.randomUUID().toString())   // "jti": identifies this token, so logout can revoke it
                .issuer(ISSUER)
                .subject(username)
                .issuedAt(now)
                .expiresAt(now.plus(lifetime))
                .claim(ROLES_CLAIM, List.of(role.name()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(token, lifetime.toSeconds());
    }
}
