package support;

import io.gymcrm.dto.Credentials;
import io.gymcrm.web.auth.BasicAuthArgumentResolver;
import io.gymcrm.web.error.GlobalExceptionHandler;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class ControllerTestSupport {

    public static final Credentials JOHN = new Credentials("John.Smith", "secret");
    public static final Credentials ANNA = new Credentials("Anna.Lee", "secret");

    private ControllerTestSupport() {
    }

    public static MockMvc mockMvc(Object controller) {
        return MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new BasicAuthArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    public static String basic(Credentials credentials) {
        String value = credentials.username() + ":" + credentials.password();
        return "Basic " + Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
