package io.gymcrm.dto;

public record CreatedProfile<T>(T profile, String password) {

    @Override
    public String toString() {
        return "CreatedProfile[profile=" + profile + "]";
    }
}
