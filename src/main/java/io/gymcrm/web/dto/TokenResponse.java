package io.gymcrm.web.dto;

/**
 * @param token     send it on later requests as {@code Authorization: Bearer <token>}
 * @param expiresIn seconds until the token expires
 */
public record TokenResponse(String token, String tokenType, long expiresIn) {

    @Override
    public String toString() {
        return "TokenResponse[tokenType=" + tokenType + ", expiresIn=" + expiresIn + "]";
    }
}
