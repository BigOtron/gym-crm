package config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Loads the real application.yml and application-&lt;profile&gt;.yml files the way Spring Boot does
 * (but without starting the application or connecting to a database) and checks what each
 * environment ends up with.
 */
class ProfilesTest {

    /** An empty configuration: only the Environment is built, no beans and no auto-configuration. */
    @Configuration
    static class NoBeans {
    }

    private ConfigurableApplicationContext context;

    @AfterEach
    void close() {
        if (context != null) {
            context.close();
        }
    }

    /** The environment for a profile; {@code variables} play the role of OS environment variables. */
    private Environment environment(String profile, Map<String, Object> variables) {
        // All tests run with the "test" profile (see build.gradle.kts). A command-line argument
        // overrides that, so exactly the requested profile is active (none means the default, "local").
        String activeProfiles = "--spring.profiles.active=" + (profile == null ? "" : profile);
        context = new SpringApplicationBuilder(NoBeans.class)
                .web(WebApplicationType.NONE)
                .logStartupInfo(false)
                .properties(variables)
                .run(activeProfiles);
        return context.getEnvironment();
    }

    private static Map<String, Object> allVariables() {
        return Map.of("DB_HOST", "db.example", "DB_USERNAME", "user", "DB_PASSWORD", "secret");
    }

    @Test
    void withoutAProfileLocalIsUsed() {
        Environment env = environment(null, Map.of());

        assertArrayEquals(new String[]{"local"}, env.getDefaultProfiles());
        assertEquals("jdbc:postgresql://localhost:5432/gymcrm", env.getProperty("spring.datasource.url"));
        assertEquals("gymcrm", env.getProperty("spring.datasource.password"));
    }

    @Test
    void eachEnvironmentHasItsOwnDatabase() {
        Set<String> urls = Stream.of("local", "dev", "stg", "prod")
                .map(profile -> {
                    String url = environment(profile, allVariables()).getProperty("spring.datasource.url");
                    context.close();
                    return url;
                })
                .collect(Collectors.toSet());

        assertEquals(Set.of(
                "jdbc:postgresql://localhost:5432/gymcrm",
                "jdbc:postgresql://db.example:5432/gymcrm_dev",
                "jdbc:postgresql://db.example:5432/gymcrm_stg",
                "jdbc:postgresql://db.example:5432/gymcrm"), urls);
    }

    @Test
    void devAndStgHaveDefaultHostsAndUsers() {
        Environment dev = environment("dev", Map.of("DB_PASSWORD", "secret"));

        assertEquals("jdbc:postgresql://dev-db.gymcrm.internal:5432/gymcrm_dev", dev.getProperty("spring.datasource.url"));
        assertEquals("gymcrm_dev", dev.getProperty("spring.datasource.username"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"dev", "stg", "prod"})
    void serversRequireThePasswordFromTheEnvironment(String profile) {
        Environment env = environment(profile, Map.of("DB_HOST", "db.example", "DB_USERNAME", "user"));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> env.getProperty("spring.datasource.password"));
        assertTrue(e.getMessage().contains("DB_PASSWORD"), e.getMessage());
    }

    @Test
    void prodRequiresHostAndUserToo() {
        Environment env = environment("prod", Map.of("DB_PASSWORD", "secret"));

        assertThrows(IllegalArgumentException.class, () -> env.getProperty("spring.datasource.url"));
        assertThrows(IllegalArgumentException.class, () -> env.getProperty("spring.datasource.username"));
    }

    @Test
    void connectionPoolGrowsTowardsProduction() {
        int local = poolSize("local");
        int dev = poolSize("dev");
        int stg = poolSize("stg");
        int prod = poolSize("prod");

        assertTrue(local < dev && dev < stg && stg < prod, local + " " + dev + " " + stg + " " + prod);
    }

    private int poolSize(String profile) {
        int size = Integer.parseInt(environment(profile, allVariables())
                .getProperty("spring.datasource.hikari.maximum-pool-size"));
        context.close();
        return size;
    }

    @Test
    void debugLoggingOnlyOutsideStagingAndProduction() {
        assertEquals("DEBUG", logLevel("local"));
        assertEquals("DEBUG", logLevel("dev"));
        assertEquals("INFO", logLevel("stg"));
        assertEquals("INFO", logLevel("prod"));
    }

    private String logLevel(String profile) {
        String level = environment(profile, allVariables()).getProperty("logging.level.io.gymcrm");
        context.close();
        return level;
    }

    @Test
    void sharedSettingsApplyToEveryEnvironment() {
        for (String profile : new String[]{"local", "dev", "stg", "prod"}) {
            Environment env = environment(profile, allVariables());
            assertEquals("validate", env.getProperty("spring.jpa.hibernate.ddl-auto"), profile);
            assertEquals("false", env.getProperty("spring.jpa.open-in-view"), profile);
            context.close();
        }
    }

    @Test
    void healthDetailsAreOnlyShownOnDeveloperEnvironments() {
        // The actuator endpoints have no authentication, and details include e.g. the database URL.
        assertEquals("always", healthDetails("local"));
        assertEquals("always", healthDetails("dev"));
        assertEquals("never", healthDetails("stg"));
        assertEquals("never", healthDetails("prod"));
    }

    private String healthDetails(String profile) {
        String value = environment(profile, allVariables()).getProperty("management.endpoint.health.show-details");
        context.close();
        return value;
    }
}
