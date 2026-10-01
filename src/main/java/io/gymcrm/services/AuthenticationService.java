package io.gymcrm.services;

import io.gymcrm.dto.Credentials;

public interface AuthenticationService {
    boolean traineeCredentialsMatch(Credentials credentials);
    boolean trainerCredentialsMatch(Credentials credentials);

    void authenticateTrainee(Credentials credentials);

    void authenticateTrainer(Credentials credentials);

    void authenticate(Credentials credentials);
}