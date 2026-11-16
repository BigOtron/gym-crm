package health;

import io.gymcrm.dao.TrainingTypeDao;
import io.gymcrm.health.TrainingTypesHealthIndicator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static support.TestData.FITNESS;
import static support.TestData.YOGA;

@ExtendWith(MockitoExtension.class)
class TrainingTypesHealthIndicatorTest {

    @Mock
    private TrainingTypeDao trainingTypeDao;

    private Health check() {
        return new TrainingTypesHealthIndicator(trainingTypeDao).health();
    }

    @Test
    void upWhenTypesExist() {
        when(trainingTypeDao.findAll()).thenReturn(List.of(FITNESS, YOGA));

        Health health = check();

        assertEquals(Status.UP, health.getStatus());
        assertEquals(2, health.getDetails().get("count"));
        assertEquals(List.of("Fitness", "Yoga"), health.getDetails().get("types"));
    }

    @Test
    void downWhenTheTableIsEmpty() {
        when(trainingTypeDao.findAll()).thenReturn(List.of());

        Health health = check();

        assertEquals(Status.DOWN, health.getStatus());
        assertTrue(health.getDetails().get("reason").toString().contains("No training types"));
    }

    @Test
    void downWhenTheQueryFails() {
        when(trainingTypeDao.findAll()).thenThrow(new IllegalStateException("connection refused"));

        Health health = check();

        assertEquals(Status.DOWN, health.getStatus());
        assertTrue(health.getDetails().get("error").toString().contains("connection refused"));
    }
}
