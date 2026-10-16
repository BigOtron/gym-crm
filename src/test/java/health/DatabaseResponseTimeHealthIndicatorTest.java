package health;

import io.gymcrm.health.DatabaseResponseTimeHealthIndicator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabaseResponseTimeHealthIndicatorTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private Health check(Duration limit) {
        return new DatabaseResponseTimeHealthIndicator(jdbcTemplate, limit).health();
    }

    @Test
    void upWhenTheDatabaseIsFast() {
        when(jdbcTemplate.queryForObject("select 1", Integer.class)).thenReturn(1);

        Health health = check(Duration.ofSeconds(5));

        assertEquals(Status.UP, health.getStatus());
        assertEquals(5000L, health.getDetails().get("maxResponseTimeMs"));
        assertTrue((Long) health.getDetails().get("responseTimeMs") < 5000);
    }

    @Test
    void downWhenTheDatabaseIsSlowerThanTheLimit() {
        when(jdbcTemplate.queryForObject("select 1", Integer.class)).thenAnswer(invocation -> {
            Thread.sleep(60);
            return 1;
        });

        Health health = check(Duration.ofMillis(20));

        assertEquals(Status.DOWN, health.getStatus());
        assertTrue((Long) health.getDetails().get("responseTimeMs") >= 60);
    }

    @Test
    void downWhenTheDatabaseDoesNotAnswer() {
        when(jdbcTemplate.queryForObject("select 1", Integer.class))
                .thenThrow(new DataAccessResourceFailureException("connection refused"));

        Health health = check(Duration.ofSeconds(5));

        assertEquals(Status.DOWN, health.getStatus());
        assertTrue(health.getDetails().get("error").toString().contains("connection refused"));
    }
}
