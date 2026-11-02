package support;

import io.gymcrm.entities.Trainee;
import io.gymcrm.entities.Trainer;
import io.gymcrm.entities.TrainingType;
import io.gymcrm.entities.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public final class TestData {

    public static final TrainingType YOGA = new TrainingType(6L, "Yoga");
    /** Real BCrypt with the lowest strength, so unit tests check real hashing but stay fast. */
    public static final PasswordEncoder ENCODER = new BCryptPasswordEncoder(4);
    public static final TrainingType FITNESS = new TrainingType(1L, "Fitness");

    private TestData() {
    }

    public static Trainee trainee(String username, String password) {
        Trainee trainee = new Trainee();
        trainee.setUser(new User("John", "Smith", username, password, true));
        return trainee;
    }

    public static Trainer trainer(String username, String password, TrainingType specialization) {
        Trainer trainer = new Trainer();
        trainer.setUser(new User("Anna", "Lee", username, password, true));
        trainer.setSpecialization(specialization);
        return trainer;
    }
}
