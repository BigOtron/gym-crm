package io.gymcrm.health;

import io.gymcrm.dao.TrainerDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * "trainerAvailability": every training type has at least one active trainer. A type without one
 * can't be booked; that isn't an outage, so it is reported as {@link #WARNING} (HTTP 200), not DOWN.
 */
@Slf4j
@Component
public class TrainerAvailabilityHealthIndicator extends AbstractHealthIndicator {

    /** Ranked between DOWN and UP and mapped to HTTP 200, see management.endpoint.health.status in application.yml. */
    public static final Status WARNING = new Status("WARNING", "Some training types have no active trainer");

    private final TrainerDao trainerDao;

    public TrainerAvailabilityHealthIndicator(TrainerDao trainerDao) {
        super("Trainer availability check failed");
        this.trainerDao = trainerDao;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        Map<String, Long> activeTrainers = trainerDao.countActiveTrainersPerTrainingType();
        if (activeTrainers.isEmpty()) {
            // Reported by the trainingTypes check; nothing to say about trainers.
            builder.unknown().withDetail("reason", "No training types");
            return;
        }
        List<String> uncovered = activeTrainers.entrySet().stream()
                .filter(entry -> entry.getValue() == 0)
                .map(Map.Entry::getKey)
                .toList();

        builder.withDetail("activeTrainers", activeTrainers);
        if (uncovered.isEmpty()) {
            builder.up();
        } else {
            log.debug("Health: no active trainer for {}", uncovered);
            builder.status(WARNING).withDetail("withoutTrainer", uncovered);
        }
    }
}
