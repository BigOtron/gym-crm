package io.gymcrm.config;

import io.gymcrm.entities.Trainee;
import io.gymcrm.entities.Trainer;
import io.gymcrm.entities.Training;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static io.gymcrm.config.StorageNames.TRAINEE_STORAGE;
import static io.gymcrm.config.StorageNames.TRAINER_STORAGE;
import static io.gymcrm.config.StorageNames.TRAINING_STORAGE;

@Configuration
public class StorageConfig {

    @Bean(TRAINEE_STORAGE)
    public Map<UUID, Trainee> traineeStorage() {
        return new ConcurrentHashMap<>();
    }

    @Bean(TRAINER_STORAGE)
    public Map<UUID, Trainer> trainerStorage() {
        return new ConcurrentHashMap<>();
    }

    @Bean(TRAINING_STORAGE)
    public Map<UUID, Training> trainingStorage() {
        return new ConcurrentHashMap<>();
    }
}
