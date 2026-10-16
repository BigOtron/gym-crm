package io.gymcrm.web.controller;

import io.gymcrm.dto.Credentials;
import io.gymcrm.facade.GymFacade;
import io.gymcrm.web.auth.BasicAuth;
import io.gymcrm.web.dto.NewTrainingRequest;
import io.gymcrm.web.mapper.WebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trainings")
@Tag(name = "Trainings", description = "Adding trainings; they can't be changed or deleted through the API")
public class TrainingController {

    private final GymFacade facade;

    public TrainingController(GymFacade facade) {
        this.facade = facade;
    }

    @PostMapping
    @Operation(summary = "Add a training",
            description = "Can be added by the training's trainee or trainer. The training type is the "
                    + "trainer's specialization, and the trainer is added to the trainee's trainers list.")
    @ApiResponse(responseCode = "200", description = "Training added")
    @ApiResponse(responseCode = "400", description = "A required field is missing, or the duration isn't positive")
    @ApiResponse(responseCode = "401", description = "Missing or wrong credentials")
    @ApiResponse(responseCode = "403", description = "Neither the trainee nor the trainer of this training")
    @ApiResponse(responseCode = "404", description = "The trainee or the trainer doesn't exist")
    public ResponseEntity<Void> add(@Parameter(hidden = true) @BasicAuth Credentials auth,
                                    @Valid @RequestBody NewTrainingRequest request) {
        facade.addTraining(auth, WebMapper.toNewTraining(request));
        return ResponseEntity.ok().build();
    }
}
