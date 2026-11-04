package security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import io.gymcrm.security.JwtTokenService;
import io.gymcrm.security.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.ResultActions;
import support.WebTestBase;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Attacks and security rules through real HTTP requests, with the whole filter chain.
 */
class SecurityIntegrationTest extends WebTestBase {

    private static final String PROTECTED = "/api/trainees/John.Smith";

    @Autowired
    private JwtEncoder encoder;

    @Autowired
    private JwtDecoder decoder;

    @Autowired
    private DataSource dataSource;

    private ResultActions read(String authorization) throws Exception {
        return mvc.perform(get(PROTECTED).header(HttpHeaders.AUTHORIZATION, authorization));
    }

    // Which endpoints need a token

    @Test
    void everyProtectedEndpointRejectsARequestWithoutAToken() throws Exception {
        String empty = "{}";
        mvc.perform(get(PROTECTED)).andExpect(status().isUnauthorized());
        mvc.perform(put(PROTECTED).contentType(MediaType.APPLICATION_JSON).content(empty)).andExpect(status().isUnauthorized());
        mvc.perform(delete(PROTECTED)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/trainees/John.Smith/available-trainers")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/trainees/John.Smith/trainers").contentType(MediaType.APPLICATION_JSON).content(empty)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/trainees/John.Smith/trainings")).andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/trainees/John.Smith/status").contentType(MediaType.APPLICATION_JSON).content(empty)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/trainers/Anna.Lee")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/trainers/Anna.Lee").contentType(MediaType.APPLICATION_JSON).content(empty)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/trainers/Anna.Lee/trainings")).andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/trainers/Anna.Lee/status").contentType(MediaType.APPLICATION_JSON).content(empty)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/trainings").contentType(MediaType.APPLICATION_JSON).content(empty)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/training-types")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/auth/password").contentType(MediaType.APPLICATION_JSON).content(empty)).andExpect(status().isUnauthorized());
    }

    @Test
    void registrationAndLoginNeedNoToken() throws Exception {
        register("/api/trainees", """
                {"firstName":"New","lastName":"Trainee"}""");
        register("/api/trainers", """
                {"firstName":"New","lastName":"Trainer","specializationId":1}""");
        login("John.Smith", passwordOf(john)).andExpect(status().isOk());
    }

    @Test
    void monitoringEndpointsNeedNoToken() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(get("/actuator/info")).andExpect(status().isOk());
        mvc.perform(get("/actuator/prometheus")).andExpect(status().isOk());
    }

    @Test
    void errorsFromTheSecurityFiltersHaveTheNormalFormat() throws Exception {
        mvc.perform(get(PROTECTED))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Authentication required"))
                .andExpect(jsonPath("$.instance").value(PROTECTED));
    }

    // Tokens

    @Test
    void theTokenFromRegistrationAndFromLoginBothWork() throws Exception {
        read(john).andExpect(status().isOk());
        read(loginHeader("John.Smith", passwordOf(john))).andExpect(status().isOk());
    }

    @Test
    void theTokenCarriesTheUsernameAndRoleButNotThePassword() throws Exception {
        Jwt jwt = decoder.decode(john.substring("Bearer ".length()));

        assertEquals("John.Smith", jwt.getSubject());
        assertEquals(List.of("TRAINEE"), jwt.getClaimAsStringList(JwtTokenService.ROLES_CLAIM));
        assertEquals("gym-crm", jwt.getClaimAsString("iss"));
        assertTrue(jwt.getClaims().keySet().stream().noneMatch(claim -> claim.toLowerCase().contains("password")));
    }

    @Test
    void tamperedTokenIsRejected() throws Exception {
        // Swap Anna's payload into John's token: the signature no longer matches.
        String[] johnParts = john.substring("Bearer ".length()).split("\\.");
        String[] annaParts = anna.substring("Bearer ".length()).split("\\.");

        read("Bearer " + johnParts[0] + "." + annaParts[1] + "." + johnParts[2])
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid or expired token"));
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() throws Exception {
        SecretKey otherKey = new SecretKeySpec(
                "an-attackers-own-secret-of-sufficient-length".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JwtTokenService attacker = new JwtTokenService(new NimbusJwtEncoder(new ImmutableSecret<>(otherKey)),
                Duration.ofHours(1), Clock.systemUTC());

        read("Bearer " + attacker.issue("John.Smith", Role.TRAINEE).token()).andExpect(status().isUnauthorized());
    }

    @Test
    void expiredTokenIsRejected() throws Exception {
        Clock yesterday = Clock.fixed(Instant.now().minus(Duration.ofDays(1)), ZoneOffset.UTC);
        JwtTokenService issuedYesterday = new JwtTokenService(encoder, Duration.ofHours(1), yesterday);

        read("Bearer " + issuedYesterday.issue("John.Smith", Role.TRAINEE).token())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid or expired token"));
    }

    @Test
    void validTokensExpireAfterTheConfiguredLifetime() throws Exception {
        Jwt jwt = decoder.decode(john.substring("Bearer ".length()));

        assertEquals(Duration.ofHours(1), Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()));
    }

    // Authorization

    @Test
    void someoneElsesProfileIsForbidden() throws Exception {
        String maria = register("/api/trainees", """
                {"firstName":"Maria","lastName":"Garcia"}""");

        mvc.perform(delete(PROTECTED).header(HttpHeaders.AUTHORIZATION, maria))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("You are not allowed to do this"));
        read(john).andExpect(status().isOk());
    }

    @Test
    void wrongRoleIsForbiddenEvenOnTheOwnUsername() throws Exception {
        mvc.perform(delete("/api/trainees/Anna.Lee").header(HttpHeaders.AUTHORIZATION, anna))
                .andExpect(status().isForbidden());
        mvc.perform(json(patch("/api/trainers/John.Smith/status"), john, """
                        {"isActive":false}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    void someoneElsesPasswordCantBeChanged() throws Exception {
        mvc.perform(json(put("/api/auth/password"), anna, """
                        {"username":"John.Smith","oldPassword":"%s","newPassword":"hacked12"}""".formatted(passwordOf(john))))
                .andExpect(status().isForbidden());

        login("John.Smith", passwordOf(john)).andExpect(status().isOk());
    }

    // Password storage

    @Test
    void registrationResponseHasThePlainPasswordButTheDatabaseOnlyAHash() throws Exception {
        String stored = new JdbcTemplate(dataSource)
                .queryForObject("select password from users where username = 'John.Smith'", String.class);

        assertTrue(stored.startsWith("$2a$"), stored);
        assertTrue(!stored.contains(passwordOf(john)));
    }

    @Test
    void passwordsNeverAppearInProfiles() throws Exception {
        read(john)
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("password"))))
                .andExpect(content().string(not(containsString("$2a$"))));
    }

    // Brute force

    @Test
    void threeFailedLoginsBlockTheUserForFiveMinutes() throws Exception {
        for (int i = 0; i < 3; i++) {
            login("John.Smith", "wrong").andExpect(status().isUnauthorized());
        }

        // Even the right password is refused now.
        login("John.Smith", passwordOf(john))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "300"))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Too many failed logins. Try again later"));
    }

    @Test
    void twoFailuresDontBlockAndASuccessResetsTheCount() throws Exception {
        String password = passwordOf(john);
        login("John.Smith", "wrong").andExpect(status().isUnauthorized());
        login("John.Smith", "wrong").andExpect(status().isUnauthorized());
        login("John.Smith", password).andExpect(status().isOk());
        login("John.Smith", "wrong").andExpect(status().isUnauthorized());
        login("John.Smith", "wrong").andExpect(status().isUnauthorized());

        login("John.Smith", password).andExpect(status().isOk());
    }

    @Test
    void aBlockAffectsOnlyThatUser() throws Exception {
        for (int i = 0; i < 3; i++) {
            login("John.Smith", "wrong").andExpect(status().isUnauthorized());
        }

        login("Anna.Lee", passwordOf(anna)).andExpect(status().isOk());
    }

    @Test
    void aBlockedUsersExistingTokenKeepsWorking() throws Exception {
        for (int i = 0; i < 3; i++) {
            login("John.Smith", "wrong").andExpect(status().isUnauthorized());
        }

        read(john).andExpect(status().isOk());
    }

    @Test
    void unknownUsernamesBehaveLikeRealOnes() throws Exception {
        for (int i = 0; i < 3; i++) {
            login("Nobody.Here", "wrong").andExpect(status().isUnauthorized());
        }

        login("Nobody.Here", "wrong").andExpect(status().isTooManyRequests());
    }

    // Logout

    @Test
    void logoutRevokesTheToken() throws Exception {
        read(john).andExpect(status().isOk());

        mvc.perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, john))
                .andExpect(status().isOk());

        read(john)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid or expired token"));
    }

    @Test
    void logoutOnlyAffectsThatToken() throws Exception {
        String secondSession = loginHeader("John.Smith", passwordOf(john));

        mvc.perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, john)).andExpect(status().isOk());

        read(secondSession).andExpect(status().isOk());
        read(anna).andExpect(status().isOk());
    }

    @Test
    void afterLogoutTheUserCanLogInAgain() throws Exception {
        mvc.perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, john)).andExpect(status().isOk());

        read(loginHeader("John.Smith", passwordOf(john))).andExpect(status().isOk());
    }

    @Test
    void logoutNeedsAValidToken() throws Exception {
        mvc.perform(post("/api/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"));
        mvc.perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, "Bearer nonsense"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loggingOutTwiceFailsTheSecondTime() throws Exception {
        mvc.perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, john)).andExpect(status().isOk());

        mvc.perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, john))
                .andExpect(status().isUnauthorized());
    }

    // CORS

    @Test
    void preflightFromAnAllowedOrigin() throws Exception {
        mvc.perform(options("/api/trainees")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("POST")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsString("Authorization")));
    }

    @Test
    void preflightFromAnotherOriginIsRejected() throws Exception {
        mvc.perform(options("/api/trainees")
                        .header(HttpHeaders.ORIGIN, "http://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void actualRequestFromAnAllowedOriginGetsTheHeaders() throws Exception {
        mvc.perform(get(PROTECTED)
                        .header(HttpHeaders.AUTHORIZATION, john)
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, containsString("X-Transaction-Id")));
    }

    // Logging

    @Test
    void tokensAndPasswordsDontReachTheLogs() throws Exception {
        try (support.LogCapture logs = new support.LogCapture("io.gymcrm")) {
            String password = passwordOf(john);
            String token = loginHeader("John.Smith", password).substring("Bearer ".length());

            String all = String.join("\n", logs.messages());
            assertTrue(!all.contains(password), "password in the logs");
            assertTrue(!all.contains(token), "token in the logs");
            assertTrue(all.contains("\"token\":\"***\""), all);
        }
    }
}
