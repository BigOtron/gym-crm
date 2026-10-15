package io.gymcrm.web.controller;

import io.gymcrm.dto.Credentials;
import io.gymcrm.facade.GymFacade;
import io.gymcrm.web.auth.BasicAuth;
import io.gymcrm.web.dto.TrainingTypeResponse;
import io.gymcrm.web.mapper.WebMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/training-types")
public class TrainingTypeController {

    private final GymFacade facade;

    public TrainingTypeController(GymFacade facade) {
        this.facade = facade;
    }

    @GetMapping
    public List<TrainingTypeResponse> getAll(@BasicAuth Credentials auth) {
        return WebMapper.toTrainingTypes(facade.getTrainingTypes(auth));
    }
}
