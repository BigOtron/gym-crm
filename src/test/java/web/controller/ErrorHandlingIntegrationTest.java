package web.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import support.WebTestBase;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ErrorHandlingIntegrationTest extends WebTestBase {

    private static final MediaType PROBLEM = MediaType.APPLICATION_PROBLEM_JSON;

    // 401 Unauthorized

    @Test
    void missingCredentials() throws Exception {
        mvc.perform(get("/api/trainees/John.Smith"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(content().contentType(PROBLEM))
                .andExpect(content().json("""
                        {"title":"Unauthorized","status":401,
                         "detail":"Authentication required","instance":"/api/trainees/John.Smith"}""", true));
    }

    @Test
    void wrongPassword() throws Exception {
        login("John.Smith", "wrong")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid username or password"));
    }

    @Test
    void unknownUserGetsTheSameMessageAsWrongPassword() throws Exception {
        login("Nobody.Here", "wrong")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid username or password"));
    }

    @Test
    void invalidToken() throws Exception {
        mvc.perform(get("/api/trainees/John.Smith").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(PROBLEM))
                .andExpect(jsonPath("$.detail").value("Invalid or expired token"));
    }

    @Test
    void basicAuthIsNoLongerAccepted() throws Exception {
        mvc.perform(get("/api/trainees/John.Smith").header("Authorization", "Basic Sm9objpzZWNyZXQ="))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Authentication required"));
    }

    @Test
    void changePasswordWithWrongOldPassword() throws Exception {
        mvc.perform(json(put("/api/auth/password"), john, """
                        {"username":"John.Smith","oldPassword":"wrong","newPassword":"brandNew1"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("The old password is wrong"));
    }

    @Test
    void changePasswordWithoutToken() throws Exception {
        mvc.perform(put("/api/auth/password").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":"John.Smith","oldPassword":"%s","newPassword":"brandNew1"}""".formatted(passwordOf(john))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void trainerCantUseTraineeOperations() throws Exception {
        mvc.perform(delete("/api/trainees/Anna.Lee").header("Authorization", anna))
                .andExpect(status().isForbidden());
    }

    // 403 Forbidden

    @Test
    void changingSomeoneElsesProfile() throws Exception {
        String maria = register("/api/trainees", """
                {"firstName":"Maria","lastName":"Garcia"}""");

        mvc.perform(delete("/api/trainees/John.Smith").header("Authorization", maria))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("You are not allowed to do this"));
        mvc.perform(json(patch("/api/trainers/Anna.Lee/status"), david, """
                        {"isActive":false}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    void addingSomeoneElsesTraining() throws Exception {
        mvc.perform(json(post("/api/trainings"), david, """
                        {"traineeUsername":"John.Smith","trainerUsername":"Anna.Lee","trainingName":"Yoga",
                         "trainingDate":"2026-09-01","trainingDuration":60}"""))
                .andExpect(status().isForbidden());
    }

    // 404 Not Found

    @Test
    void unknownProfile() throws Exception {
        mvc.perform(get("/api/trainers/Nobody.Here").header("Authorization", john))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Trainer not found: Nobody.Here"));
    }

    @Test
    void unknownSpecialization() throws Exception {
        mvc.perform(post("/api/trainers").contentType(MediaType.APPLICATION_JSON).content("""
                        {"firstName":"Anna","lastName":"Lee","specializationId":99}"""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Training type not found: 99"));
    }

    @Test
    void unknownTrainerInList() throws Exception {
        mvc.perform(json(put("/api/trainees/John.Smith/trainers"), john, """
                        {"trainerUsernames":["Anna.Lee","Nobody.Here"]}"""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Trainers not found: [Nobody.Here]"));
    }

    @Test
    void unknownUrl() throws Exception {
        mvc.perform(get("/api/nothing-here").header("Authorization", john))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(PROBLEM));
    }

    // 409 Conflict

    @Test
    void activatingAnActiveUser() throws Exception {
        mvc.perform(json(patch("/api/trainees/John.Smith/status"), john, """
                        {"isActive":true}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Trainee John.Smith is already active"));
    }

    // 400 Bad Request

    @Test
    void validationErrorsListEveryField() throws Exception {
        mvc.perform(post("/api/trainees").contentType(MediaType.APPLICATION_JSON).content("""
                        {"firstName":" ","dateOfBirth":"2999-01-01"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(PROBLEM))
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors.firstName").value("must not be blank"))
                .andExpect(jsonPath("$.errors.lastName").value("must not be blank"))
                .andExpect(jsonPath("$.errors.dateOfBirth").value("must be a past date"));
    }

    @Test
    void invalidQueryParameters() throws Exception {
        mvc.perform(get("/api/trainees/John.Smith/trainings").header("Authorization", john)
                        .param("from", "2026-09-30").param("to", "2026-09-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.periodValid").value("'from' must not be after 'to'"));
        mvc.perform(get("/api/trainees/John.Smith/trainings").header("Authorization", john)
                        .param("from", "yesterday"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.from").value("invalid value 'yesterday'"));
    }

    @Test
    void malformedJson() throws Exception {
        mvc.perform(json(put("/api/trainees/John.Smith"), john, "{\"firstName\": "))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(PROBLEM));
    }

    @Test
    void wrongTypeInJson() throws Exception {
        mvc.perform(json(patch("/api/trainees/John.Smith/status"), john, """
                        {"isActive":"maybe"}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void blankNewPasswordIsRejected() throws Exception {
        mvc.perform(json(put("/api/auth/password"), john, """
                        {"username":"John.Smith","oldPassword":"%s","newPassword":" "}""".formatted(passwordOf(john))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.newPassword").value("must not be blank"));
    }

    // Other HTTP errors handled by Spring's base class

    @Test
    void wrongMethod() throws Exception {
        mvc.perform(delete("/api/trainers/Anna.Lee").header("Authorization", anna))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentType(PROBLEM));
    }

    @Test
    void wrongContentType() throws Exception {
        mvc.perform(post("/api/trainees").contentType(MediaType.TEXT_PLAIN).content("John Smith"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentType(PROBLEM));
    }

    @Test
    void problemBodyNeverContainsStackTraces() throws Exception {
        mvc.perform(get("/api/trainers/Nobody.Here").header("Authorization", john))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Exception"))));
    }
}
