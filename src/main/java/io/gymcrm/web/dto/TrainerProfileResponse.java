package io.gymcrm.web.dto;

import java.util.List;

public record TrainerProfileResponse(String username, String firstName, String lastName,
                                     TrainingTypeResponse specialization, Boolean isActive,
                                     List<TraineeSummary> trainees) {
}
