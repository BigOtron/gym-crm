package io.gymcrm.dto;

import java.time.LocalDate;

public record TraineeUpdate(String firstName, String lastName, LocalDate dateOfBirth, String address) {
}
