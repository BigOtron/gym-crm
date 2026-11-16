package metrics;

import io.gymcrm.dao.TraineeDao;
import io.gymcrm.dao.TrainerDao;
import io.gymcrm.metrics.ActiveUsersMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActiveUsersMetricsTest {

    @Mock
    private TraineeDao traineeDao;

    @Mock
    private TrainerDao trainerDao;

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

    private double gauge(String role) {
        return registry.get(ActiveUsersMetrics.ACTIVE_USERS).tag("role", role).gauge().value();
    }

    @Test
    void gaugesAskTheDatabaseEachTimeTheyAreRead() {
        new ActiveUsersMetrics(traineeDao, trainerDao).bindTo(registry);
        when(traineeDao.countActive()).thenReturn(5L, 6L);
        when(trainerDao.countActive()).thenReturn(2L);

        assertEquals(5, gauge("trainee"));
        assertEquals(6, gauge("trainee"), "a gauge is read again, not cached");
        assertEquals(2, gauge("trainer"));
    }

    @Test
    void bindingDoesNotQueryTheDatabaseYet() {
        new ActiveUsersMetrics(traineeDao, trainerDao).bindTo(registry);

        verifyNoInteractions(traineeDao, trainerDao);
    }
}
