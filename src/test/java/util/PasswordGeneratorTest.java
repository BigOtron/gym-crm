package util;

import io.gymcrm.util.PasswordGenerator;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordGeneratorTest {

    private final PasswordGenerator generator = new PasswordGenerator();

    @RepeatedTest(20)
    void generatesTenAlphanumericCharacters() {
        String password = generator.generate();

        assertEquals(10, password.length());
        assertTrue(password.matches("[A-Za-z0-9]{10}"), "unexpected characters in " + password);
    }

    @Test
    void generatesDifferentPasswords() {
        Set<String> passwords = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            passwords.add(generator.generate());
        }

        assertEquals(100, passwords.size());
    }
}
