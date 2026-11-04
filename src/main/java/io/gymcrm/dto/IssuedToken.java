package io.gymcrm.dto;

/**
 * A signed JWT and how many seconds it stays valid.
 */
public record IssuedToken(String token, long expiresInSeconds) {

    @Override
    public String toString() {
        return "IssuedToken[expiresInSeconds=" + expiresInSeconds + "]";
    }
}
