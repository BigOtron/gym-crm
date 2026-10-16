package io.gymcrm.web.dto;

import jakarta.validation.constraints.NotNull;

public record ActiveStatusRequest(@NotNull Boolean isActive) {
}
