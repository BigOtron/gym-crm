package web.auth;

import io.gymcrm.dto.Credentials;
import io.gymcrm.exceptions.AuthenticationException;
import io.gymcrm.web.auth.BasicAuth;
import io.gymcrm.web.auth.BasicAuthArgumentResolver;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BasicAuthArgumentResolverTest {

    private final BasicAuthArgumentResolver resolver = new BasicAuthArgumentResolver();

    @SuppressWarnings("unused")
    private void handler(@BasicAuth Credentials annotated, Credentials notAnnotated, @BasicAuth String wrongType) {
    }

    private static MethodParameter parameter(int index) throws NoSuchMethodException {
        Method method = BasicAuthArgumentResolverTest.class.getDeclaredMethod(
                "handler", Credentials.class, Credentials.class, String.class);
        return new MethodParameter(method, index);
    }

    @Test
    void supportsOnlyAnnotatedCredentialsParameters() throws Exception {
        assertTrue(resolver.supportsParameter(parameter(0)));
        assertFalse(resolver.supportsParameter(parameter(1)));
        assertFalse(resolver.supportsParameter(parameter(2)));
    }

    @Test
    void resolvesCredentialsFromHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic Sm9obi5TbWl0aDpzZWNyZXQ="); // John.Smith:secret

        Credentials credentials = resolver.resolveArgument(parameter(0), null, new ServletWebRequest(request), null);

        assertEquals(new Credentials("John.Smith", "secret"), credentials);
    }

    @Test
    void missingHeaderIsAnAuthenticationError() throws Exception {
        var request = new ServletWebRequest(new MockHttpServletRequest());

        assertThrows(AuthenticationException.class, () -> resolver.resolveArgument(parameter(0), null, request, null));
    }
}
