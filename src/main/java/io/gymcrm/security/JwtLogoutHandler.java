package io.gymcrm.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * POST /api/auth/logout. Spring Security's LogoutFilter runs before the token is authenticated, so this
 * reads the token itself. A valid token is revoked and the answer is 200; otherwise 401.
 */
@Slf4j
@Component
public class JwtLogoutHandler implements LogoutHandler, LogoutSuccessHandler {

    private static final String REVOKED = JwtLogoutHandler.class.getName() + ".revoked";

    private final BearerTokenResolver tokenResolver = new DefaultBearerTokenResolver();
    private final JwtDecoder decoder;
    private final TokenRevocationService revocations;
    private final SecurityErrorHandler errors;

    public JwtLogoutHandler(JwtDecoder decoder, TokenRevocationService revocations, SecurityErrorHandler errors) {
        this.decoder = decoder;
        this.revocations = revocations;
        this.errors = errors;
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication ignored) {
        try {
            String token = tokenResolver.resolve(request);
            if (token != null) {
                Jwt jwt = decoder.decode(token);
                revocations.revoke(jwt.getId(), jwt.getExpiresAt());
                request.setAttribute(REVOKED, true);
                log.info("User {} logged out", jwt.getSubject());
            }
        } catch (JwtException | OAuth2AuthenticationException e) {
            log.debug("Logout with an invalid token: {}", e.getMessage());
        }
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication ignored)
            throws IOException, ServletException {
        if (request.getAttribute(REVOKED) != null) {
            response.setStatus(HttpStatus.OK.value());
        } else {
            errors.commence(request, response, new BadCredentialsException("A valid token is required to log out"));
        }
    }
}
