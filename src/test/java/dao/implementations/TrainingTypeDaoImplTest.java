package dao.implementations;

import io.gymcrm.dao.TrainingTypeDao;
import io.gymcrm.entities.TrainingType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import support.DaoTestBase;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrainingTypeDaoImplTest extends DaoTestBase {

    @Autowired
    private TrainingTypeDao trainingTypeDao;

    @Test
    void findAllReturnsSeededTypesInIdOrder() {
        List<String> names = trainingTypeDao.findAll().stream().map(TrainingType::getTrainingTypeName).toList();

        assertEquals(List.of("Fitness", "Running", "Heavy lifting", "Pull ups", "Push ups", "Yoga"), names);
    }

    @Test
    void findByNameIgnoresCase() {
        assertEquals(6L, trainingTypeDao.findByName("yoga").orElseThrow().getId());
        assertEquals(3L, trainingTypeDao.findByName("HEAVY LIFTING").orElseThrow().getId());
    }

    @Test
    void findByNameReturnsEmptyForUnknownType() {
        assertTrue(trainingTypeDao.findByName("Boxing").isEmpty());
    }

    @Test
    void changesToTrainingTypesAreNotSaved() {
        // There are no setters, so change the field the way Hibernate would, through reflection.
        TrainingType yoga = trainingTypeDao.findByName("Yoga").orElseThrow();
        ReflectionTestUtils.setField(yoga, "trainingTypeName", "Hot yoga");
        flushAndClear();

        assertEquals("Yoga", trainingTypeDao.findByName("Yoga").orElseThrow().getTrainingTypeName());
    }
}
