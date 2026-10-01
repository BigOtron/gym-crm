package io.gymcrm.dao.implementations;

import io.gymcrm.dao.TraineeDao;
import io.gymcrm.entities.Trainee;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Slf4j
@Repository
public class TraineeDaoImpl implements TraineeDao {

    private EntityManager entityManager;

    @PersistenceContext
    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Trainee save(Trainee trainee) {
        entityManager.persist(trainee);
        log.debug("Persisted trainee {} with id {}", trainee.getUser().getUsername(), trainee.getId());
        return trainee;
    }

    @Override
    public void delete(Trainee trainee) {
        entityManager.remove(trainee);
        log.debug("Removed trainee {} with id {}", trainee.getUser().getUsername(), trainee.getId());
    }

    @Override
    public Optional<Trainee> findByUsername(String username) {
        Optional<Trainee> trainee = entityManager.createQuery("""
                        select t from Trainee t
                        join fetch t.user u
                        left join fetch t.trainers
                        where u.username = :username""", Trainee.class)
                .setParameter("username", username)
                .getResultStream()
                .findFirst();
        log.debug("Lookup trainee by username {}: {}", username, trainee.isPresent() ? "found" : "not found");
        return trainee;
    }
}
