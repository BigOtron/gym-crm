package io.gymcrm.workload.service;

import io.gymcrm.workload.model.ActionType;
import io.gymcrm.workload.model.TrainerWorkload;
import io.gymcrm.workload.repository.InMemoryTrainerWorkloadRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;

import static io.gymcrm.workload.model.ActionType.ADD;
import static io.gymcrm.workload.model.ActionType.DELETE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The in-memory repository is a plain object, so it's used as is rather than mocked. */
class TrainerWorkloadServiceImplTest {

    private static final YearMonth NOV_2026 = YearMonth.of(2026, 11);

    private final InMemoryTrainerWorkloadRepository repository = new InMemoryTrainerWorkloadRepository();
    private final TrainerWorkloadServiceImpl service = new TrainerWorkloadServiceImpl(repository);

    private static WorkloadUpdate anna(String date, int minutes, ActionType action) {
        return new WorkloadUpdate("Anna.Lee", "Anna", "Lee", true, LocalDate.parse(date), minutes, action);
    }

    @Test
    void addCreatesTheTrainerAndSumsPerMonth() {
        service.apply(anna("2026-11-03", 60, ADD));
        service.apply(anna("2026-11-20", 45, ADD));
        service.apply(anna("2026-12-01", 30, ADD));

        TrainerWorkload workload = service.getWorkload("Anna.Lee");
        assertEquals("Anna", workload.getFirstName());
        assertEquals("Lee", workload.getLastName());
        assertTrue(workload.isActive());
        assertEquals(Map.of(2026, Map.of(11, 105, 12, 30)), workload.getMinutesByYear());
    }

    @Test
    void deleteRemovesTheTrainingsMinutes() {
        service.apply(anna("2026-11-03", 60, ADD));
        service.apply(anna("2026-11-20", 45, ADD));

        service.apply(anna("2026-11-20", 45, DELETE));

        assertEquals(60, service.getMinutes("Anna.Lee", NOV_2026));
    }

    @Test
    void deleteOfTheLastTrainingKeepsTheTrainerWithoutMonths() {
        service.apply(anna("2026-11-03", 60, ADD));

        service.apply(anna("2026-11-03", 60, DELETE));

        assertTrue(service.getWorkload("Anna.Lee").getMinutesByYear().isEmpty());
        assertEquals(0, service.getMinutes("Anna.Lee", NOV_2026));
    }

    @Test
    void deleteForUnknownTrainerChangesNothing() {
        service.apply(anna("2026-11-03", 60, DELETE));

        assertThrows(TrainerNotFoundException.class, () -> service.getWorkload("Anna.Lee"));
    }

    @Test
    void deleteNeverGoesBelowZero() {
        service.apply(anna("2026-11-03", 30, ADD));

        service.apply(anna("2026-11-03", 60, DELETE));

        assertEquals(0, service.getMinutes("Anna.Lee", NOV_2026));
    }

    @Test
    void latestEventUpdatesNameAndStatus() {
        service.apply(anna("2026-11-03", 30, ADD));

        service.apply(new WorkloadUpdate("Anna.Lee", "Annie", "Leigh", false, LocalDate.parse("2026-12-01"), 15, ADD));

        TrainerWorkload workload = service.getWorkload("Anna.Lee");
        assertEquals("Annie", workload.getFirstName());
        assertEquals("Leigh", workload.getLastName());
        assertFalse(workload.isActive());
    }

    @Test
    void trainersAreKeptApart() {
        service.apply(anna("2026-11-03", 30, ADD));
        service.apply(new WorkloadUpdate("David.Brown", "David", "Brown", true, LocalDate.parse("2026-11-03"), 90, ADD));

        assertEquals(30, service.getMinutes("Anna.Lee", NOV_2026));
        assertEquals(90, service.getMinutes("David.Brown", NOV_2026));
    }

    @Test
    void unknownTrainer() {
        assertThrows(TrainerNotFoundException.class, () -> service.getWorkload("Nobody"));
        assertThrows(TrainerNotFoundException.class, () -> service.getMinutes("Nobody", NOV_2026));
    }

    @Test
    void rejectsMissingUpdate() {
        assertThrows(NullPointerException.class, () -> service.apply(null));
    }
}
