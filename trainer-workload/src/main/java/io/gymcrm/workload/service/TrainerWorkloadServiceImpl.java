package io.gymcrm.workload.service;

import io.gymcrm.workload.model.TrainerWorkload;
import io.gymcrm.workload.repository.TrainerWorkloadRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class TrainerWorkloadServiceImpl implements TrainerWorkloadService {

    private final TrainerWorkloadRepository repository;

    public TrainerWorkloadServiceImpl(TrainerWorkloadRepository repository) {
        this.repository = repository;
    }

    @Override
    public void apply(WorkloadUpdate update) {
        Objects.requireNonNull(update, "update must not be null");
        YearMonth month = YearMonth.from(update.trainingDate());
        switch (update.actionType()) {
            case ADD -> add(update, month);
            case DELETE -> delete(update, month);
        }
    }

    private void add(WorkloadUpdate update, YearMonth month) {
        TrainerWorkload stored = repository.update(update.trainerUsername(), current -> {
            TrainerWorkload workload = current != null ? current : new TrainerWorkload(update.trainerUsername(),
                    update.trainerFirstName(), update.trainerLastName(), update.active());
            workload.updateTrainer(update.trainerFirstName(), update.trainerLastName(), update.active());
            workload.addMinutes(month, update.trainingDurationMinutes());
            return workload;
        });
        log.info("Added {} min to trainer {} for {}, month total {} min", update.trainingDurationMinutes(),
                update.trainerUsername(), month, stored.minutesIn(month));
    }

    private void delete(WorkloadUpdate update, YearMonth month) {
        AtomicInteger removed = new AtomicInteger();
        TrainerWorkload stored = repository.update(update.trainerUsername(), current -> {
            if (current == null) {
                return null;
            }
            current.updateTrainer(update.trainerFirstName(), update.trainerLastName(), update.active());
            removed.set(current.removeMinutes(month, update.trainingDurationMinutes()));
            return current;
        });

        if (stored == null) {
            log.warn("Nothing to delete: no workload for trainer {}", update.trainerUsername());
            return;
        }
        if (removed.get() < update.trainingDurationMinutes()) {
            // The ADD for this training never arrived here (e.g. it happened while this service was down).
            log.warn("Trainer {} had only {} of {} min in {} to delete", update.trainerUsername(), removed.get(),
                    update.trainingDurationMinutes(), month);
        }
        log.info("Removed {} min from trainer {} for {}, month total {} min", removed.get(),
                update.trainerUsername(), month, stored.minutesIn(month));
    }

    @Override
    public TrainerWorkload getWorkload(String trainerUsername) {
        log.debug("Selecting workload of trainer {}", trainerUsername);
        return repository.findByUsername(trainerUsername).orElseThrow(() -> {
            log.warn("No workload found for trainer {}", trainerUsername);
            return new TrainerNotFoundException(trainerUsername);
        });
    }

    @Override
    public int getMinutes(String trainerUsername, YearMonth month) {
        return getWorkload(trainerUsername).minutesIn(month);
    }
}
