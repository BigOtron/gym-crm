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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Trainers", description = "Trainer profiles and their trainings")
public class TrainerController {

    private final GymFacade facade;

    public TrainerController(GymFacade facade) {
        this.facade = facade;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a trainer",
            description = "Generates the username (firstName.lastName, plus a number if taken) and a random "
                    + "10-character password. This response is the only time the password is shown. "
                    + "The specialization is a training type id from GET /api/training-types.")
    @SecurityRequirements
    @ApiResponse(responseCode = "201", description = "Trainer registered; returns the generated credentials")
    @ApiResponse(responseCode = "400", description = "A required field is missing or a field is invalid")
    @ApiResponse(responseCode = "404", description = "No training type with this specializationId")
    public CredentialsResponse register(@Valid @RequestBody TrainerRegistrationRequest request) {
        return WebMapper.toCredentials(facade.createTrainer(WebMapper.toRegistration(request)).getUser());
    }

    @GetMapping("/{username}")
    @Operation(summary = "Get a trainer's profile", description = "Any authenticated user can read it.")
    @ApiResponse(responseCode = "200", description = "The profile, with the trainer's trainees")
    @ApiResponse(responseCode = "401", description = "Missing or wrong credentials")
    @ApiResponse(responseCode = "404", description = "No trainer with this username")
    public TrainerProfileResponse getProfile(@Parameter(hidden = true) @BasicAuth Credentials auth,
                                             @Parameter(description = "Trainer's username", example = "Anna.Lee")
                                             @PathVariable String username) {
        return WebMapper.toProfile(facade.getTrainerByUsername(auth, username));
    }

    @PutMapping("/{username}")
    @Operation(summary = "Update a trainer's profile",
            description = "Only the trainer can update their own profile. The username can't be changed, and "
                    + "the specialization is read-only: a specializationId in the request is ignored.")
    @ApiResponse(responseCode = "200", description = "The updated profile")
    @ApiResponse(responseCode = "400", description = "A required field is missing or a field is invalid")
    @ApiResponse(responseCode = "401", description = "Missing or wrong credentials, or not a trainer")
    @ApiResponse(responseCode = "403", description = "Someone else's profile")
    @ApiResponse(responseCode = "404", description = "No trainer with this username")
    public TrainerProfileResponse update(@Parameter(hidden = true) @BasicAuth Credentials auth,
                                         @Parameter(description = "Trainer's username", example = "Anna.Lee")
                                         @PathVariable String username,
                                         @Valid @RequestBody TrainerUpdateRequest request) {
        return WebMapper.toProfile(facade.updateTrainer(auth, username, WebMapper.toUpdate(request)));
    }

    @GetMapping("/{username}/trainings")
    @Operation(summary = "List a trainer's trainings",
            description = "Sorted by date. All filters are optional and can be combined.")
    @ApiResponse(responseCode = "200", description = "The matching trainings")
    @ApiResponse(responseCode = "400", description = "Invalid filter, e.g. 'from' after 'to' or a bad date")
    @ApiResponse(responseCode = "401", description = "Missing or wrong credentials")
    @ApiResponse(responseCode = "404", description = "No trainer with this username")
    public List<TrainerTrainingResponse> getTrainings(@Parameter(hidden = true) @BasicAuth Credentials auth,
                                                      @Parameter(description = "Trainer's username",
                                                              example = "Anna.Lee")
                                                      @PathVariable String username,
                                                      @Valid TrainerTrainingsQuery query) {
        return WebMapper.toTrainerTrainings(facade.getTrainerTrainings(auth, username, WebMapper.toCriteria(query)));
    }

    @PatchMapping("/{username}/status")
    @Operation(summary = "Activate or de-activate a trainer",
            description = "Not idempotent: setting the state the trainer is already in returns 409. "
                    + "Only the trainer can change their own status.")
    @ApiResponse(responseCode = "200", description = "Status changed")
    @ApiResponse(responseCode = "400", description = "isActive is missing")
    @ApiResponse(responseCode = "401", description = "Missing or wrong credentials, or not a trainer")
    @ApiResponse(responseCode = "403", description = "Someone else's profile")
    @ApiResponse(responseCode = "404", description = "No trainer with this username")
    @ApiResponse(responseCode = "409", description = "Already in the requested state")
    public ResponseEntity<Void> setActive(@Parameter(hidden = true) @BasicAuth Credentials auth,
                                          @Parameter(description = "Trainer's username", example = "Anna.Lee")
                                          @PathVariable String username,
                                          @Valid @RequestBody ActiveStatusRequest request) {
        facade.setTrainerActive(auth, username, request.isActive());
        return ResponseEntity.ok().build();
    }
}
