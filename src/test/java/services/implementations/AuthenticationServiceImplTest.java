package services.implementations;

import io.gymcrm.dao.UserDao;
import io.gymcrm.dto.Credentials;
import io.gymcrm.dto.IssuedToken;
import io.gymcrm.entities.User;
import io.gymcrm.exceptions.AuthenticationException;
import io.gymcrm.exceptions.LoginBlockedException;
import io.gymcrm.exceptions.NotFoundException;
import io.gymcrm.metrics.GymMetrics;
import io.gymcrm.security.JwtTokenService;
import io.gymcrm.security.LoginAttemptService;
import io.gymcrm.security.Role;
import io.gymcrm.services.implementations.AuthenticationServiceImpl;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import support.TestData;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {

    private static final Credentials JOHN = new Credentials("John.Smith", "secret");

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private LoginAttemptService loginAttempts;

    @Mock
    private JwtTokenService tokenService;

    @Mock
    private UserDao userDao;

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

    private AuthenticationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AuthenticationServiceImpl();
        service.setAuthenticationManager(authenticationManager);
        service.setLoginAttempts(loginAttempts);
        service.setTokenService(tokenService);
        service.setUserDao(userDao);
        service.setPasswordEncoder(TestData.ENCODER);
        service.setMetrics(new GymMetrics(registry));
    }

    private double logins(String result) {
        return registry.get(GymMetrics.LOGINS).tag("result", result).counter().count();
    }

    // Login

    @Test
    void successfulLoginIssuesATokenForTheUsersRole() {
        IssuedToken token = new IssuedToken("jwt", 3600);
        when(loginAttempts.remainingBlock("John.Smith")).thenReturn(Optional.empty());
        when(authenticationManager.authenticate(any()))
                .thenReturn(new TestingAuthenticationToken("John.Smith", null, "ROLE_TRAINEE"));
        when(tokenService.issue("John.Smith", Role.TRAINEE)).thenReturn(token);

        assertSame(token, service.login(JOHN));

        verify(loginAttempts).recordSuccess("John.Smith");
        assertEquals(1, logins("success"));
    }

    @Test
    void wrongPasswordIsRecordedAndGetsTheGenericMessage() {
        when(loginAttempts.remainingBlock("John.Smith")).thenReturn(Optional.empty());
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        AuthenticationException e = assertThrows(AuthenticationException.class, () -> service.login(JOHN));

        assertEquals("Invalid username or password", e.getMessage());
        verify(loginAttempts).recordFailure("John.Smith");
        verify(loginAttempts, never()).recordSuccess(any());
        verifyNoInteractions(tokenService);
        assertEquals(1, logins("failure"));
        assertEquals(1, registry.get(GymMetrics.AUTHENTICATION_FAILURES).tag("source", "login").counter().count());
    }

    @Test
    void blockedUsernameIsRefusedWithoutCheckingThePassword() {
        when(loginAttempts.remainingBlock("John.Smith")).thenReturn(Optional.of(Duration.ofMinutes(4)));

        LoginBlockedException e = assertThrows(LoginBlockedException.class, () -> service.login(JOHN));

        assertEquals(Duration.ofMinutes(4), e.getRetryAfter());
        verifyNoInteractions(authenticationManager, tokenService);
        assertEquals(1, logins("blocked"));
    }

    @Test
    void nullCredentialsAreRejected() {
        assertThrows(NullPointerException.class, () -> service.login(null));
        verifyNoInteractions(authenticationManager);
    }

    // Change password

    private User givenJohn() {
        User user = new User("John", "Smith", "John.Smith", TestData.ENCODER.encode("old-password"), true);
        when(userDao.findByUsername("John.Smith")).thenReturn(Optional.of(user));
        return user;
    }

    @Test
    void changePasswordStoresAHashOfTheNewPassword() {
        User user = givenJohn();

        service.changePassword("John.Smith", "old-password", "new-password");

        assertTrue(TestData.ENCODER.matches("new-password", user.getPassword()));
    }

    @Test
    void wrongOldPasswordChangesNothing() {
        User user = givenJohn();
        String before = user.getPassword();

        assertThrows(AuthenticationException.class,
                () -> service.changePassword("John.Smith", "wrong", "new-password"));

        assertEquals(before, user.getPassword());
    }

    @Test
    void missingOldPasswordChangesNothing() {
        User user = givenJohn();
        String before = user.getPassword();

        assertThrows(AuthenticationException.class, () -> service.changePassword("John.Smith", null, "new-password"));

        assertEquals(before, user.getPassword());
    }

    @Test
    void blankNewPasswordIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.changePassword("John.Smith", "old-password", " "));
        verifyNoInteractions(userDao);
    }

    @Test
    void userDeletedAfterTheTokenWasIssued() {
        when(userDao.findByUsername("John.Smith")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.changePassword("John.Smith", "x", "new-password"));
    }
}
