package security;

import io.gymcrm.dao.TraineeDao;
import io.gymcrm.dao.UserDao;
import io.gymcrm.entities.User;
import io.gymcrm.security.GymUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static support.TestData.trainee;

@ExtendWith(MockitoExtension.class)
class GymUserDetailsServiceTest {

    @Mock
    private UserDao userDao;

    @Mock
    private TraineeDao traineeDao;

    private GymUserDetailsService service;

    @BeforeEach
    void setUp() {
        service = new GymUserDetailsService(userDao, traineeDao);
    }

    private static List<String> authorities(UserDetails details) {
        return details.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
    }

    @Test
    void traineeGetsTheTraineeRoleAndTheStoredHash() {
        when(userDao.findByUsername("John.Smith"))
                .thenReturn(Optional.of(new User("John", "Smith", "John.Smith", "$2a$hash", true)));
        when(traineeDao.findByUsername("John.Smith")).thenReturn(Optional.of(trainee("John.Smith", "x")));

        UserDetails details = service.loadUserByUsername("John.Smith");

        assertEquals("$2a$hash", details.getPassword());
        assertEquals(List.of("ROLE_TRAINEE"), authorities(details));
    }

    @Test
    void anyoneElseIsATrainer() {
        when(userDao.findByUsername("Anna.Lee"))
                .thenReturn(Optional.of(new User("Anna", "Lee", "Anna.Lee", "$2a$hash", true)));
        when(traineeDao.findByUsername("Anna.Lee")).thenReturn(Optional.empty());

        assertEquals(List.of("ROLE_TRAINER"), authorities(service.loadUserByUsername("Anna.Lee")));
    }

    @Test
    void deactivatedUsersCanStillLogIn() {
        when(userDao.findByUsername("John.Smith"))
                .thenReturn(Optional.of(new User("John", "Smith", "John.Smith", "$2a$hash", false)));
        when(traineeDao.findByUsername("John.Smith")).thenReturn(Optional.of(trainee("John.Smith", "x")));

        assertTrue(service.loadUserByUsername("John.Smith").isEnabled());
    }

    @Test
    void unknownUser() {
        when(userDao.findByUsername("Nobody")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("Nobody"));
    }
}
