package io.gymcrm.dao.implementations;

import io.gymcrm.dao.TrainingTypeDao;
import io.gymcrm.entities.TrainingType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
public class TrainingTypeDaoImpl implements TrainingTypeDao {

    private EntityManager entityManager;

    @PersistenceContext
    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<TrainingType> findByName(String name) {
        Optional<TrainingType> type = entityManager.createQuery(
                        "select t from TrainingType t where lower(t.trainingTypeName) = lower(:name)",
                        TrainingType.class)
                .setParameter("name", name)
                .getResultStream()
                .findFirst();
        log.debug("Lookup training type {}: {}", name, type.isPresent() ? "found" : "not found");
        return type;
    }

    @Override
    public List<TrainingType> findAll() {
        return entityManager.createQuery("select t from TrainingType t order by t.id", TrainingType.class)
                .getResultList();
    }
}
