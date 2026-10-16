package io.gymcrm.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * "databaseResponseTime": how fast the database answers a trivial query. Spring Boot's own "db"
 * check only tells whether a connection works; a database that answers but takes seconds makes
 * every request slow, so above {@code gymcrm.health.database.max-response-time} this is DOWN.
 */
@Slf4j
@Component
public class DatabaseResponseTimeHealthIndicator extends AbstractHealthIndicator {

    private final JdbcTemplate jdbcTemplate;
    private final Duration maxResponseTime;

    public DatabaseResponseTimeHealthIndicator(JdbcTemplate jdbcTemplate,
                                               @Value("${gymcrm.health.database.max-response-time:500ms}")
                                               Duration maxResponseTime) {
        super("Database response time check failed");
        this.jdbcTemplate = jdbcTemplate;
        this.maxResponseTime = maxResponseTime;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        long start = System.nanoTime();
        jdbcTemplate.queryForObject("select 1", Integer.class);
        Duration responseTime = Duration.ofNanos(System.nanoTime() - start);

        builder.withDetail("responseTimeMs", responseTime.toMillis())
                .withDetail("maxResponseTimeMs", maxResponseTime.toMillis());
        if (responseTime.compareTo(maxResponseTime) > 0) {
            log.warn("Health: database answered in {} ms, limit is {} ms",
                    responseTime.toMillis(), maxResponseTime.toMillis());
            builder.down();
        } else {
            builder.up();
        }
    }
}
