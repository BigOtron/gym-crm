package util;

import io.gymcrm.dao.TraineeDao;
import io.gymcrm.dao.TrainerDao;
import io.gymcrm.entities.Trainee;
import io.gymcrm.entities.Trainer;
import io.gymcrm.util.UsernameGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsernameGeneratorTest {

    @Mock
    private TraineeDao traineeDao;

    @Mock
    private TrainerDao trainerDao;

    private UsernameGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new UsernameGenerator();
        generator.setTraineeDao(traineeDao);
        generator.setTrainerDao(trainerDao);
    }

    private void givenTraineeUsernames(String... usernames) {
        List<Trainee> trainees = Arrays.stream(usernames).map(username -> {
            Trainee trainee = new Trainee();
            trainee.setUsername(username);
            return trainee;
        }).toList();
        when(traineeDao.findAll()).thenReturn(trainees);
    }

    private void givenTrainerUsernames(String... usernames) {
        List<Trainer> trainers = Arrays.stream(usernames).map(username -> {
            Trainer trainer = new Trainer();
            trainer.setUsername(username);
            return trainer;
        }).toList();
        when(trainerDao.findAll()).thenReturn(trainers);
    }

    @Test
    void generatesFirstNameDotLastNameWhenFree() {
        givenTraineeUsernames();
        givenTrainerUsernames();

        assertEquals("John.Smith", generator.generate("John", "Smith"));
    }

    @Test
    void addsSerialWhenTraineeHasSameName() {
        givenTraineeUsernames("John.Smith");
        givenTrainerUsernames();

        assertEquals("John.Smith1", generator.generate("John", "Smith"));
    }

    @Test
    void addsSerialWhenTrainerHasSameName() {
        givenTraineeUsernames();
        givenTrainerUsernames("John.Smith");

        assertEquals("John.Smith1", generator.generate("John", "Smith"));
    }

    @Test
    void usesNextFreeSerial() {
        givenTraineeUsernames("John.Smith", "John.Smith1");
        givenTrainerUsernames("John.Smith2");

        assertEquals("John.Smith3", generator.generate("John", "Smith"));
    }

    @Test
    void reusesGapLeftByDeletedUser() {
        givenTraineeUsernames("John.Smith", "John.Smith2");
        givenTrainerUsernames();

        assertEquals("John.Smith1", generator.generate("John", "Smith"));
    }

    @Test
    void similarUsernamesDoNotCountAsTaken() {
        givenTraineeUsernames("John.Smithson", "Johnny.Smith");
        givenTrainerUsernames();

        assertEquals("John.Smith", generator.generate("John", "Smith"));
    }

    @Test
    void trimsWhitespaceAroundNames() {
        givenTraineeUsernames();
        givenTrainerUsernames();

        assertEquals("John.Smith", generator.generate("  John ", " Smith  "));
    }
}
