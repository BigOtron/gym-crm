package web.controller;

import io.gymcrm.config.AppConfig;
import io.gymcrm.web.config.WebConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sends real HTTP requests through the whole application (controllers, facade, services, Hibernate)
 * against H2. Every request runs in its own transaction, so lazy-loading mistakes in the mapping fail here.
 */
@SpringJUnitWebConfig(classes = {AppConfig.class, WebConfig.class})
class RestApiIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private DataSource dataSource;

    private MockMvc mvc;

    /** "Basic ..." header values for John (trainee), Anna (trainer, Yoga) and David (trainer, Heavy lifting). */
    private String john;
    private String anna;
    private String david;

    @BeforeEach
    void setUp() throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        for (String table : new String[]{"training", "trainee_trainer", "trainee", "trainer", "users"}) {
            jdbc.update("delete from " + table);
        }
        mvc = MockMvcBuilders.webAppContextSetup(context).build();

        john = register("/api/trainees", """
                {"firstName":"John","lastName":"Smith","dateOfBirth":"1995-04-12","address":"Tashkent"}""");
        anna = register("/api/trainers", """
                {"firstName":"Anna","lastName":"Lee","specializationId":6}""");
        david = register("/api/trainers", """
                {"firstName":"David","lastName":"Brown","specializationId":3}""");
    }

    /** Registers a user and returns their Basic auth header, built from the response. */
    private String register(String url, String json) throws Exception {
        MvcResult result = mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        String username = body.replaceAll(".*\"username\":\"([^\"]+)\".*", "$1");
        String password = body.replaceAll(".*\"password\":\"([^\"]+)\".*", "$1");
        return basic(username, password);
    }

    private static String basic(String username, String password) {
        return "Basic " + Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String auth, String body) {
        return request.header("Authorization", auth).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private void addTraining(String auth, String trainer, String name, String date) throws Exception {
        mvc.perform(json(post("/api/trainings"), auth, """
                        {"traineeUsername":"John.Smith","trainerUsername":"%s","trainingName":"%s",\
                        "trainingDate":"%s","trainingDuration":60}""".formatted(trainer, name, date)))
                .andExpect(status().isOk());
    }

    // 1, 2. Registration

    @Test
    void registrationReturnsGeneratedCredentials() throws Exception {
        mvc.perform(post("/api/trainees").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"John","lastName":"Smith"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("John.Smith1"))
                .andExpect(jsonPath("$.password").value(matchesPattern("[A-Za-z0-9]{10}")));
    }

    @Test
    void registrationValidatesRequiredFields() throws Exception {
        mvc.perform(post("/api/trainees").contentType(MediaType.APPLICATION_JSON).content("""
                        {"firstName":" ","dateOfBirth":"2999-01-01"}"""))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/trainers").contentType(MediaType.APPLICATION_JSON).content("""
                        {"firstName":"Anna","lastName":"Lee"}"""))
                .andExpect(status().isBadRequest());
    }

    // 3, 4. Login and password change

    @Test
    void login() throws Exception {
        mvc.perform(get("/api/auth/login").header("Authorization", john)).andExpect(status().isOk());
        mvc.perform(get("/api/auth/login").header("Authorization", anna)).andExpect(status().isOk());
    }

    @Test
    void changePassword() throws Exception {
        String oldPassword = new String(Base64.getDecoder().decode(anna.substring(6))).split(":", 2)[1];

        mvc.perform(put("/api/auth/password").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":"Anna.Lee","oldPassword":"%s","newPassword":"brandNew1"}""".formatted(oldPassword)))
                .andExpect(status().isOk());

        mvc.perform(get("/api/auth/login").header("Authorization", basic("Anna.Lee", "brandNew1")))
                .andExpect(status().isOk());
    }

    // 5, 6, 7. Trainee profile

    @Test
    void getTraineeProfile() throws Exception {
        addTraining(john, "Anna.Lee", "Yoga 1", "2026-09-01");

        mvc.perform(get("/api/trainees/John.Smith").header("Authorization", anna))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"username":"John.Smith","firstName":"John","lastName":"Smith","dateOfBirth":"1995-04-12",
                         "address":"Tashkent","isActive":true,
                         "trainers":[{"username":"Anna.Lee","firstName":"Anna","lastName":"Lee",
                                      "specialization":{"id":6,"trainingType":"Yoga"}}]}""", true));
    }

    @Test
    void updateTraineeProfile() throws Exception {
        mvc.perform(json(put("/api/trainees/John.Smith"), john, """
                        {"firstName":"Johnny","lastName":"Smith","address":"Samarkand","isActive":false}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("John.Smith"))
                .andExpect(jsonPath("$.firstName").value("Johnny"))
                .andExpect(jsonPath("$.dateOfBirth").isEmpty())
                .andExpect(jsonPath("$.address").value("Samarkand"))
                .andExpect(jsonPath("$.isActive").value(false))
                .andExpect(jsonPath("$.trainers").isArray());
    }

    @Test
    void updateTraineeProfileRequiresIsActive() throws Exception {
        mvc.perform(json(put("/api/trainees/John.Smith"), john, """
                        {"firstName":"Johnny","lastName":"Smith"}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteTraineeProfile() throws Exception {
        addTraining(john, "Anna.Lee", "Yoga 1", "2026-09-01");

        mvc.perform(delete("/api/trainees/John.Smith").header("Authorization", john)).andExpect(status().isOk());

        mvc.perform(get("/api/trainers/Anna.Lee/trainings").header("Authorization", anna))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // 8, 9. Trainer profile

    @Test
    void getTrainerProfile() throws Exception {
        addTraining(john, "Anna.Lee", "Yoga 1", "2026-09-01");

        mvc.perform(get("/api/trainers/Anna.Lee").header("Authorization", john))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"username":"Anna.Lee","firstName":"Anna","lastName":"Lee",
                         "specialization":{"id":6,"trainingType":"Yoga"},"isActive":true,
                         "trainees":[{"username":"John.Smith","firstName":"John","lastName":"Smith"}]}""", true));
    }

    @Test
    void updateTrainerProfileIgnoresSpecialization() throws Exception {
        mvc.perform(json(put("/api/trainers/Anna.Lee"), anna, """
                        {"firstName":"Ann","lastName":"Lee","specializationId":1,"isActive":true}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Ann"))
                .andExpect(jsonPath("$.specialization.trainingType").value("Yoga"));
    }

    // 10, 11. Trainers of a trainee

    @Test
    void availableTrainersAndUpdatingTheList() throws Exception {
        mvc.perform(get("/api/trainees/John.Smith/available-trainers").header("Authorization", john))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].username", contains("Anna.Lee", "David.Brown")));

        mvc.perform(json(put("/api/trainees/John.Smith/trainers"), john, """
                        {"trainerUsernames":["David.Brown"]}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].username", contains("David.Brown")))
                .andExpect(jsonPath("$[0].specialization.trainingType").value("Heavy lifting"));

        mvc.perform(get("/api/trainees/John.Smith/available-trainers").header("Authorization", john))
                .andExpect(jsonPath("$[*].username", contains("Anna.Lee")));
    }

    @Test
    void updateTrainersRequiresTheList() throws Exception {
        mvc.perform(json(put("/api/trainees/John.Smith/trainers"), john, "{}"))
                .andExpect(status().isBadRequest());
    }

    // 12, 13, 14. Trainings

    @Test
    void traineeTrainingsWithFilters() throws Exception {
        addTraining(john, "Anna.Lee", "Yoga 1", "2026-09-01");
        addTraining(anna, "Anna.Lee", "Yoga 2", "2026-09-15");
        addTraining(david, "David.Brown", "Lifting", "2026-09-20");

        mvc.perform(get("/api/trainees/John.Smith/trainings").header("Authorization", john))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].trainingName", contains("Yoga 1", "Yoga 2", "Lifting")));

        mvc.perform(get("/api/trainees/John.Smith/trainings").header("Authorization", john)
                        .param("from", "2026-09-10").param("trainerName", "anna").param("trainingType", "Yoga"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [{"trainingName":"Yoga 2","trainingDate":"2026-09-15","trainingType":"Yoga",
                          "trainingDuration":60,"trainerName":"Anna Lee"}]""", true));
    }

    @Test
    void trainingsRejectReversedPeriodAndBadDates() throws Exception {
        mvc.perform(get("/api/trainees/John.Smith/trainings").header("Authorization", john)
                        .param("from", "2026-09-30").param("to", "2026-09-01"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/trainers/Anna.Lee/trainings").header("Authorization", anna)
                        .param("from", "not-a-date"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void trainerTrainings() throws Exception {
        addTraining(john, "Anna.Lee", "Yoga 1", "2026-09-01");

        mvc.perform(get("/api/trainers/Anna.Lee/trainings").header("Authorization", anna)
                        .param("traineeName", "smith"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [{"trainingName":"Yoga 1","trainingDate":"2026-09-01","trainingType":"Yoga",
                          "trainingDuration":60,"traineeName":"John Smith"}]""", true));
    }

    @Test
    void addTrainingValidatesFields() throws Exception {
        mvc.perform(json(post("/api/trainings"), john, """
                        {"traineeUsername":"John.Smith","trainerUsername":"Anna.Lee","trainingName":"Yoga",
                         "trainingDate":"2026-09-01","trainingDuration":0}"""))
                .andExpect(status().isBadRequest());
    }

    // 15, 16. Activation

    @Test
    void activateAndDeactivate() throws Exception {
        mvc.perform(json(patch("/api/trainees/John.Smith/status"), john, """
                        {"isActive":false}"""))
                .andExpect(status().isOk());
        mvc.perform(json(patch("/api/trainers/Anna.Lee/status"), anna, """
                        {"isActive":false}"""))
                .andExpect(status().isOk());

        mvc.perform(get("/api/trainees/John.Smith").header("Authorization", anna))
                .andExpect(jsonPath("$.isActive").value(false));
        mvc.perform(get("/api/trainers/Anna.Lee").header("Authorization", john))
                .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    void activationRequiresIsActive() throws Exception {
        mvc.perform(json(patch("/api/trainees/John.Smith/status"), john, "{}"))
                .andExpect(status().isBadRequest());
    }

    // 17. Training types

    @Test
    void trainingTypes() throws Exception {
        mvc.perform(get("/api/training-types").header("Authorization", john))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6)))
                .andExpect(jsonPath("$[5].id").value(6))
                .andExpect(jsonPath("$[5].trainingType").value("Yoga"));
    }
}
