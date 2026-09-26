package io.gymcrm.entities;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TrainingType {
    FITNESS("Fitness"),
    RUNNING("Running"),
    HEAVY_LIFTING("Heavy lifting"),
    PULL_UPS("Pull ups"),
    PUSH_UPS("Push ups"),
    YOGA("Yoga");

    private final String trainingTypeName;
}
