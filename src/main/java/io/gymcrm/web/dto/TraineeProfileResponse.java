package io.gymcrm.web.dto;

import java.time.LocalDate;
import java.util.List;

public record TraineeProfileResponse(String username, String firstName, String lastName,
                                     LocalDate dateOfBirth, String address, Boolean isActive,
                                     List<TrainerSummary> trainers) {
}
