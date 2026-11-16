package io.gymcrm.web.controller;

import io.gymcrm.dto.Credentials;
import io.gymcrm.facade.GymFacade;
import io.gymcrm.web.dto.ChangePasswordRequest;
import io.gymcrm.web.dto.LoginRequest;
import io.gymcrm.web.dto.TokenResponse;
import io.gymcrm.web.mapper.WebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Logging out is POST /api/auth/logout, handled by Spring Security (see SecurityConfig), not by a controller.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Login and password change. "
        + "POST /api/auth/logout with the Bearer token revokes that token.")
public class AuthController {

    private final GymFacade facade;

    public AuthController(GymFacade facade) {
        this.facade = facade;
    }

    /** Login: returns the token to send on all later requests. */
    @PostMapping("/login")
    @Operation(summary = "Log in",
            description = "Checks the username and password and returns a JWT. After 3 failed attempts the "
                    + "username is blocked for 5 minutes, even with the right password.")
    @SecurityRequirements
    @ApiResponse(responseCode = "200", description = "The token")
    @ApiResponse(responseCode = "400", description = "Username or password is missing")
    @ApiResponse(responseCode = "401", description = "Wrong username or password")
    @ApiResponse(responseCode = "429", description = "Blocked after too many failed logins; see Retry-After")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return WebMapper.toTokenResponse(facade.login(new Credentials(request.username(), request.password())));
    }

    /** Change login: needs the token and the old password. */
    @PutMapping("/password")
    @Operation(summary = "Change password",
            description = "Works for trainees and trainers, only on the logged-in user's own account. "
                    + "The old password is checked as well.")
    @ApiResponse(responseCode = "200", description = "Password changed")
    @ApiResponse(responseCode = "400", description = "A field is missing or blank")
    @ApiResponse(responseCode = "401", description = "Missing, invalid or expired token, or wrong old password")
    @ApiResponse(responseCode = "403", description = "Someone else's account")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        facade.changePassword(request.username(), request.oldPassword(), request.newPassword());
        return ResponseEntity.ok().build();
    }
}
