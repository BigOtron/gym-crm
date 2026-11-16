package io.gymcrm.web.controller;

import io.gymcrm.dto.Credentials;
import io.gymcrm.facade.GymFacade;
import io.gymcrm.web.auth.BasicAuth;
import io.gymcrm.web.dto.TrainingTypeResponse;
import io.gymcrm.web.mapper.WebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/training-types")
@Tag(name = "Training types", description = "The fixed list of training types")
public class TrainingTypeController {

    private final GymFacade facade;

    public TrainingTypeController(GymFacade facade) {
        this.facade = facade;
    }

    @GetMapping
    @Operation(summary = "List training types",
            description = "The ids are used as specializationId when registering a trainer.")
    @ApiResponse(responseCode = "200", description = "All training types, sorted by id")
    @ApiResponse(responseCode = "401", description = "Missing or wrong credentials")
    public List<TrainingTypeResponse> getAll(@Parameter(hidden = true) @BasicAuth Credentials auth) {
        return WebMapper.toTrainingTypes(facade.getTrainingTypes(auth));
    }
}
