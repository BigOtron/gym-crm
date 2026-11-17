package io.gymcrm.workload.model;

import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrainerWorkloadTest {

    private static final YearMonth NOV_2026 = YearMonth.of(2026, 11);
    private static final YearMonth DEC_2026 = YearMonth.of(2026, 12);
    private static final YearMonth JAN_2027 = YearMonth.of(2027, 1);

    private final TrainerWorkload workload = new TrainerWorkload("Anna.Lee", "Anna", "Lee", true);

    @Test
    void addsMinutesPerMonth() {
        workload.addMinutes(NOV_2026, 60);
        workload.addMinutes(NOV_2026, 45);
        workload.addMinutes(DEC_2026, 30);

        assertEquals(105, workload.minutesIn(NOV_2026));
        assertEquals(30, workload.minutesIn(DEC_2026));
        assertEquals(0, workload.minutesIn(JAN_2027));
    }

    @Test
    void groupsByYearThenMonthInAscendingOrder() {
        workload.addMinutes(JAN_2027, 10);
        workload.addMinutes(DEC_2026, 20);
        workload.addMinutes(NOV_2026, 30);

        assertEquals(Map.of(2026, Map.of(11, 30, 12, 20), 2027, Map.of(1, 10)), workload.getMinutesByYear());
        assertEquals(List.of(2026, 2027), List.copyOf(workload.getMinutesByYear().keySet()));
        assertEquals(List.of(11, 12), List.copyOf(workload.getMinutesByYear().get(2026).keySet()));
    }

    @Test
    void removesMinutes() {
        workload.addMinutes(NOV_2026, 100);

        assertEquals(40, workload.removeMinutes(NOV_2026, 40));
        assertEquals(60, workload.minutesIn(NOV_2026));
    }

    @Test
    void neverRemovesMoreThanTheMonthHas() {
        workload.addMinutes(NOV_2026, 30);

        assertEquals(30, workload.removeMinutes(NOV_2026, 50));
        assertEquals(0, workload.minutesIn(NOV_2026));
    }

    @Test
    void removingFromAnUnknownMonthRemovesNothing() {
        workload.addMinutes(NOV_2026, 30);

        assertEquals(0, workload.removeMinutes(DEC_2026, 30));
        assertEquals(0, workload.removeMinutes(JAN_2027, 30));
        assertEquals(30, workload.minutesIn(NOV_2026));
    }

    @Test
    void dropsEmptyMonthsAndYears() {
        workload.addMinutes(NOV_2026, 30);
        workload.addMinutes(DEC_2026, 20);

        workload.removeMinutes(NOV_2026, 30);
        assertEquals(Map.of(2026, Map.of(12, 20)), workload.getMinutesByYear());

        workload.removeMinutes(DEC_2026, 20);
        assertTrue(workload.getMinutesByYear().isEmpty());
    }

    @Test
    void rejectsNonPositiveMinutes() {
        assertThrows(IllegalArgumentException.class, () -> workload.addMinutes(NOV_2026, 0));
        assertThrows(IllegalArgumentException.class, () -> workload.removeMinutes(NOV_2026, -5));
    }

    @Test
    void updatesTrainerDetails() {
        workload.updateTrainer("Annie", "Leigh", false);

        assertEquals("Anna.Lee", workload.getUsername());
        assertEquals("Annie", workload.getFirstName());
        assertEquals("Leigh", workload.getLastName());
        assertFalse(workload.isActive());
    }

    @Test
    void copyIsIndependent() {
        workload.addMinutes(NOV_2026, 30);
        TrainerWorkload copy = workload.copy();

        copy.addMinutes(NOV_2026, 15);
        copy.updateTrainer("Annie", "Leigh", false);

        assertEquals(30, workload.minutesIn(NOV_2026));
        assertEquals("Anna", workload.getFirstName());
        assertEquals(45, copy.minutesIn(NOV_2026));
    }

    @Test
    void viewIsReadOnly() {
        workload.addMinutes(NOV_2026, 30);

        assertThrows(UnsupportedOperationException.class, () -> workload.getMinutesByYear().clear());
        assertThrows(UnsupportedOperationException.class, () -> workload.getMinutesByYear().get(2026).put(12, 5));
    }

    @Test
    void requiresUsername() {
        assertThrows(NullPointerException.class, () -> new TrainerWorkload(null, "Anna", "Lee", true));
    }
}
