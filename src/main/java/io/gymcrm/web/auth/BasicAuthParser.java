package io.gymcrm.web.auth;

import io.gymcrm.dto.Credentials;
import io.gymcrm.exceptions.AuthenticationException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Parses an HTTP Basic {@code Authorization} header: {@code "Basic " + base64(username + ":" + password)}.
 * Only the format is checked here; whether the credentials are correct is decided by the facade.
 */
public final class BasicAuthParser {

    private static final String PREFIX = "Basic ";

    private BasicAuthParser() {
    }

    public static Credentials parse(String header) {
        if (header == null || header.isBlank()) {
            throw new AuthenticationException("Authentication required");
        }
        if (!header.regionMatches(true, 0, PREFIX, 0, PREFIX.length())) {
            throw new AuthenticationException("Only Basic authentication is supported");
        }

        String decoded;
        try {
            byte[] bytes = Base64.getDecoder().decode(header.substring(PREFIX.length()).trim());
            decoded = new String(bytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new AuthenticationException("Malformed Basic authentication header");
        }

        int colon = decoded.indexOf(':');
        if (colon <= 0) {
            throw new AuthenticationException("Malformed Basic authentication header");
        }
        return new Credentials(decoded.substring(0, colon), decoded.substring(colon + 1));
    }
}
