package config;

import ch.qos.logback.classic.spi.ILoggingEvent;
import io.gymcrm.config.DatabaseSettingsCheck;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import support.LogCapture;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseSettingsCheckTest {

    private static MockEnvironment prod() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:postgresql://${DB_HOST}:5432/gymcrm")
                .withProperty("spring.datasource.username", "${DB_USERNAME}")
                .withProperty("spring.datasource.password", "${DB_PASSWORD}");
        env.setActiveProfiles("prod");
        return env;
    }

    private static void check(MockEnvironment env) {
        DatabaseSettingsCheck check = new DatabaseSettingsCheck();
        check.setEnvironment(env);
        check.postProcessBeanFactory(null);
    }

    @Test
    void passesAndLogsTheDatabaseWithoutThePassword() {
        MockEnvironment env = prod()
                .withProperty("DB_HOST", "db.example")
                .withProperty("DB_USERNAME", "gym")
                .withProperty("DB_PASSWORD", "top-secret");

        try (LogCapture logs = new LogCapture(DatabaseSettingsCheck.class.getName())) {
            check(env);

            List<String> messages = logs.messages();
            assertEquals(List.of("Environment [prod]: database jdbc:postgresql://db.example:5432/gymcrm as user gym"),
                    messages);
            assertFalse(messages.get(0).contains("top-secret"));
        }
    }

    @Test
    void missingVariableStopsTheStartUpAndNamesIt() {
        MockEnvironment env = prod()
                .withProperty("DB_HOST", "db.example")
                .withProperty("DB_USERNAME", "gym");

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> check(env));

        assertTrue(e.getMessage().contains("spring.datasource.password"), e.getMessage());
        assertTrue(e.getMessage().contains("DB_PASSWORD"), e.getMessage());
        assertTrue(e.getMessage().contains("[prod]"), e.getMessage());
    }

    @Test
    void missingHostIsReportedFirst() {
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> check(prod()));

        assertTrue(e.getMessage().contains("DB_HOST"), e.getMessage());
    }

    @Test
    void settingMissingCompletely() {
        MockEnvironment env = new MockEnvironment().withProperty("spring.datasource.url", "jdbc:h2:mem:x");

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> check(env));

        assertTrue(e.getMessage().contains("spring.datasource.username is not set"), e.getMessage());
    }

    @Test
    void defaultProfileIsNamedWhenNoneIsActive() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:postgresql://localhost:5432/gymcrm")
                .withProperty("spring.datasource.username", "gymcrm")
                .withProperty("spring.datasource.password", "gymcrm");
        env.setDefaultProfiles("local");

        try (LogCapture logs = new LogCapture(DatabaseSettingsCheck.class.getName())) {
            check(env);
            List<ILoggingEvent> events = logs.events();
            assertTrue(events.get(0).getFormattedMessage().startsWith("Environment [local]"));
        }
    }
}
