package support;

import io.gymcrm.security.Role;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * For tests that call services directly: the logged-in user, as if a token had been verified.
 */
public final class SecurityTestSupport {

    private SecurityTestSupport() {
    }

    public static void loginAs(String username, Role role) {
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken(username, null, "ROLE_" + role.name()));
    }

    public static void logout() {
        SecurityContextHolder.clearContext();
    }
}
