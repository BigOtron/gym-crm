package io.gymcrm.web.controller;

import io.gymcrm.dto.Credentials;
import io.gymcrm.facade.GymFacade;
import io.gymcrm.web.auth.BasicAuth;
import io.gymcrm.web.dto.NewTrainingRequest;
import io.gymcrm.web.mapper.WebMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trainings")
public class TrainingController {

    private final GymFacade facade;

    public TrainingController(GymFacade facade) {
        this.facade = facade;
    }

    @PostMapping
    public ResponseEntity<Void> add(@BasicAuth Credentials auth, @Valid @RequestBody NewTrainingRequest request) {
        facade.addTraining(auth, WebMapper.toNewTraining(request));
        return ResponseEntity.ok().build();
    }
}
