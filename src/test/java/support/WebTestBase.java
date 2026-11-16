package support;

import io.gymcrm.Application;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
public abstract class WebTestBase {

    @Autowired
    private DataSource dataSource;

    @Autowired
    protected MockMvc mvc;

    protected String john;
    protected String anna;
    protected String david;

    @BeforeEach
    void setUpUsers() throws Exception {
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

    protected String register(String url, String json) throws Exception {
        MvcResult result = mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        String username = body.replaceAll(".*\"username\":\"([^\"]+)\".*", "$1");
        String password = body.replaceAll(".*\"password\":\"([^\"]+)\".*", "$1");
        return basic(username, password);
    }

    protected static String basic(String username, String password) {
        return "Basic " + Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    protected static String passwordOf(String basicHeader) {
        String decoded = new String(Base64.getDecoder().decode(basicHeader.substring("Basic ".length())),
                StandardCharsets.UTF_8);
        return decoded.substring(decoded.indexOf(':') + 1);
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
