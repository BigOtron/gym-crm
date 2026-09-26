package io.gymcrm.util;

import io.gymcrm.dao.TraineeDao;
import io.gymcrm.dao.TrainerDao;
import io.gymcrm.entities.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class UsernameGenerator {

    private TraineeDao traineeDao;
    private TrainerDao trainerDao;

    @Autowired
    public void setTraineeDao(TraineeDao traineeDao) {
        this.traineeDao = traineeDao;
    }

    @Autowired
    public void setTrainerDao(TrainerDao trainerDao) {
        this.trainerDao = trainerDao;
    }

    public String generate(String firstName, String lastName) {
        String base = firstName.trim() + "." + lastName.trim();
        Set<String> taken = existingUsernames();

        if (!taken.contains(base)) {
            return base;
        }

        int serial = 1;
        while (taken.contains(base + serial)) {
            serial++;
        }
        return base + serial;
    }

    private Set<String> existingUsernames() {
        return Stream.concat(
                        traineeDao.findAll().stream().map(User::getUsername),
                        trainerDao.findAll().stream().map(User::getUsername))
                .collect(Collectors.toSet());
    }
}