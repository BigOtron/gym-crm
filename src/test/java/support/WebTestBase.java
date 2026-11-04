package support;

import io.gymcrm.Application;
import io.gymcrm.security.LoginAttemptService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs the whole application, security filters included, against H2. Each test starts with John (trainee),
 * Anna (trainer, Yoga) and David (trainer, Heavy lifting), each logged in with a real token.
 */
@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
public abstract class WebTestBase {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private LoginAttemptService loginAttempts;

    @Autowired
    protected MockMvc mvc;

    private final Map<String, String> passwords = new HashMap<>();

    /** "Bearer ..." Authorization header values. */
    protected String john;
    protected String anna;
    protected String david;

    @BeforeEach
    void setUpUsers() throws Exception {
        // The application context is shared between test classes, and the usernames repeat.
        loginAttempts.clear();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        for (String table : new String[]{"training", "trainee_trainer", "trainee", "trainer", "users"}) {
            jdbc.update("delete from " + table);
        }

        john = register("/api/trainees", """
                {"firstName":"John","lastName":"Smith","dateOfBirth":"1995-04-12","address":"Tashkent"}""");
        anna = register("/api/trainers", """
                {"firstName":"Anna","lastName":"Lee","specializationId":6}""");
        david = register("/api/trainers", """
                {"firstName":"David","lastName":"Brown","specializationId":3}""");
    }

    /** Registers a user and returns their Authorization header, using the token from the response. */
    protected String register(String url, String json) throws Exception {
        MvcResult result = mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        String password = body.replaceAll(".*\"password\":\"([^\"]+)\".*", "$1");
        String header = "Bearer " + body.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
        passwords.put(header, password);
        return header;
    }

    /** The generated password of a user registered in this test, given their Authorization header. */
    protected String passwordOf(String authorizationHeader) {
        return passwords.get(authorizationHeader);
    }

    protected ResultActions login(String username, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                {"username":"%s","password":"%s"}""".formatted(username, password)));
    }

    /** Logs in and returns the Authorization header with the new token. */
    protected String loginHeader(String username, String password) throws Exception {
        String body = login(username, password).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + body.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
    }

    protected static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String auth,
                                                        String body) {
        return request.header("Authorization", auth).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    protected void addTraining(String auth, String trainer, String name, String date) throws Exception {
        mvc.perform(json(post("/api/trainings"), auth, """
                        {"traineeUsername":"John.Smith","trainerUsername":"%s","trainingName":"%s",\
                        "trainingDate":"%s","trainingDuration":60}""".formatted(trainer, name, date)))
                .andExpect(status().isOk());
    }
}
