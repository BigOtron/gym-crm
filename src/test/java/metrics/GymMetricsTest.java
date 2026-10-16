package metrics;

import io.gymcrm.metrics.GymMetrics;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GymMetricsTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final GymMetrics metrics = new GymMetrics(registry);

    private double count(String name, String tag, String value) {
        return registry.get(name).tag(tag, value).counter().count();
    }

    @Test
    void knownCountersStartAtZero() {
        assertEquals(0, count(GymMetrics.REGISTRATIONS, "role", "trainee"));
        assertEquals(0, count(GymMetrics.REGISTRATIONS, "role", "trainer"));
        assertEquals(0, count(GymMetrics.LOGINS, "result", "success"));
        assertEquals(0, count(GymMetrics.LOGINS, "result", "failure"));
    }

    @Test
    void countsPerLabel() {
        metrics.userRegistered("trainee");
        metrics.userRegistered("trainee");
        metrics.userRegistered("trainer");
        metrics.loginFailed();
        metrics.authenticationFailed("trainee");

        assertEquals(2, count(GymMetrics.REGISTRATIONS, "role", "trainee"));
        assertEquals(1, count(GymMetrics.REGISTRATIONS, "role", "trainer"));
        assertEquals(1, count(GymMetrics.LOGINS, "result", "failure"));
        assertEquals(1, count(GymMetrics.AUTHENTICATION_FAILURES, "role", "trainee"));
    }

    @Test
    void trainingsAreCountedAndTheirDurationsSummarisedPerType() {
        metrics.trainingCreated("Yoga", 60);
        metrics.trainingCreated("Yoga", 30);
        metrics.trainingCreated("Running", 45);

        DistributionSummary yoga = registry.get(GymMetrics.TRAINING_DURATION).tag("type", "Yoga").summary();
        assertEquals(2, count(GymMetrics.TRAININGS_ADDED, "type", "Yoga"));
        assertEquals(1, count(GymMetrics.TRAININGS_ADDED, "type", "Running"));
        assertEquals(2, yoga.count());
        assertEquals(90, yoga.totalAmount());
        assertEquals(60, yoga.max());
        assertEquals("minutes", yoga.getId().getBaseUnit());
    }
}
