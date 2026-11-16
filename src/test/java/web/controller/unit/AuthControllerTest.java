package web.controller.unit;

import io.gymcrm.dto.Credentials;
import io.gymcrm.exceptions.AuthenticationException;
import io.gymcrm.facade.GymFacade;
import io.gymcrm.web.controller.AuthController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static support.ControllerTestSupport.JOHN;
import static support.ControllerTestSupport.basic;
import static support.ControllerTestSupport.mockMvc;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private GymFacade facade;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = mockMvc(new AuthController(facade));
    }

    // Login

    @Test
    void loginWithValidCredentials() throws Exception {
        mvc.perform(get("/api/auth/login").header("Authorization", basic(JOHN)))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(facade).login(JOHN);
    }

    @Test
    void loginWithWrongCredentials() throws Exception {
        doThrow(new AuthenticationException("Invalid username or password")).when(facade).login(JOHN);

        mvc.perform(get("/api/auth/login").header("Authorization", basic(JOHN)))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Basic realm=\"gym-crm\""))
                .andExpect(jsonPath("$.detail").value("Invalid username or password"));
    }

    @Test
    void loginWithoutHeader() throws Exception {
        mvc.perform(get("/api/auth/login"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Authentication required"));

        verifyNoInteractions(facade);
    }

    // Change password

    @Test
    void changePassword() throws Exception {
        mvc.perform(put("/api/auth/password").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":"John.Smith","oldPassword":"secret","newPassword":"brandNew1"}"""))
                .andExpect(status().isOk());

        verify(facade).changePassword(new Credentials("John.Smith", "secret"), "brandNew1");
    }

    @Test
    void changePasswordWithWrongOldPassword() throws Exception {
        doThrow(new AuthenticationException("Invalid username or password"))
                .when(facade).changePassword(any(), any());

        mvc.perform(put("/api/auth/password").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":"John.Smith","oldPassword":"wrong","newPassword":"brandNew1"}"""))
                .andExpect(status().isUnauthorized());
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

    @Test
    void changePasswordWithMalformedJson() throws Exception {
        mvc.perform(put("/api/auth/password").contentType(MediaType.APPLICATION_JSON).content("{\"username\":"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(facade);
    }
}
