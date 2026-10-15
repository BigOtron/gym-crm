package io.gymcrm.web.controller;

import io.gymcrm.dto.Credentials;
import io.gymcrm.facade.GymFacade;
import io.gymcrm.web.auth.BasicAuth;
import io.gymcrm.web.dto.ActiveStatusRequest;
import io.gymcrm.web.dto.CredentialsResponse;
import io.gymcrm.web.dto.TrainerProfileResponse;
import io.gymcrm.web.dto.TrainerRegistrationRequest;
import io.gymcrm.web.dto.TrainerTrainingResponse;
import io.gymcrm.web.dto.TrainerTrainingsQuery;
import io.gymcrm.web.dto.TrainerUpdateRequest;
import io.gymcrm.web.mapper.WebMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/trainers")
public class TrainerController {

    private final GymFacade facade;

    public TrainerController(GymFacade facade) {
        this.facade = facade;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CredentialsResponse register(@Valid @RequestBody TrainerRegistrationRequest request) {
        return WebMapper.toCredentials(facade.createTrainer(WebMapper.toRegistration(request)).getUser());
    }


    @GetMapping("/{username}")
    public TrainerProfileResponse getProfile(@BasicAuth Credentials auth, @PathVariable String username) {
        return WebMapper.toProfile(facade.getTrainerByUsername(auth, username));
    }

    @PutMapping("/{username}")
    public TrainerProfileResponse update(@BasicAuth Credentials auth, @PathVariable String username,
                                         @Valid @RequestBody TrainerUpdateRequest request) {
        return WebMapper.toProfile(facade.updateTrainer(auth, username, WebMapper.toUpdate(request)));
    }

    @GetMapping("/{username}/trainings")
    public List<TrainerTrainingResponse> getTrainings(@BasicAuth Credentials auth, @PathVariable String username,
                                                      @Valid TrainerTrainingsQuery query) {
        return WebMapper.toTrainerTrainings(facade.getTrainerTrainings(auth, username, WebMapper.toCriteria(query)));
    }

    @PatchMapping("/{username}/status")
    public ResponseEntity<Void> setActive(@BasicAuth Credentials auth, @PathVariable String username,
                                          @Valid @RequestBody ActiveStatusRequest request) {
        facade.setTrainerActive(auth, username, request.isActive());
        return ResponseEntity.ok().build();
    }
}
