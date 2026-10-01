package util;

import io.gymcrm.dao.UserDao;
import io.gymcrm.util.UsernameGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsernameGeneratorTest {

    @Mock
    private UserDao userDao;

    private UsernameGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new UsernameGenerator();
        generator.setUserDao(userDao);
    }

    private void givenTaken(String... usernames) {
        when(userDao.findUsernamesStartingWith("John.Smith")).thenReturn(Set.of(usernames));
    }

    @Test
    void usesFirstNameDotLastNameWhenFree() {
        givenTaken();

        assertEquals("John.Smith", generator.generate("John", "Smith"));
    }

    @Test
    void trimsNames() {
        givenTaken();

        assertEquals("John.Smith", generator.generate("  John ", " Smith "));
    }

    @Test
    void addsSerialNumberWhenTaken() {
        givenTaken("John.Smith");

        assertEquals("John.Smith1", generator.generate("John", "Smith"));
    }

    @Test
    void usesNextFreeSerialNumber() {
        givenTaken("John.Smith", "John.Smith1", "John.Smith2");

        assertEquals("John.Smith3", generator.generate("John", "Smith"));
    }

    @Test
    void fillsGapsInSerialNumbers() {
        givenTaken("John.Smith", "John.Smith2");

        assertEquals("John.Smith1", generator.generate("John", "Smith"));
    }

    @Test
    void ignoresLongerNamesWithTheSamePrefix() {
        givenTaken("John.Smithson");

        assertEquals("John.Smith", generator.generate("John", "Smith"));
    }
}
