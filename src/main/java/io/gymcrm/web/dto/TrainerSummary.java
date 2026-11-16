package io.gymcrm.web.dto;

public record TrainerSummary(String username, String firstName, String lastName,
                             TrainingTypeResponse specialization) {
}
