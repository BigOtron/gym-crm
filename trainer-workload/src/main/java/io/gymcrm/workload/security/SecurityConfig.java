package io.gymcrm.workload.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Only Gym CRM may call this service's REST API. It signs a short-lived service token with a secret both
 * services share (HMAC-SHA256). The token must come from Gym CRM (issuer), be meant for this service
 * (audience), and carry the workload.read scope. Trainings are not reported over REST any more but through
 * the message queue, which is protected by the broker's own login.
 */
@Configuration
public class SecurityConfig {

    public static final String ISSUER = "gym-crm";
    public static final String AUDIENCE = "trainer-workload";
    public static final String READ_SCOPE = "workload.read";

    private static final int MIN_SECRET_BYTES = 32;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityErrorHandler errors) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        // Docker and Eureka check these; restrict them at the network level on real servers.
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/error").permitAll()
                        // Spring Security turns each scope in the token into a "SCOPE_..." authority.
                        .requestMatchers(HttpMethod.GET, "/api/trainer-workloads/**").hasAuthority("SCOPE_" + READ_SCOPE)
                        .anyRequest().denyAll())
                .oauth2ResourceServer(server -> server
                        .jwt(jwt -> { })
                        .authenticationEntryPoint(errors)
                        .accessDeniedHandler(errors))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(errors)
                        .accessDeniedHandler(errors))
                .build();
    }

    /** Checks the signature, the expiry, the issuer and the audience. */
    @Bean
    public JwtDecoder jwtDecoder(@Value("${workload.security.jwt.secret}") String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("workload.security.jwt.secret must be at least " + MIN_SECRET_BYTES
                    + " bytes long for HS256");
        }
        SecretKey key = new SecretKeySpec(bytes, "HmacSHA256");
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        JwtClaimValidator<List<String>> audience = new JwtClaimValidator<>(JwtClaimNames.AUD,
                aud -> aud != null && aud.contains(AUDIENCE));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(ISSUER), audience));
        return decoder;
    }
}
