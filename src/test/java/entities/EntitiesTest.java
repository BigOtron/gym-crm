package entities;

import io.gymcrm.entities.Trainee;
import io.gymcrm.entities.Trainer;
import io.gymcrm.entities.Training;
import io.gymcrm.entities.TrainingType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntitiesTest {

    @Test
    void usersWithSameIdAreEqualEvenIfFieldsDiffer() {
        UUID id = UUID.randomUUID();
        Trainee first = new Trainee();
        first.setUserId(id);
        first.setAddress("Tashkent");
        Trainee second = new Trainee();
        second.setUserId(id);
        second.setAddress("Samarkand");

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void unsavedUsersAreNotEqual() {
        Trainee first = new Trainee();
        Trainee second = new Trainee();

        assertNotEquals(first, second);
        assertEquals(first, first);
    }

    @Test
    void trainerAndTraineeWithSameIdAreNotEqual() {
        UUID id = UUID.randomUUID();
        Trainee trainee = new Trainee();
        trainee.setUserId(id);
        Trainer trainer = new Trainer();
        trainer.setUserId(id);

        assertNotEquals(trainee, trainer);
    }

    @Test
    void trainingsCompareById() {
        UUID id = UUID.randomUUID();
        Training first = new Training();
        first.setTrainingId(id);
        first.setTrainingName("A");
        Training second = new Training();
        second.setTrainingId(id);
        second.setTrainingName("B");

        assertEquals(first, second);
        assertNotEquals(new Training(), new Training());
    }

    @Test
    void userToStringHidesPassword() {
        Trainee trainee = new Trainee();
        trainee.setUsername("John.Smith");
        trainee.setPassword("SecretPass");

        assertFalse(trainee.toString().contains("SecretPass"));
        assertTrue(trainee.toString().contains("John.Smith"));
    }

    @Test
    void trainerSpecializationCannotBeChangedFromOutside() {
        Trainer trainer = new Trainer();
        trainer.addSpecialization(TrainingType.YOGA);

        Set<TrainingType> view = trainer.getSpecialization();

        assertThrows(UnsupportedOperationException.class, () -> view.add(TrainingType.FITNESS));
    }

    @Test
    void trainerSetterCopiesInput() {
        Trainer trainer = new Trainer();
        List<TrainingType> input = new ArrayList<>(List.of(TrainingType.YOGA));

        trainer.setSpecialization(input);
        input.add(TrainingType.FITNESS);

        assertEquals(Set.of(TrainingType.YOGA), trainer.getSpecialization());
    }

    @Test
    void trainerSetterAcceptsNullAsEmpty() {
        Trainer trainer = new Trainer();
        trainer.addSpecialization(TrainingType.YOGA);

        trainer.setSpecialization(null);

        assertTrue(trainer.getSpecialization().isEmpty());
    }
}
