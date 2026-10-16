package io.gymcrm.health;

import io.gymcrm.dao.TrainingTypeDao;
import io.gymcrm.entities.TrainingType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * "trainingTypes": the training types (reference data from db/data.sql) are present.
 * Without them no trainer can register, so an empty table means the application is DOWN.
 */
@Slf4j
@Component
public class TrainingTypesHealthIndicator extends AbstractHealthIndicator {

    private final TrainingTypeDao trainingTypeDao;

    public TrainingTypesHealthIndicator(TrainingTypeDao trainingTypeDao) {
        super("Training types check failed");
        this.trainingTypeDao = trainingTypeDao;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        List<String> names = trainingTypeDao.findAll().stream().map(TrainingType::getTrainingTypeName).toList();
        if (names.isEmpty()) {
            log.warn("Health: no training types found, trainers can't register");
            builder.down().withDetail("reason", "No training types found; check db/data.sql");
            return;
        }
        builder.up().withDetail("count", names.size()).withDetail("types", names);
    }
}
