package integration.workload;

import io.gymcrm.integration.workload.DisabledTrainerWorkloadClient;
import io.gymcrm.integration.workload.DisabledTrainerWorkloadPublisher;
import io.gymcrm.integration.workload.TrainerWorkloadEvent;
import io.gymcrm.integration.workload.WorkloadAction;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisabledWorkloadIntegrationTest {

    @Test
    void reportsNothingAndKnowsNothing() {
        assertDoesNotThrow(() -> new DisabledTrainerWorkloadPublisher().publish(new TrainerWorkloadEvent("Anna.Lee",
                "Anna", "Lee", true, LocalDate.of(2026, 11, 20), 60, WorkloadAction.ADD)));
        assertTrue(new DisabledTrainerWorkloadClient().findWorkload("Anna.Lee").isEmpty());
    }
}
