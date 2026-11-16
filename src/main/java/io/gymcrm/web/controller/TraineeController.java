package io.gymcrm.web.controller;

import io.gymcrm.dto.Credentials;
import io.gymcrm.facade.GymFacade;
import io.gymcrm.web.auth.BasicAuth;
import io.gymcrm.web.dto.ActiveStatusRequest;
import io.gymcrm.web.dto.CredentialsResponse;
import io.gymcrm.web.dto.TraineeProfileResponse;
import io.gymcrm.web.dto.TraineeRegistrationRequest;
import io.gymcrm.web.dto.TraineeTrainingResponse;
import io.gymcrm.web.dto.TraineeTrainingsQuery;
import io.gymcrm.web.dto.TraineeUpdateRequest;
import io.gymcrm.web.dto.TrainerSummary;
import io.gymcrm.web.dto.UpdateTrainersRequest;
import io.gymcrm.web.mapper.WebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/trainees")
@Tag(name = "Trainees", description = "Trainee profiles, their trainers and their trainings")
public class TraineeController {

    private final GymFacade facade;

    public TraineeController(GymFacade facade) {
        this.facade = facade;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a trainee",
            description = "Generates the username (firstName.lastName, plus a number if taken) and a random "
                    + "10-character password. This response is the only time the password is shown.")
    @SecurityRequirements
    @ApiResponse(responseCode = "201", description = "Trainee registered; returns the generated credentials")
    @ApiResponse(responseCode = "400", description = "A required field is missing or a field is invalid")
    public CredentialsResponse register(@Valid @RequestBody TraineeRegistrationRequest request) {
        return WebMapper.toCredentials(facade.createTrainee(WebMapper.toRegistration(request)).getUser());
    }

    @GetMapping("/{username}")
    @Operation(summary = "Get a trainee's profile", description = "Any authenticated user can read it.")
    @ApiResponse(responseCode = "200", description = "The profile, with the trainee's trainers")
    @ApiResponse(responseCode = "401", description = "Missing or wrong credentials")
    @ApiResponse(responseCode = "404", description = "No trainee with this username")
    public TraineeProfileResponse getProfile(@Parameter(hidden = true) @BasicAuth Credentials auth,
                                             @Parameter(description = "Trainee's username", example = "John.Smith")
                                             @PathVariable String username) {
        return WebMapper.toProfile(facade.getTraineeByUsername(auth, username));
    }

    @PutMapping("/{username}")
    @Operation(summary = "Update a trainee's profile",
            description = "Only the trainee can update their own profile. The username can't be changed.")
    @ApiResponse(responseCode = "200", description = "The updated profile")
    @ApiResponse(responseCode = "400", description = "A required field is missing or a field is invalid")
    @ApiResponse(responseCode = "401", description = "Missing or wrong credentials, or not a trainee")
    @ApiResponse(responseCode = "403", description = "Someone else's profile")
    @ApiResponse(responseCode = "404", description = "No trainee with this username")
    public TraineeProfileResponse update(@Parameter(hidden = true) @BasicAuth Credentials auth,
                                         @Parameter(description = "Trainee's username", example = "John.Smith")
                                         @PathVariable String username,
                                         @Valid @RequestBody TraineeUpdateRequest request) {
        return WebMapper.toProfile(facade.updateTrainee(auth, username, WebMapper.toUpdate(request)));
    }

    @DeleteMapping("/{username}")
    @Operation(summary = "Delete a trainee's profile",
            description = "Only the trainee can delete their own profile. Their trainings are deleted too.")
    @ApiResponse(responseCode = "200", description = "Profile and trainings deleted")
    @ApiResponse(responseCode = "401", description = "Missing or wrong credentials, or not a trainee")
    @ApiResponse(responseCode = "403", description = "Someone else's profile")
    @ApiResponse(responseCode = "404", description = "No trainee with this username")
    public ResponseEntity<Void> delete(@Parameter(hidden = true) @BasicAuth Credentials auth,
                                       @Parameter(description = "Trainee's username", example = "John.Smith")
                                       @PathVariable String username) {
        facade.deleteTrainee(auth, username);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{username}/available-trainers")
    @Operation(summary = "List trainers the trainee could add",
            description = "Active trainers that are not in the trainee's trainers list, sorted by username.")
    @ApiResponse(responseCode = "200", description = "The trainers")
    @ApiResponse(responseCode = "401", description = "Missing or wrong credentials")
    @ApiResponse(responseCode = "404", description = "No trainee with this username")
    public List<TrainerSummary> getAvailableTrainers(@Parameter(hidden = true) @BasicAuth Credentials auth,
                                                     @Parameter(description = "Trainee's username",
                                                             example = "John.Smith")
                                                     @PathVariable String username) {
        return WebMapper.toTrainerSummaries(facade.getTrainersNotAssignedToTrainee(auth, username));
    }

    @PutMapping("/{username}/trainers")
    @Operation(summary = "Replace a trainee's trainers list",
            description = "The list in the request becomes the complete new list; an empty list removes "
                    + "all trainers. Past trainings are kept. Only the trainee can change their own list.")
    @ApiResponse(responseCode = "200", description = "The new trainers list")
    @ApiResponse(responseCode = "400", description = "The list is missing or contains a blank username")
    @ApiResponse(responseCode = "401", description = "Missing or wrong credentials, or not a trainee")
    @ApiResponse(responseCode = "403", description = "Someone else's list")
    @ApiResponse(responseCode = "404", description = "The trainee or one of the trainers doesn't exist")
    public List<TrainerSummary> updateTrainers(@Parameter(hidden = true) @BasicAuth Credentials auth,
                                               @Parameter(description = "Trainee's username", example = "John.Smith")
                                               @PathVariable String username,
                                               @Valid @RequestBody UpdateTrainersRequest request) {
        return WebMapper.toTrainerSummaries(facade.updateTraineeTrainers(auth, username, request.trainerUsernames()));
    }

    @GetMapping("/{username}/trainings")
    @Operation(summary = "List a trainee's trainings",
            description = "Sorted by date. All filters are optional and can be combined.")
    @ApiResponse(responseCode = "200", description = "The matching trainings")
    @ApiResponse(responseCode = "400", description = "Invalid filter, e.g. 'from' after 'to' or a bad date")
    @ApiResponse(responseCode = "401", description = "Missing or wrong credentials")
    @ApiResponse(responseCode = "404", description = "No trainee with this username")
    public List<TraineeTrainingResponse> getTrainings(@Parameter(hidden = true) @BasicAuth Credentials auth,
                                                      @Parameter(description = "Trainee's username",
                                                              example = "John.Smith")
                                                      @PathVariable String username,
                                                      @Valid TraineeTrainingsQuery query) {
        return WebMapper.toTraineeTrainings(facade.getTraineeTrainings(auth, username, WebMapper.toCriteria(query)));
    }

    @PatchMapping("/{username}/status")
    @Operation(summary = "Activate or de-activate a trainee",
            description = "Not idempotent: setting the state the trainee is already in returns 409. "
                    + "Only the trainee can change their own status.")
    @ApiResponse(responseCode = "200", description = "Status changed")
    @ApiResponse(responseCode = "400", description = "isActive is missing")
    @ApiResponse(responseCode = "401", description = "Missing or wrong credentials, or not a trainee")
    @ApiResponse(responseCode = "403", description = "Someone else's profile")
    @ApiResponse(responseCode = "404", description = "No trainee with this username")
    @ApiResponse(responseCode = "409", description = "Already in the requested state")
    public ResponseEntity<Void> setActive(@Parameter(hidden = true) @BasicAuth Credentials auth,
                                          @Parameter(description = "Trainee's username", example = "John.Smith")
                                          @PathVariable String username,
                                          @Valid @RequestBody ActiveStatusRequest request) {
        facade.setTraineeActive(auth, username, request.isActive());
        return ResponseEntity.ok().build();
    }
}
