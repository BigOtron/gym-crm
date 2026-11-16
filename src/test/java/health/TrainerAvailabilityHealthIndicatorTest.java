package health;

import io.gymcrm.dao.TrainerDao;
import io.gymcrm.health.TrainerAvailabilityHealthIndicator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainerAvailabilityHealthIndicatorTest {

    @Mock
    private TrainerDao trainerDao;

    private Health check() {
        return new TrainerAvailabilityHealthIndicator(trainerDao).health();
    }

    private static Map<String, Long> counts(Object... typeAndCount) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (int i = 0; i < typeAndCount.length; i += 2) {
            counts.put((String) typeAndCount[i], ((Number) typeAndCount[i + 1]).longValue());
        }
        return counts;
    }

    @Test
    void upWhenEveryTypeHasAnActiveTrainer() {
        when(trainerDao.countActiveTrainersPerTrainingType()).thenReturn(counts("Fitness", 2, "Yoga", 1));

        Health health = check();

        assertEquals(Status.UP, health.getStatus());
        assertFalse(health.getDetails().containsKey("withoutTrainer"));
    }

    @Test
    void warningListsTheTypesWithoutTrainer() {
        when(trainerDao.countActiveTrainersPerTrainingType()).thenReturn(counts("Fitness", 0, "Yoga", 3, "Running", 0));

        Health health = check();

        assertEquals(TrainerAvailabilityHealthIndicator.WARNING, health.getStatus());
        assertEquals(List.of("Fitness", "Running"), health.getDetails().get("withoutTrainer"));
        assertEquals(counts("Fitness", 0, "Yoga", 3, "Running", 0), health.getDetails().get("activeTrainers"));
    }

    @Test
    void unknownWithoutTrainingTypes() {
        when(trainerDao.countActiveTrainersPerTrainingType()).thenReturn(Map.of());

        assertEquals(Status.UNKNOWN, check().getStatus());
    }

    @Test
    void downWhenTheQueryFails() {
        when(trainerDao.countActiveTrainersPerTrainingType()).thenThrow(new IllegalStateException("db gone"));

        assertEquals(Status.DOWN, check().getStatus());
    }
}
