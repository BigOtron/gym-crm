package support;

import io.gymcrm.metrics.GymMetrics;
import io.gymcrm.web.error.GlobalExceptionHandler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * For controller unit tests: MockMvc around a single controller (with its facade mocked) and the error
 * handler. No Spring context, database or security filters are started; those are in the integration tests.
 */
public final class ControllerTestSupport {

    private ControllerTestSupport() {
    }

    public static MockMvc mockMvc(Object controller) {
        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(new GymMetrics(new SimpleMeterRegistry())))
                .build();
    }
}
