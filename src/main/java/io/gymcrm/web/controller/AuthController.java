package io.gymcrm.web.controller;

import io.gymcrm.dto.Credentials;
import io.gymcrm.facade.GymFacade;
import io.gymcrm.web.auth.BasicAuth;
import io.gymcrm.web.dto.ChangePasswordRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final GymFacade facade;

    public AuthController(GymFacade facade) {
        this.facade = facade;
    }

    @GetMapping("/login")
    public ResponseEntity<Void> login(@BasicAuth Credentials auth) {
        facade.login(auth);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        facade.changePassword(new Credentials(request.username(), request.oldPassword()), request.newPassword());
        return ResponseEntity.ok().build();
    }
}
