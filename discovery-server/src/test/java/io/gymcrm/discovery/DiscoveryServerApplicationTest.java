package io.gymcrm.discovery;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "logging.level.root=WARN")
class DiscoveryServerApplicationTest {

    @LocalServerPort
    private int port;

    @Test
    void startsAndReportsHealthy() {
        String health = RestClient.create("http://localhost:" + port)
                .get().uri("/actuator/health").retrieve().body(String.class);

        assertThat(health).contains("\"status\":\"UP\"");
    }
}
