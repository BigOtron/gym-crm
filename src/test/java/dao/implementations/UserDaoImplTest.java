package dao.implementations;

import io.gymcrm.dao.UserDao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import support.DaoTestBase;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserDaoImplTest extends DaoTestBase {

    @Autowired
    private UserDao userDao;

    @Test
    void findByUsernameFindsTraineesAndTrainers() {
        persistTrainee("John", "Smith", "John.Smith");
        persistTrainer("Anna", "Lee", "Anna.Lee", "Yoga");
        flushAndClear();

        assertEquals("John", userDao.findByUsername("John.Smith").orElseThrow().getFirstName());
        assertEquals("Anna", userDao.findByUsername("Anna.Lee").orElseThrow().getFirstName());
    }

    @Test
    void findByUsernameIsExactAndCaseSensitive() {
        persistTrainee("John", "Smith", "John.Smith");

        assertTrue(userDao.findByUsername("john.smith").isEmpty());
        assertTrue(userDao.findByUsername("John.Smit").isEmpty());
    }

    @Test
    void findUsernamesStartingWithReturnsPrefixMatches() {
        persistTrainee("John", "Smith", "John.Smith");
        persistTrainee("John", "Smith", "John.Smith1");
        persistTrainer("John", "Smith", "John.Smith2", "Yoga");
        persistTrainee("John", "Smithson", "John.Smithson");
        persistTrainee("Johnny", "Smith", "Johnny.Smith");

        Set<String> found = userDao.findUsernamesStartingWith("John.Smith");

        assertEquals(Set.of("John.Smith", "John.Smith1", "John.Smith2", "John.Smithson"), found);
    }

    @Test
    void underscoreIsNotTreatedAsWildcard() {
        persistTrainee("John", "Smith", "John.Smith");
        persistTrainee("John", "XSmith", "JohnXSmith");

        assertTrue(userDao.findUsernamesStartingWith("John_").isEmpty());
    }

    @Test
    void percentIsNotTreatedAsWildcard() {
        persistTrainee("John", "Smith", "John.Smith");

        assertTrue(userDao.findUsernamesStartingWith("%").isEmpty());
    }

    @Test
    void specialCharactersInTheNameItselfAreMatchedLiterally() {
        persistTrainee("Jo_hn", "Smith", "Jo_hn.Smith");
        persistTrainee("Jo%hn", "Smith", "Jo%hn.Smith");

        assertEquals(Set.of("Jo_hn.Smith"), userDao.findUsernamesStartingWith("Jo_hn"));
        assertEquals(Set.of("Jo%hn.Smith"), userDao.findUsernamesStartingWith("Jo%hn"));
    }
}
