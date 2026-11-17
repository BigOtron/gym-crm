package io.gymcrm.workload.repository;

import io.gymcrm.workload.model.TrainerWorkload;

import java.util.Optional;
import java.util.function.UnaryOperator;

/** Where trainers' workloads are kept. */
public interface TrainerWorkloadRepository {

    /** A copy of the trainer's workload; changing it doesn't change what is stored. */
    Optional<TrainerWorkload> findByUsername(String username);

    /**
     * Changes one trainer's workload as a single atomic step, so two events for the same trainer arriving
     * at the same time can't overwrite each other. {@code change} gets a copy of the current workload, or
     * {@code null} if there is none yet, and returns what to store ({@code null} stores nothing).
     *
     * @return what was stored, or {@code null}
     */
    TrainerWorkload update(String username, UnaryOperator<TrainerWorkload> change);
}
