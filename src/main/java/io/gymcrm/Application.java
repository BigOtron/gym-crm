package io.gymcrm;

import io.gymcrm.config.AppConfig;
import io.gymcrm.entities.Trainee;
import io.gymcrm.entities.Trainer;
import io.gymcrm.entities.Training;
import io.gymcrm.entities.TrainingType;
import io.gymcrm.facade.GymFacade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.time.Duration;
import java.time.LocalDate;

@Slf4j
public class Application {

    public static void main(String[] args) {
        log.info("Starting Gym CRM");
        try (var context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            GymFacade facade = context.getBean(GymFacade.class);

            log.info("Storage contains {} trainees, {} trainers, {} trainings",
                    facade.getAllTrainees().size(),
                    facade.getAllTrainers().size(),
                    facade.getAllTrainings().size());

            Trainee trainee = new Trainee();
            trainee.setFirstName("John");
            trainee.setLastName("Smith");
            trainee.setDateOfBirth(LocalDate.of(1998, 3, 14));
            trainee.setAddress("Tashkent");
            trainee = facade.createTrainee(trainee);

            Trainer trainer = new Trainer();
            trainer.setFirstName("Anna");
            trainer.setLastName("Lee");
            trainer.addSpecialization(TrainingType.YOGA);
            trainer = facade.createTrainer(trainer);

            Training training = new Training();
            training.setTraineeId(trainee.getUserId());
            training.setTrainerId(trainer.getUserId());
            training.setTrainingName("Morning yoga");
            training.setTrainingType(TrainingType.YOGA);
            training.setTrainingDate(LocalDate.now());
            training.setTrainingDuration(Duration.ofMinutes(60));
            facade.createTraining(training);

            Trainee namesake = new Trainee();
            namesake.setFirstName("John");
            namesake.setLastName("Smith");
            namesake = facade.createTrainee(namesake);
            log.info("Second John Smith got username {}", namesake.getUsername());

            log.info("Storage now contains {} trainees, {} trainers, {} trainings",
                    facade.getAllTrainees().size(),
                    facade.getAllTrainers().size(),
                    facade.getAllTrainings().size());
        } catch (RuntimeException e) {
            log.error("Gym CRM failed", e);
            throw e;
        }
        log.info("Gym CRM stopped");
    }
}