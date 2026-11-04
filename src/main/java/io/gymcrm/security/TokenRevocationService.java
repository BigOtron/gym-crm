package io.gymcrm.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The tokens of users who logged out. A JWT is valid on its own until it expires, so logout works by
 * remembering its id until then. Kept in memory: lost on restart and not shared between instances
 * (a shared store such as Redis would be needed for several instances).
 */
@Slf4j
@Service
public class TokenRevocationService {

    private final Map<String, Instant> revoked = new ConcurrentHashMap<>();
    private final Clock clock;

    public TokenRevocationService(Clock clock) {
        this.clock = clock;
    }

    public void revoke(String tokenId, Instant expiresAt) {
        // An expired token is rejected anyway, so its entry is no longer needed.
        revoked.values().removeIf(expiry -> expiry.isBefore(clock.instant()));
        revoked.put(tokenId, expiresAt);
        log.debug("Token {} revoked until {}", tokenId, expiresAt);
    }

    public boolean isRevoked(String tokenId) {
        return revoked.containsKey(tokenId);
    }
}
