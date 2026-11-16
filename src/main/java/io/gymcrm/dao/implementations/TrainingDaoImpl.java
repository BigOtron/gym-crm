package io.gymcrm.dao.implementations;

import io.gymcrm.dao.TrainingDao;
import io.gymcrm.dto.TraineeTrainingCriteria;
import io.gymcrm.dto.TrainerTrainingCriteria;
import io.gymcrm.entities.Trainee;
import io.gymcrm.entities.Trainer;
import io.gymcrm.entities.Training;
import io.gymcrm.entities.TrainingType;
import io.gymcrm.entities.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
public class TrainingDaoImpl implements TrainingDao {

    private EntityManager entityManager;

    @PersistenceContext
    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Training save(Training training) {
        entityManager.persist(training);
        log.debug("Persisted training '{}' with id {}", training.getTrainingName(), training.getId());
        return training;
    }

    @Override
    public List<Training> findTraineeTrainings(String traineeUsername, TraineeTrainingCriteria criteria) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Training> query = cb.createQuery(Training.class);
        Root<Training> training = query.from(Training.class);
        Joins joins = fetchAll(training);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(joins.traineeUser().get("username"), traineeUsername));
        addDateRange(cb, training, criteria.fromDate(), criteria.toDate(), predicates);
        if (hasText(criteria.trainerName())) {
            predicates.add(nameMatches(cb, joins.trainerUser(), criteria.trainerName()));
        }
        if (hasText(criteria.trainingType())) {
            predicates.add(cb.equal(cb.lower(joins.type().get("trainingTypeName")),
                    criteria.trainingType().trim().toLowerCase()));
        }

        List<Training> result = run(query, training, predicates);
        log.debug("Found {} trainings for trainee {} with {}", result.size(), traineeUsername, criteria);
        return result;
    }

    @Override
    public List<Training> findTrainerTrainings(String trainerUsername, TrainerTrainingCriteria criteria) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Training> query = cb.createQuery(Training.class);
        Root<Training> training = query.from(Training.class);
        Joins joins = fetchAll(training);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(joins.trainerUser().get("username"), trainerUsername));
        addDateRange(cb, training, criteria.fromDate(), criteria.toDate(), predicates);
        if (hasText(criteria.traineeName())) {
            predicates.add(nameMatches(cb, joins.traineeUser(), criteria.traineeName()));
        }

        List<Training> result = run(query, training, predicates);
        log.debug("Found {} trainings for trainer {} with {}", result.size(), trainerUsername, criteria);
        return result;
    }

    private record Joins(Join<Trainee, User> traineeUser, Join<Trainer, User> trainerUser,
                         Join<Training, TrainingType> type) {
    }

    /**
     * Fetch-joins everything a caller reads from a training, so the result can be used
     * after the transaction is closed. The fetches are also used as joins in the WHERE clause.
     */
    @SuppressWarnings("unchecked")
    private static Joins fetchAll(Root<Training> training) {
        Join<Training, Trainee> trainee = (Join<Training, Trainee>) training.<Training, Trainee>fetch("trainee");
        Join<Training, Trainer> trainer = (Join<Training, Trainer>) training.<Training, Trainer>fetch("trainer");
        Join<Trainee, User> traineeUser = (Join<Trainee, User>) trainee.<Trainee, User>fetch("user");
        Join<Trainer, User> trainerUser = (Join<Trainer, User>) trainer.<Trainer, User>fetch("user");
        trainer.fetch("specialization");
        Join<Training, TrainingType> type =
                (Join<Training, TrainingType>) training.<Training, TrainingType>fetch("trainingType");
        return new Joins(traineeUser, trainerUser, type);
    }

    private static void addDateRange(CriteriaBuilder cb, Root<Training> training,
                                     LocalDate from, LocalDate to, List<Predicate> predicates) {
        if (from != null) {
            predicates.add(cb.greaterThanOrEqualTo(training.get("trainingDate"), from));
        }
        if (to != null) {
            predicates.add(cb.lessThanOrEqualTo(training.get("trainingDate"), to));
        }
    }

    /** Case-insensitive "contains" on first name, last name or "first last". */
    private static Predicate nameMatches(CriteriaBuilder cb, Join<?, User> user, String name) {
        String pattern = "%" + name.trim().toLowerCase() + "%";
        return cb.like(cb.lower(cb.concat(cb.concat(user.get("firstName"), " "), user.get("lastName"))), pattern);
    }

    private List<Training> run(CriteriaQuery<Training> query, Root<Training> training, List<Predicate> predicates) {
        query.select(training)
                .where(predicates.toArray(Predicate[]::new))
                .orderBy(entityManager.getCriteriaBuilder().asc(training.get("trainingDate")));
        return entityManager.createQuery(query).getResultList();
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
