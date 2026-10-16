package web.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import support.WebTestBase;

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

class RestApiIntegrationTest extends WebTestBase {

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
        String oldPassword = passwordOf(anna);

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
