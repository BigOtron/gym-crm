package io.gymcrm.workload.web;

import io.gymcrm.workload.model.TrainerWorkload;
import io.gymcrm.workload.service.TrainerNotFoundException;
import io.gymcrm.workload.service.TrainerWorkloadService;
import io.gymcrm.workload.web.error.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.YearMonth;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The controller and the error handler alone; security is in WorkloadApiIntegrationTest. */
@ExtendWith(MockitoExtension.class)
class TrainerWorkloadControllerTest {

    @Mock
    private TrainerWorkloadService service;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new TrainerWorkloadController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
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
        when(service.getWorkload("Anna.Lee")).thenThrow(new NullPointerException("secret internals"));

        mvc.perform(get("/api/trainer-workloads/Anna.Lee"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("Unexpected error, please try again later"));
    }
}
