package io.gymcrm.entities;

import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

@NoArgsConstructor
@ToString(callSuper = true)
public class Trainer extends User {

    private Set<TrainingType> specialization = EnumSet.noneOf(TrainingType.class);

    public Set<TrainingType> getSpecialization() {
        return Collections.unmodifiableSet(specialization);
    }

    public void setSpecialization(Collection<TrainingType> specialization) {
        this.specialization = EnumSet.noneOf(TrainingType.class);
        if (specialization != null) {
            this.specialization.addAll(specialization);
        }
    }

    public void addSpecialization(TrainingType type) {
        specialization.add(Objects.requireNonNull(type));
    }

    public void removeSpecialization(TrainingType type) {
        specialization.remove(type);
    }
}
