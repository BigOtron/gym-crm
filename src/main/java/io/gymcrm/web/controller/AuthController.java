package io.gymcrm.web.controller;

import io.gymcrm.dto.Credentials;
import io.gymcrm.facade.GymFacade;
import io.gymcrm.web.auth.BasicAuth;
import io.gymcrm.web.dto.ChangePasswordRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Login and password change")
public class AuthController {

    private final GymFacade facade;

    public AuthController(GymFacade facade) {
        this.facade = facade;
    }

    @GetMapping("/login")
    @Operation(summary = "Log in",
            description = "Checks the username and password sent in the Basic Authorization header.")
    @ApiResponse(responseCode = "200", description = "Credentials are valid")
    @ApiResponse(responseCode = "401", description = "Missing or wrong credentials")
    public ResponseEntity<Void> login(@Parameter(hidden = true) @BasicAuth Credentials auth) {
        facade.login(auth);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/password")
    @Operation(summary = "Change password",
            description = "Works for trainees and trainers. The old password in the body authenticates "
                    + "the call, so no Authorization header is needed.")
    @SecurityRequirements
    @ApiResponse(responseCode = "200", description = "Password changed")
    @ApiResponse(responseCode = "400", description = "A field is missing or blank")
    @ApiResponse(responseCode = "401", description = "Wrong username or old password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        facade.changePassword(new Credentials(request.username(), request.oldPassword()), request.newPassword());
        return ResponseEntity.ok().build();
    }
}
