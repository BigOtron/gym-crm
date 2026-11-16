package web.controller.unit;

import io.gymcrm.dto.Credentials;
import io.gymcrm.dto.IssuedToken;
import io.gymcrm.exceptions.AuthenticationException;
import io.gymcrm.exceptions.LoginBlockedException;
import io.gymcrm.facade.GymFacade;
import io.gymcrm.web.controller.AuthController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static support.ControllerTestSupport.mockMvc;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private static final String LOGIN = """
            {"username":"John.Smith","password":"secret"}""";
    private static final Credentials JOHN = new Credentials("John.Smith", "secret");

    @Mock
    private GymFacade facade;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = mockMvc(new AuthController(facade));
    }

    // Login

    @Test
    void loginReturnsTheToken() throws Exception {
        when(facade.login(JOHN)).thenReturn(new IssuedToken("jwt-token", 3600));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"token":"jwt-token","tokenType":"Bearer","expiresIn":3600}""", true));
    }

    @Test
    void loginWithWrongCredentials() throws Exception {
        when(facade.login(any())).thenThrow(new AuthenticationException("Invalid username or password"));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.detail").value("Invalid username or password"));
    }

    @Test
    void blockedLogin() throws Exception {
        when(facade.login(any())).thenThrow(new LoginBlockedException(Duration.ofMinutes(5)));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "300"));
    }

    @Test
    void loginWithMissingFields() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":" "}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username").value("must not be blank"))
                .andExpect(jsonPath("$.errors.password").value("must not be blank"));

        verifyNoInteractions(facade);
    }

    // Change password

    @Test
    void changePassword() throws Exception {
        mvc.perform(put("/api/auth/password").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":"John.Smith","oldPassword":"secret","newPassword":"brandNew1"}"""))
                .andExpect(status().isOk());

        verify(facade).changePassword("John.Smith", "secret", "brandNew1");
    }

    @Test
    void changePasswordWithWrongOldPassword() throws Exception {
        doThrow(new AuthenticationException("The old password is wrong"))
                .when(facade).changePassword(any(), any(), any());

        mvc.perform(put("/api/auth/password").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":"John.Smith","oldPassword":"wrong","newPassword":"brandNew1"}"""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changingSomeoneElsesPassword() throws Exception {
        doThrow(new AccessDeniedException("denied")).when(facade).changePassword(any(), any(), any());

        mvc.perform(put("/api/auth/password").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":"Maria.Garcia","oldPassword":"x","newPassword":"brandNew1"}"""))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("You are not allowed to do this"));
    }

    @Test
    void changePasswordWithMissingFields() throws Exception {
        mvc.perform(put("/api/auth/password").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":"John.Smith","newPassword":" "}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.oldPassword").value("must not be blank"))
                .andExpect(jsonPath("$.errors.newPassword").value("must not be blank"));

        verifyNoInteractions(facade);
    }
}
