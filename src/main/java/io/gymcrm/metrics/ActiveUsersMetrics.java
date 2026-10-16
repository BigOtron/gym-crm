package io.gymcrm.metrics;

import io.gymcrm.dao.TraineeDao;
import io.gymcrm.dao.TrainerDao;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.stereotype.Component;

/**
 * {@code gymcrm_users_active{role="trainee"|"trainer"}}: the number of active users right now.
 * A gauge reports a current value instead of counting events; Micrometer calls the count queries
 * each time the metrics are read, e.g. on every Prometheus scrape.
 * Spring Boot registers every {@link MeterBinder} bean automatically.
 */
@Component
public class ActiveUsersMetrics implements MeterBinder {

    public static final String ACTIVE_USERS = "gymcrm.users.active";

    private final TraineeDao traineeDao;
    private final TrainerDao trainerDao;

    public ActiveUsersMetrics(TraineeDao traineeDao, TrainerDao trainerDao) {
        this.traineeDao = traineeDao;
        this.trainerDao = trainerDao;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        Gauge.builder(ACTIVE_USERS, traineeDao, TraineeDao::countActive)
                .description("Active users")
                .tag("role", GymMetrics.TRAINEE)
                .register(registry);
        Gauge.builder(ACTIVE_USERS, trainerDao, TrainerDao::countActive)
                .description("Active users")
                .tag("role", GymMetrics.TRAINER)
                .register(registry);
    }
}
