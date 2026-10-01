package io.gymcrm.dto;

import java.time.LocalDate;

public record TraineeRegistration(String firstName, String lastName, LocalDate dateOfBirth, String address) {
}
