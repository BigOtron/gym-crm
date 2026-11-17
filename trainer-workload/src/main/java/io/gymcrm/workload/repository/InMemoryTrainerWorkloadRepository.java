package io.gymcrm.workload.repository;

import io.gymcrm.workload.model.TrainerWorkload;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.UnaryOperator;

/**
 * The in-memory database: everything is lost when the service stops. ConcurrentHashMap.compute locks
 * only the one trainer being changed, so events for different trainers are processed in parallel.
 */
@Repository
public class InMemoryTrainerWorkloadRepository implements TrainerWorkloadRepository {

    private final ConcurrentMap<String, TrainerWorkload> workloads = new ConcurrentHashMap<>();

    @Override
    public Optional<TrainerWorkload> findByUsername(String username) {
        return Optional.ofNullable(workloads.get(username)).map(TrainerWorkload::copy);
    }

    @Override
    public TrainerWorkload update(String username, UnaryOperator<TrainerWorkload> change) {
        // The stored object is never changed in place: readers either see the old one or the new one.
        TrainerWorkload stored = workloads.compute(username,
                (key, current) -> change.apply(current == null ? null : current.copy()));
        return stored == null ? null : stored.copy();
    }
}
