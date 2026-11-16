package io.gymcrm.dto;

/**
 * A newly registered trainee or trainer, with the generated password and a token for immediate use.
 * Only a hash of the password is stored, so this is the one moment the plain password can be shown.
 */
public record CreatedProfile<T>(T profile, String password, IssuedToken token) {

    @Override
    public String toString() {
        return "CreatedProfile[profile=" + profile + "]";
    }
}
