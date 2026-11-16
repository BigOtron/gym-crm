package io.gymcrm.dao.implementations;

import io.gymcrm.dao.TrainerDao;
import io.gymcrm.entities.Trainer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
public class TrainerDaoImpl implements TrainerDao {

    private EntityManager entityManager;

    @PersistenceContext
    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Trainer save(Trainer trainer) {
        entityManager.persist(trainer);
        log.debug("Persisted trainer {} with id {}", trainer.getUser().getUsername(), trainer.getId());
        return trainer;
    }

    @Override
    public Optional<Trainer> findByUsername(String username) {
        Optional<Trainer> trainer = entityManager.createQuery("""
                        select t from Trainer t
                        join fetch t.user u
                        join fetch t.specialization
                        left join fetch t.trainees
                        where u.username = :username""", Trainer.class)
                .setParameter("username", username)
                .getResultStream()
                .findFirst();
        log.debug("Lookup trainer by username {}: {}", username, trainer.isPresent() ? "found" : "not found");
        return trainer;
    }

    @Override
    public List<Trainer> findByUsernames(Collection<String> usernames) {
        if (usernames.isEmpty()) {
            return List.of();
        }
        List<Trainer> trainers = entityManager.createQuery("""
                        select t from Trainer t
                        join fetch t.user u
                        join fetch t.specialization
                        where u.username in :usernames""", Trainer.class)
                .setParameter("usernames", usernames)
                .getResultList();
        log.debug("Found {} of {} requested trainers", trainers.size(), usernames.size());
        return trainers;
    }

    @Override
    public List<Trainer> findNotAssignedToTrainee(String traineeUsername) {
        List<Trainer> trainers = entityManager.createQuery("""
                        select t from Trainer t
                        join fetch t.user u
                        join fetch t.specialization
                        where u.active = true
                          and t not in (
                              select assigned from Trainee te
                              join te.trainers assigned
                              where te.user.username = :username)
                        order by u.username""", Trainer.class)
                .setParameter("username", traineeUsername)
                .getResultList();
        log.debug("Found {} trainers not assigned to trainee {}", trainers.size(), traineeUsername);
        return trainers;
    }
}
