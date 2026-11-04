package io.gymcrm.web.dto;

/**
 * Returned once, at registration: the only time the generated password is shown.
 *
 * @param token a token for immediate use, the same as one from logging in
 */
public record CredentialsResponse(String username, String password, String token) {

    @Override
    public String toString() {
        return "CredentialsResponse[username=" + username + "]";
    }
}
