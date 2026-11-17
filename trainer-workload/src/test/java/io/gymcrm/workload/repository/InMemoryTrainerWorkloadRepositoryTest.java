package io.gymcrm.workload.repository;

import io.gymcrm.workload.model.TrainerWorkload;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryTrainerWorkloadRepositoryTest {

    private static final YearMonth NOV_2026 = YearMonth.of(2026, 11);

    private final InMemoryTrainerWorkloadRepository repository = new InMemoryTrainerWorkloadRepository();

    private TrainerWorkload addMinutes(TrainerWorkload current, int minutes) {
        TrainerWorkload workload = current != null ? current : new TrainerWorkload("Anna.Lee", "Anna", "Lee", true);
        workload.addMinutes(NOV_2026, minutes);
        return workload;
    }

    @Test
    void storesAndFinds() {
        repository.update("Anna.Lee", current -> addMinutes(current, 60));

        assertEquals(60, repository.findByUsername("Anna.Lee").orElseThrow().minutesIn(NOV_2026));
        assertTrue(repository.findByUsername("David.Brown").isEmpty());
    }

    @Test
    void updateGetsTheCurrentValue() {
        repository.update("Anna.Lee", current -> addMinutes(current, 60));
        TrainerWorkload stored = repository.update("Anna.Lee", current -> addMinutes(current, 30));

        assertEquals(90, stored.minutesIn(NOV_2026));
    }

    @Test
    void returningNullStoresNothing() {
        assertNull(repository.update("Anna.Lee", current -> null));
        assertTrue(repository.findByUsername("Anna.Lee").isEmpty());
    }

    @Test
    void changingReturnedObjectsDoesNotChangeStoredData() {
        TrainerWorkload stored = repository.update("Anna.Lee", current -> addMinutes(current, 60));
        stored.addMinutes(NOV_2026, 1000);
        repository.findByUsername("Anna.Lee").orElseThrow().addMinutes(NOV_2026, 1000);

        assertEquals(60, repository.findByUsername("Anna.Lee").orElseThrow().minutesIn(NOV_2026));
    }

    @Test
    void failedChangeKeepsPreviousValue() {
        repository.update("Anna.Lee", current -> addMinutes(current, 60));

        try {
            repository.update("Anna.Lee", current -> {
                current.addMinutes(NOV_2026, 30);
                throw new IllegalStateException("boom");
            });
        } catch (IllegalStateException expected) {
            // the change must not be half-applied
        }

        assertEquals(60, repository.findByUsername("Anna.Lee").orElseThrow().minutesIn(NOV_2026));
    }

    @Test
    void concurrentUpdatesOfOneTrainerAreNotLost() throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        for (int i = 0; i < 1000; i++) {
            pool.execute(() -> repository.update("Anna.Lee", current -> addMinutes(current, 1)));
        }
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));

        assertEquals(1000, repository.findByUsername("Anna.Lee").orElseThrow().minutesIn(NOV_2026));
    }
}
