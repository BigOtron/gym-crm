package io.gymcrm;

import io.gymcrm.config.AppConfig;
import io.gymcrm.dto.Credentials;
import io.gymcrm.dto.NewTraining;
import io.gymcrm.dto.TraineeRegistration;
import io.gymcrm.dto.TraineeTrainingCriteria;
import io.gymcrm.dto.TrainerRegistration;
import io.gymcrm.entities.Trainee;
import io.gymcrm.entities.Trainer;
import io.gymcrm.facade.GymFacade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.time.LocalDate;
import java.util.List;

@Slf4j
public class Application {

    public static void main(String[] args) {
        log.info("Starting Gym CRM");
        try (var context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            GymFacade facade = context.getBean(GymFacade.class);

            Trainee trainee = facade.createTrainee(
                    new TraineeRegistration("John", "Smith", LocalDate.of(1998, 3, 14), "Tashkent"));
            Trainer trainer = facade.createTrainer(new TrainerRegistration("Anna", "Lee", "Yoga"));
            var traineeAuth = new Credentials(trainee.getUser().getUsername(), trainee.getUser().getPassword());
            var trainerAuth = new Credentials(trainer.getUser().getUsername(), trainer.getUser().getPassword());

            facade.addTraining(trainerAuth, new NewTraining(traineeAuth.username(), trainerAuth.username(),
                    "Morning yoga", LocalDate.now(), 60));
            log.info("{} has {} trainings", traineeAuth.username(),
                    facade.getTraineeTrainings(traineeAuth, traineeAuth.username(),
                            TraineeTrainingCriteria.none()).size());

            List<Trainer> free = facade.getTrainersNotAssignedToTrainee(traineeAuth, traineeAuth.username());
            log.info("{} trainers are not assigned to {}", free.size(), traineeAuth.username());

            Trainee namesake = facade.createTrainee(new TraineeRegistration("John", "Smith", null, null));
            log.info("Second John Smith got username {}", namesake.getUser().getUsername());
        } catch (RuntimeException e) {
            log.error("Gym CRM failed", e);
            throw e;
        }
        log.info("Gym CRM stopped");
    }
}
