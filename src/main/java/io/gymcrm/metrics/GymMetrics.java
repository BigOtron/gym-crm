package io.gymcrm.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * The application's own business metrics, exported to Prometheus at /actuator/prometheus.
 * Dots in the names become underscores there, and counters get a "_total" suffix, e.g.
 * {@code gymcrm_registrations_total{role="trainee"}}.
 * <p>
 * Label values are always from small, fixed sets (role, result, training type): every distinct
 * value creates a separate time series in Prometheus, so never use e.g. usernames as labels.
 */
@Component
public class GymMetrics {

    public static final String REGISTRATIONS = "gymcrm.registrations";
    public static final String LOGINS = "gymcrm.logins";
    public static final String AUTHENTICATION_FAILURES = "gymcrm.authentication.failures";
    /** Not "...created": Prometheus reserves the "_created" suffix and would drop it from the name. */
    public static final String TRAININGS_ADDED = "gymcrm.trainings.added";
    public static final String TRAINING_DURATION = "gymcrm.trainings.duration";

    public static final String TRAINEE = "trainee";
    public static final String TRAINER = "trainer";

    private final MeterRegistry registry;

    public GymMetrics(MeterRegistry registry) {
        this.registry = registry;
        // Create the counters with known labels up front, so they are exported as 0 before the
        // first event instead of being missing (Prometheus queries handle 0 better than "no data").
        registrations(TRAINEE);
        registrations(TRAINER);
        logins("success");
        logins("failure");
    }

    public void userRegistered(String role) {
        registrations(role).increment();
    }

    public void loginSucceeded() {
        logins("success").increment();
    }

    public void loginFailed() {
        logins("failure").increment();
    }

    /** Credentials rejected on any operation; {@code role} is the role that was required. */
    public void authenticationFailed(String role) {
        Counter.builder(AUTHENTICATION_FAILURES)
                .description("Requests rejected because of wrong or missing credentials")
                .tag("role", role)
                .register(registry)
                .increment();
    }

    public void trainingCreated(String trainingType, int durationMinutes) {
        Counter.builder(TRAININGS_ADDED)
                .description("Trainings added")
                .tag("type", trainingType)
                .register(registry)
                .increment();
        DistributionSummary.builder(TRAINING_DURATION)
                .description("Duration of the trainings added")
                .baseUnit("minutes")
                .tag("type", trainingType)
                .register(registry)
                .record(durationMinutes);
    }

    // register() returns the existing meter if one with the same name and tags exists.

    private Counter registrations(String role) {
        return Counter.builder(REGISTRATIONS)
                .description("Trainee and trainer profiles created")
                .tag("role", role)
                .register(registry);
    }

    private Counter logins(String result) {
        return Counter.builder(LOGINS)
                .description("Calls to the login endpoint")
                .tag("result", result)
                .register(registry);
    }
}
