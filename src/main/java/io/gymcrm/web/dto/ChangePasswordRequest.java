package io.gymcrm.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank String username,
        @NotBlank String oldPassword,
        @NotBlank @Size(max = 255) String newPassword) {

    @Override
    public String toString() {
        return "ChangePasswordRequest[username=" + username + "]";
    }
}
