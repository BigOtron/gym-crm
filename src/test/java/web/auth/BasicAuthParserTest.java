package web.auth;

import io.gymcrm.dto.Credentials;
import io.gymcrm.exceptions.AuthenticationException;
import io.gymcrm.web.auth.BasicAuthParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BasicAuthParserTest {

    private static String basic(String value) {
        return "Basic " + Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void parsesUsernameAndPassword() {
        assertEquals(new Credentials("John.Smith", "secret"), BasicAuthParser.parse(basic("John.Smith:secret")));
    }

    @Test
    void passwordMayContainColons() {
        assertEquals(new Credentials("John.Smith", "a:b:c"), BasicAuthParser.parse(basic("John.Smith:a:b:c")));
    }

    @Test
    void passwordMayBeEmpty() {
        assertEquals(new Credentials("John.Smith", ""), BasicAuthParser.parse(basic("John.Smith:")));
    }

    @Test
    void schemeIsCaseInsensitive() {
        String header = "bAsIc " + Base64.getEncoder().encodeToString("John.Smith:secret".getBytes());

        assertEquals("John.Smith", BasicAuthParser.parse(header).username());
    }

    @Test
    void decodesUtf8() {
        assertEquals("Jürgen.Müller", BasicAuthParser.parse(basic("Jürgen.Müller:pw")).username());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "Bearer abc", "Basic", "Basic !!!not-base64!!!"})
    void rejectsMissingOrMalformedHeader(String header) {
        assertThrows(AuthenticationException.class, () -> BasicAuthParser.parse(header));
    }

    @ParameterizedTest
    @ValueSource(strings = {"no-colon", ":password-without-username"})
    void rejectsValueWithoutUsername(String value) {
        assertThrows(AuthenticationException.class, () -> BasicAuthParser.parse(basic(value)));
    }
}
