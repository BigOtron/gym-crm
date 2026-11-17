package io.gymcrm.workload.web;

import io.gymcrm.workload.model.TrainerWorkload;
import io.gymcrm.workload.service.TrainerWorkloadService;
import io.gymcrm.workload.service.WorkloadUpdate;
import io.gymcrm.workload.web.dto.MonthlyWorkloadResponse;
import io.gymcrm.workload.web.dto.TrainerWorkloadResponse;
import io.gymcrm.workload.web.dto.TrainerWorkloadResponse.MonthSummary;
import io.gymcrm.workload.web.dto.TrainerWorkloadResponse.TrainerStatus;
import io.gymcrm.workload.web.dto.TrainerWorkloadResponse.YearSummary;
import io.gymcrm.workload.web.dto.WorkloadRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/trainer-workloads")
@Tag(name = "Trainer workload", description = "Trainers' training minutes per month")
public class TrainerWorkloadController {

    private final TrainerWorkloadService service;

    public TrainerWorkloadController(TrainerWorkloadService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Add or remove a training's minutes (scope workload.write)")
    @ApiResponse(responseCode = "200", description = "Applied")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "401", description = "Missing or invalid token")
    @ApiResponse(responseCode = "403", description = "Token without the workload.write scope")
    public ResponseEntity<Void> apply(@Valid @RequestBody WorkloadRequest request) {
        service.apply(new WorkloadUpdate(request.trainerUsername(), request.trainerFirstName(),
                request.trainerLastName(), request.isActive(), request.trainingDate(),
                request.trainingDuration(), request.actionType()));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{username}")
    @Operation(summary = "A trainer's minutes for every year and month (scope workload.read)")
    @ApiResponse(responseCode = "200", description = "Found")
    @ApiResponse(responseCode = "404", description = "No trainings were ever reported for this trainer")
    public TrainerWorkloadResponse getWorkload(@PathVariable String username) {
        return toResponse(service.getWorkload(username));
    }

    @GetMapping("/{username}/years/{year}/months/{month}")
    @Operation(summary = "A trainer's minutes in one month (scope workload.read)")
    @ApiResponse(responseCode = "200", description = "Found (0 if the trainer had no trainings that month)")
    @ApiResponse(responseCode = "400", description = "Invalid year or month")
    @ApiResponse(responseCode = "404", description = "No trainings were ever reported for this trainer")
    public MonthlyWorkloadResponse getMonth(@PathVariable String username,
                                            @PathVariable @Min(1) @Max(9999) int year,
                                            @PathVariable @Min(1) @Max(12) int month) {
        int minutes = service.getMinutes(username, YearMonth.of(year, month));
        return new MonthlyWorkloadResponse(username, year, month, minutes);
    }

    private static TrainerWorkloadResponse toResponse(TrainerWorkload workload) {
        List<YearSummary> years = workload.getMinutesByYear().entrySet().stream()
                .map(year -> new YearSummary(year.getKey(), year.getValue().entrySet().stream()
                        .map(month -> new MonthSummary(month.getKey(), month.getValue()))
                        .toList()))
                .toList();
        return new TrainerWorkloadResponse(workload.getUsername(), workload.getFirstName(),
                workload.getLastName(), workload.isActive() ? TrainerStatus.ACTIVE : TrainerStatus.INACTIVE, years);
    }
}
