package io.gymcrm.services.implementations;

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
import io.gymcrm.services.AuthenticationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

import static io.gymcrm.services.Validation.requireText;

@Slf4j
@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private static final String FAILED = "Invalid username or password";

    private AuthenticationManager authenticationManager;
    private LoginAttemptService loginAttempts;
    private JwtTokenService tokenService;
    private UserDao userDao;
    private PasswordEncoder passwordEncoder;
    private GymMetrics metrics;

    @Autowired
    public void setAuthenticationManager(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Autowired
    public void setLoginAttempts(LoginAttemptService loginAttempts) {
        this.loginAttempts = loginAttempts;
    }

    @Autowired
    public void setTokenService(JwtTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Autowired
    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }

    @Autowired
    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Autowired
    public void setMetrics(GymMetrics metrics) {
        this.metrics = metrics;
    }

    @Override
    public IssuedToken login(Credentials credentials) {
        Objects.requireNonNull(credentials, "credentials must not be null");
        String username = credentials.username();

        // Before checking the password: while blocked, even the right password is refused.
        Optional<Duration> block = loginAttempts.remainingBlock(username);
        if (block.isPresent()) {
            log.warn("Login of {} refused, blocked for another {}", username, block.get());
            metrics.loginBlocked();
            throw new LoginBlockedException(block.get());
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(username, credentials.password()));
        } catch (org.springframework.security.core.AuthenticationException e) {
            loginAttempts.recordFailure(username);
            metrics.loginFailed();
            metrics.authenticationFailed("login");
            log.warn("Login failed for {}", username);
            // The same message for an unknown user and a wrong password.
            throw new AuthenticationException(FAILED);
        }

        loginAttempts.recordSuccess(username);
        metrics.loginSucceeded();
        log.info("User {} logged in", username);
        return tokenService.issue(username, roleOf(authentication));
    }

    @Override
    @Transactional
    @PreAuthorize("isAuthenticated() and #username == authentication.name")
    public void changePassword(String username, String oldPassword, String newPassword) {
        String password = requireText(newPassword, "New password");
        User user = userDao.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found: " + username));
        if (oldPassword == null || !passwordEncoder.matches(oldPassword, user.getPassword())) {
            log.warn("Password change rejected for {}: wrong old password", username);
            metrics.authenticationFailed("password-change");
            throw new AuthenticationException("The old password is wrong");
        }
        user.setPassword(passwordEncoder.encode(password));
        log.info("User {} changed password", username);
    }

    private static Role roleOf(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> Role.valueOf(authority.substring("ROLE_".length())))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("User has no role"));
    }
}
