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
public class TraineeController {

    private final GymFacade facade;

    public TraineeController(GymFacade facade) {
        this.facade = facade;
    }


    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CredentialsResponse register(@Valid @RequestBody TraineeRegistrationRequest request) {
        return WebMapper.toCredentials(facade.createTrainee(WebMapper.toRegistration(request)).getUser());
    }


    @GetMapping("/{username}")
    public TraineeProfileResponse getProfile(@BasicAuth Credentials auth, @PathVariable String username) {
        return WebMapper.toProfile(facade.getTraineeByUsername(auth, username));
    }


    @PutMapping("/{username}")
    public TraineeProfileResponse update(@BasicAuth Credentials auth, @PathVariable String username,
                                         @Valid @RequestBody TraineeUpdateRequest request) {
        return WebMapper.toProfile(facade.updateTrainee(auth, username, WebMapper.toUpdate(request)));
    }


    @DeleteMapping("/{username}")
    public ResponseEntity<Void> delete(@BasicAuth Credentials auth, @PathVariable String username) {
        facade.deleteTrainee(auth, username);
        return ResponseEntity.ok().build();
    }


    @GetMapping("/{username}/available-trainers")
    public List<TrainerSummary> getAvailableTrainers(@BasicAuth Credentials auth, @PathVariable String username) {
        return WebMapper.toTrainerSummaries(facade.getTrainersNotAssignedToTrainee(auth, username));
    }

    @PutMapping("/{username}/trainers")
    public List<TrainerSummary> updateTrainers(@BasicAuth Credentials auth, @PathVariable String username,
                                               @Valid @RequestBody UpdateTrainersRequest request) {
        return WebMapper.toTrainerSummaries(facade.updateTraineeTrainers(auth, username, request.trainerUsernames()));
    }

    @GetMapping("/{username}/trainings")
    public List<TraineeTrainingResponse> getTrainings(@BasicAuth Credentials auth, @PathVariable String username,
                                                      @Valid TraineeTrainingsQuery query) {
        return WebMapper.toTraineeTrainings(facade.getTraineeTrainings(auth, username, WebMapper.toCriteria(query)));
    }

    @PatchMapping("/{username}/status")
    public ResponseEntity<Void> setActive(@BasicAuth Credentials auth, @PathVariable String username,
                                          @Valid @RequestBody ActiveStatusRequest request) {
        facade.setTraineeActive(auth, username, request.isActive());
        return ResponseEntity.ok().build();
    }
}
