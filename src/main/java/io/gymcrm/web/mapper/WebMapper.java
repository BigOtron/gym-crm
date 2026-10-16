package io.gymcrm.web.mapper;

import io.gymcrm.dto.NewTraining;
import io.gymcrm.dto.TraineeRegistration;
import io.gymcrm.dto.TraineeTrainingCriteria;
import io.gymcrm.dto.TraineeUpdate;
import io.gymcrm.dto.TrainerRegistration;
import io.gymcrm.dto.TrainerTrainingCriteria;
import io.gymcrm.dto.TrainerUpdate;
import io.gymcrm.entities.Trainee;
import io.gymcrm.entities.Trainer;
import io.gymcrm.entities.Training;
import io.gymcrm.entities.TrainingType;
import io.gymcrm.entities.User;
import io.gymcrm.web.dto.CredentialsResponse;
import io.gymcrm.web.dto.NewTrainingRequest;
import io.gymcrm.web.dto.TraineeProfileResponse;
import io.gymcrm.web.dto.TraineeRegistrationRequest;
import io.gymcrm.web.dto.TraineeSummary;
import io.gymcrm.web.dto.TraineeTrainingResponse;
import io.gymcrm.web.dto.TraineeTrainingsQuery;
import io.gymcrm.web.dto.TraineeUpdateRequest;
import io.gymcrm.web.dto.TrainerProfileResponse;
import io.gymcrm.web.dto.TrainerRegistrationRequest;
import io.gymcrm.web.dto.TrainerSummary;
import io.gymcrm.web.dto.TrainerTrainingResponse;
import io.gymcrm.web.dto.TrainerTrainingsQuery;
import io.gymcrm.web.dto.TrainerUpdateRequest;
import io.gymcrm.web.dto.TrainingTypeResponse;

import java.util.Comparator;
import java.util.List;

public final class WebMapper {

    private WebMapper() {
    }

    public static TraineeRegistration toRegistration(TraineeRegistrationRequest request) {
        return new TraineeRegistration(request.firstName(), request.lastName(),
                request.dateOfBirth(), request.address());
    }

    public static TrainerRegistration toRegistration(TrainerRegistrationRequest request) {
        return new TrainerRegistration(request.firstName(), request.lastName(), request.specializationId());
    }

    public static TraineeUpdate toUpdate(TraineeUpdateRequest request) {
        return new TraineeUpdate(request.firstName(), request.lastName(),
                request.dateOfBirth(), request.address(), request.isActive());
    }

    public static TrainerUpdate toUpdate(TrainerUpdateRequest request) {
        return new TrainerUpdate(request.firstName(), request.lastName(), request.isActive());
    }

    public static NewTraining toNewTraining(NewTrainingRequest request) {
        return new NewTraining(request.traineeUsername(), request.trainerUsername(), request.trainingName(),
                request.trainingDate(), request.trainingDuration());
    }

    public static TraineeTrainingCriteria toCriteria(TraineeTrainingsQuery query) {
        return new TraineeTrainingCriteria(query.from(), query.to(), query.trainerName(), query.trainingType());
    }

    public static TrainerTrainingCriteria toCriteria(TrainerTrainingsQuery query) {
        return new TrainerTrainingCriteria(query.from(), query.to(), query.traineeName());
    }

    public static CredentialsResponse toCredentials(User user) {
        return new CredentialsResponse(user.getUsername(), user.getPassword());
    }

    public static TrainingTypeResponse toResponse(TrainingType type) {
        return new TrainingTypeResponse(type.getId(), type.getTrainingTypeName());
    }

    public static List<TrainingTypeResponse> toTrainingTypes(List<TrainingType> types) {
        return types.stream().map(WebMapper::toResponse).toList();
    }

    public static TrainerSummary toSummary(Trainer trainer) {
        User user = trainer.getUser();
        return new TrainerSummary(user.getUsername(), user.getFirstName(), user.getLastName(),
                toResponse(trainer.getSpecialization()));
    }

    public static List<TrainerSummary> toTrainerSummaries(List<Trainer> trainers) {
        return trainers.stream().map(WebMapper::toSummary).toList();
    }

    public static TraineeSummary toSummary(Trainee trainee) {
        User user = trainee.getUser();
        return new TraineeSummary(user.getUsername(), user.getFirstName(), user.getLastName());
    }

    public static TraineeProfileResponse toProfile(Trainee trainee) {
        User user = trainee.getUser();
        List<TrainerSummary> trainers = trainee.getTrainers().stream()
                .map(WebMapper::toSummary)
                .sorted(Comparator.comparing(TrainerSummary::username))
                .toList();
        return new TraineeProfileResponse(user.getUsername(), user.getFirstName(), user.getLastName(),
                trainee.getDateOfBirth(), trainee.getAddress(), user.isActive(), trainers);
    }

    public static TrainerProfileResponse toProfile(Trainer trainer) {
        User user = trainer.getUser();
        List<TraineeSummary> trainees = trainer.getTrainees().stream()
                .map(WebMapper::toSummary)
                .sorted(Comparator.comparing(TraineeSummary::username))
                .toList();
        return new TrainerProfileResponse(user.getUsername(), user.getFirstName(), user.getLastName(),
                toResponse(trainer.getSpecialization()), user.isActive(), trainees);
    }

    public static List<TraineeTrainingResponse> toTraineeTrainings(List<Training> trainings) {
        return trainings.stream()
                .map(t -> new TraineeTrainingResponse(t.getTrainingName(), t.getTrainingDate(),
                        t.getTrainingType().getTrainingTypeName(), t.getTrainingDuration(),
                        fullName(t.getTrainer().getUser())))
                .toList();
    }

    public static List<TrainerTrainingResponse> toTrainerTrainings(List<Training> trainings) {
        return trainings.stream()
                .map(t -> new TrainerTrainingResponse(t.getTrainingName(), t.getTrainingDate(),
                        t.getTrainingType().getTrainingTypeName(), t.getTrainingDuration(),
                        fullName(t.getTrainee().getUser())))
                .toList();
    }

    private static String fullName(User user) {
        return user.getFirstName() + " " + user.getLastName();
    }
}
