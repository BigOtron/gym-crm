package health;

import com.netflix.appinfo.InstanceInfo.InstanceStatus;
import io.gymcrm.health.EurekaHealthConfig.WarningAsUpHealthCheckHandler;
import io.gymcrm.health.TrainerAvailabilityHealthIndicator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.health.actuate.endpoint.SimpleStatusAggregator;
import org.springframework.boot.health.contributor.Status;
import org.springframework.cloud.netflix.eureka.EurekaHealthCheckHandler;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EurekaHealthConfigTest {

    private final WarningAsUpHealthCheckHandler handler = new WarningAsUpHealthCheckHandler(new SimpleStatusAggregator());

    @Test
    void warningIsReportedToEurekaAsUp() {
        assertEquals(InstanceStatus.UP, handler.mapToInstanceStatus(TrainerAvailabilityHealthIndicator.WARNING));
    }

    @ParameterizedTest
    @ValueSource(strings = {"UP", "DOWN", "OUT_OF_SERVICE", "UNKNOWN"})
    void otherStatusesAreMappedAsSpringCloudDoes(String code) {
        Status status = new Status(code);
        InstanceStatus springCloud = new EurekaHealthCheckHandler(new SimpleStatusAggregator()) {
            InstanceStatus map(Status s) {
                return mapToInstanceStatus(s);
            }
        }.map(status);

        assertEquals(springCloud, handler.mapToInstanceStatus(status));
    }

    @Test
    void downStaysDown() {
        assertEquals(InstanceStatus.DOWN, handler.mapToInstanceStatus(Status.DOWN));
    }
}
