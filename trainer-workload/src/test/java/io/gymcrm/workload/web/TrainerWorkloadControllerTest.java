package io.gymcrm.workload.web;

import io.gymcrm.workload.model.TrainerWorkload;
import io.gymcrm.workload.service.TrainerNotFoundException;
import io.gymcrm.workload.service.TrainerWorkloadService;
import io.gymcrm.workload.service.WorkloadUpdate;
import io.gymcrm.workload.web.error.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.YearMonth;

import static io.gymcrm.workload.model.ActionType.ADD;
import static io.gymcrm.workload.model.ActionType.DELETE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The controller and the error handler alone; security is in WorkloadApiIntegrationTest. */
@ExtendWith(MockitoExtension.class)
class TrainerWorkloadControllerTest {

    private static final String VALID = """
            {"trainerUsername":"Anna.Lee","trainerFirstName":"Anna","trainerLastName":"Lee","isActive":true,
             "trainingDate":"2026-11-20","trainingDuration":60,"actionType":"ADD"}""";

    @Mock
    private TrainerWorkloadService service;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new TrainerWorkloadController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static MockHttpServletRequestBuilder report(String body) {
        return post("/api/trainer-workloads").contentType(MediaType.APPLICATION_JSON).content(body);
    }

    @Test
    void acceptsAddedTraining() throws Exception {
        mvc.perform(report(VALID))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(service).apply(new WorkloadUpdate("Anna.Lee", "Anna", "Lee", true, LocalDate.of(2026, 11, 20), 60, ADD));
    }

    @Test
    void acceptsDeletedTraining() throws Exception {
        mvc.perform(report(VALID.replace("\"ADD\"", "\"DELETE\"").replace("true", "false")))
                .andExpect(status().isOk());

        verify(service).apply(new WorkloadUpdate("Anna.Lee", "Anna", "Lee", false, LocalDate.of(2026, 11, 20), 60, DELETE));
    }

    @Test
    void rejectsMissingAndInvalidFields() throws Exception {
        mvc.perform(report("""
                        {"trainerUsername":" ","trainingDuration":0}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors.trainerUsername").exists())
                .andExpect(jsonPath("$.errors.trainerFirstName").exists())
                .andExpect(jsonPath("$.errors.trainerLastName").exists())
                .andExpect(jsonPath("$.errors.isActive").exists())
                .andExpect(jsonPath("$.errors.trainingDate").exists())
                .andExpect(jsonPath("$.errors.trainingDuration").exists())
                .andExpect(jsonPath("$.errors.actionType").exists());

        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"actionType\":\"ADD\"", "\"trainingDate\":\"2026-11-20\""})
    void rejectsUnreadableValues(String field) throws Exception {
        String broken = field.startsWith("\"actionType")
                ? VALID.replace(field, "\"actionType\":\"UPDATE\"")
                : VALID.replace(field, "\"trainingDate\":\"20.11.2026\"");

        mvc.perform(report(broken)).andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void rejectsBrokenJson() throws Exception {
        mvc.perform(report("{")).andExpect(status().isBadRequest());
    }

    @Test
    void returnsWorkloadGroupedByYearAndMonth() throws Exception {
        TrainerWorkload workload = new TrainerWorkload("Anna.Lee", "Anna", "Lee", true);
        workload.addMinutes(YearMonth.of(2026, 12), 30);
        workload.addMinutes(YearMonth.of(2026, 11), 105);
        workload.addMinutes(YearMonth.of(2027, 1), 60);
        when(service.getWorkload("Anna.Lee")).thenReturn(workload);

        mvc.perform(get("/api/trainer-workloads/Anna.Lee"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainerUsername").value("Anna.Lee"))
                .andExpect(jsonPath("$.trainerFirstName").value("Anna"))
                .andExpect(jsonPath("$.trainerLastName").value("Lee"))
                .andExpect(jsonPath("$.trainerStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.years[0].year").value(2026))
                .andExpect(jsonPath("$.years[0].months[0].month").value(11))
                .andExpect(jsonPath("$.years[0].months[0].trainingSummaryDuration").value(105))
                .andExpect(jsonPath("$.years[0].months[1].month").value(12))
                .andExpect(jsonPath("$.years[1].year").value(2027))
                .andExpect(jsonPath("$.years[1].months[0].trainingSummaryDuration").value(60));
    }

    @Test
    void inactiveTrainer() throws Exception {
        when(service.getWorkload("Anna.Lee")).thenReturn(new TrainerWorkload("Anna.Lee", "Anna", "Lee", false));

        mvc.perform(get("/api/trainer-workloads/Anna.Lee"))
                .andExpect(jsonPath("$.trainerStatus").value("INACTIVE"))
                .andExpect(jsonPath("$.years").isEmpty());
    }

    @Test
    void returnsOneMonth() throws Exception {
        when(service.getMinutes("Anna.Lee", YearMonth.of(2026, 11))).thenReturn(105);

        mvc.perform(get("/api/trainer-workloads/Anna.Lee/years/2026/months/11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainerUsername").value("Anna.Lee"))
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.month").value(11))
                .andExpect(jsonPath("$.trainingSummaryDuration").value(105));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/trainer-workloads/Anna.Lee/years/2026/months/13",
            "/api/trainer-workloads/Anna.Lee/years/2026/months/0",
            "/api/trainer-workloads/Anna.Lee/years/0/months/5",
            "/api/trainer-workloads/Anna.Lee/years/2026/months/may"})
    void rejectsInvalidMonth(String url) throws Exception {
        mvc.perform(get(url)).andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void unknownTrainerIsNotFound() throws Exception {
        when(service.getWorkload("Nobody")).thenThrow(new TrainerNotFoundException("Nobody"));

        mvc.perform(get("/api/trainer-workloads/Nobody"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No workload found for trainer Nobody"));
    }

    @Test
    void unexpectedErrorHidesDetails() throws Exception {
        doThrow(new NullPointerException("secret internals")).when(service).apply(any());

        mvc.perform(report(VALID))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("Unexpected error, please try again later"));
    }
}
