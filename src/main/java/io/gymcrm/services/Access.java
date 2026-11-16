package io.gymcrm.services;

import io.gymcrm.dto.Credentials;
import io.gymcrm.exceptions.ForbiddenException;
import lombok.extern.slf4j.Slf4j;

/**
 * Authorization rules shared by the services.
 */
@Slf4j
public final class Access {

    private Access() {
    }

    /**
     * Only the profile's owner may change it. Call after authentication, so that a mismatch means
     * "someone else's profile" (403), not wrong credentials (401).
     */
    public static void requireOwnProfile(Credentials auth, String username) {
        if (!auth.username().equals(username)) {
            log.warn("User {} tried to change the profile of {}", auth.username(), username);
            throw new ForbiddenException("You can only change your own profile");
        }
    }
}
