package io.gymcrm.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.gymcrm.config.StorageNames;
import io.gymcrm.entities.Trainee;
import io.gymcrm.entities.Trainer;
import io.gymcrm.entities.Training;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Slf4j
@Component
public class StorageInitializer implements BeanPostProcessor {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
    private final ResourceLoader resourceLoader = new DefaultResourceLoader();

    private String traineeFile;
    private String trainerFile;
    private String trainingFile;

    @Value("${storage.trainee.file}")
    public void setTraineeFile(String traineeFile) {
        this.traineeFile = traineeFile;
    }

    @Value("${storage.trainer.file}")
    public void setTrainerFile(String trainerFile) {
        this.trainerFile = trainerFile;
    }

    @Value("${storage.training.file}")
    public void setTrainingFile(String trainingFile) {
        this.trainingFile = trainingFile;
    }

    @Override
    public Object postProcessAfterInitialization(@NonNull Object bean, String beanName) {
        switch (beanName) {
            case StorageNames.TRAINEE_STORAGE -> load(asMap(bean), traineeFile,
                    new TypeReference<>() {
                    }, Trainee::getUserId, beanName);
            case StorageNames.TRAINER_STORAGE -> load(asMap(bean), trainerFile,
                    new TypeReference<>() {
                    }, Trainer::getUserId, beanName);
            case StorageNames.TRAINING_STORAGE -> load(asMap(bean), trainingFile,
                    new TypeReference<>() {
                    }, Training::getTrainingId, beanName);
            default -> { }
        }
        return bean;
    }

    private <T> void load(Map<UUID, T> storage, String path, TypeReference<List<T>> type,
                          Function<T, UUID> idExtractor, String beanName) {
        Resource resource = resourceLoader.getResource(path);
        if (!resource.exists()) {
            return;
        }
        try (InputStream in = resource.getInputStream()) {
            List<T> items = objectMapper.readValue(in, type);
            items.forEach(item -> storage.put(idExtractor.apply(item), item));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load " + beanName + " from " + path, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> Map<UUID, T> asMap(Object bean) {
        return (Map<UUID, T>) bean;
    }
}
