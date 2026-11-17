package io.gymcrm.workload.service;

public class TrainerNotFoundException extends RuntimeException {

    public TrainerNotFoundException(String username) {
        super("No workload found for trainer " + username);
    }
}
