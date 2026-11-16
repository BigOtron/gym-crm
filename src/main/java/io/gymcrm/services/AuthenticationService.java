package io.gymcrm.services;

import io.gymcrm.dto.Credentials;
import io.gymcrm.dto.IssuedToken;

public interface AuthenticationService {

    /**
     * Checks the username and password and issues a token.
     *
     * @throws io.gymcrm.exceptions.AuthenticationException  if they don't match
     * @throws io.gymcrm.exceptions.LoginBlockedException    after too many failed logins
     */
    IssuedToken login(Credentials credentials);

    /**
     * Changes the logged-in user's own password; works for trainees and trainers.
     *
     * @throws io.gymcrm.exceptions.AuthenticationException if the old password is wrong
     */
    void changePassword(String username, String oldPassword, String newPassword);
}
