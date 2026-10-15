package support;

import io.gymcrm.config.AppConfig;
import io.gymcrm.entities.Trainee;
import io.gymcrm.entities.Trainer;
import io.gymcrm.entities.Training;
import io.gymcrm.entities.TrainingType;
import io.gymcrm.entities.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;


@SpringJUnitConfig(AppConfig.class)
@Transactional
public abstract class DaoTestBase {

    @PersistenceContext
    protected EntityManager entityManager;

    @BeforeEach
    void emptyTables() {
        for (String table : new String[]{"training", "trainee_trainer", "trainee", "trainer", "users"}) {
            entityManager.createNativeQuery("delete from " + table).executeUpdate();
        }
    }

    protected TrainingType type(String name) {
        return entityManager.createQuery(
                        "select t from TrainingType t where t.trainingTypeName = :name", TrainingType.class)
                .setParameter("name", name)
                .getSingleResult();
    }

    protected Trainee persistTrainee(String firstName, String lastName, String username) {
        Trainee trainee = new Trainee();
        trainee.setUser(new User(firstName, lastName, username, "password", true));
        trainee.setDateOfBirth(LocalDate.of(1995, 4, 12));
        trainee.setAddress("Tashkent");
        entityManager.persist(trainee);
        return trainee;
    }

    protected Trainer persistTrainer(String firstName, String lastName, String username, String specialization) {
        Trainer trainer = new Trainer();
        trainer.setUser(new User(firstName, lastName, username, "password", true));
        trainer.setSpecialization(type(specialization));
        entityManager.persist(trainer);
        return trainer;
    }

    protected Training persistTraining(Trainee trainee, Trainer trainer, String name, LocalDate date) {
        Training training = new Training();
        training.setTrainee(trainee);
        training.setTrainer(trainer);
        training.setTrainingName(name);
        training.setTrainingType(trainer.getSpecialization());
        training.setTrainingDate(date);
        training.setTrainingDuration(60);
        entityManager.persist(training);
        trainee.getTrainings().add(training);
        return training;
    }

    protected void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    protected long count(String table) {
        return ((Number) entityManager.createNativeQuery("select count(*) from " + table).getSingleResult())
                .longValue();
    }
}
