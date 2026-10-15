package io.gymcrm.web.dto;

/**
 * Returned once, at registration: the only time the generated password is shown.
 */
public record CredentialsResponse(String username, String password) {

    @Override
    public String toString() {
        return "CredentialsResponse[username=" + username + "]";
    }
}
