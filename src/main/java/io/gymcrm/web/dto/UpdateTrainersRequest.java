package io.gymcrm.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * @param trainerUsernames the trainee's complete new list; an empty list removes all trainers
 */
public record UpdateTrainersRequest(@NotNull List<@NotBlank String> trainerUsernames) {
}
