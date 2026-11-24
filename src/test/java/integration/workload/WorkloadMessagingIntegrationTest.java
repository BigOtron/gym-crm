package integration.workload;

import io.gymcrm.integration.workload.TrainerWorkloadEvent;
import io.gymcrm.integration.workload.TrainerWorkloadPublisher;
import io.gymcrm.integration.workload.WorkloadAction;
import io.gymcrm.integration.workload.WorkloadProperties;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import support.WebTestBase;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Gym CRM with the real publisher on an in-memory ActiveMQ broker: the test reads the workload queue the
 * way the Trainer Workload service does.
 */
class WorkloadMessagingIntegrationTest extends WebTestBase {

    /** An in-memory broker inside the test JVM (activemq-broker is a test dependency). */
    static final String BROKER = "vm://gym-crm-test?broker.persistent=false&broker.useJmx=false"
            + "&broker.useShutdownHook=false";

    private static final String TRAINING = """
            {"traineeUsername":"John.Smith","trainerUsername":"Anna.Lee","trainingName":"Evening yoga",
             "trainingDate":"2026-11-20","trainingDuration":60}""";

    @DynamicPropertySource
    static void broker(DynamicPropertyRegistry registry) {
        registry.add("gymcrm.workload.enabled", () -> "true");
        registry.add("spring.activemq.broker-url", () -> BROKER);
    }

    @Autowired
    private ConnectionFactory connectionFactory;

    @Autowired
    private WorkloadProperties properties;

    @Autowired
    private TrainerWorkloadPublisher publisher;

    @Autowired
    private PlatformTransactionManager transactionManager;

    /** Our own template, not the application's: it is not transacted, so it only sees committed messages. */
    private JmsTemplate queue;

    @BeforeEach
    void emptyTheQueue() {
        queue = new JmsTemplate(connectionFactory);
        queue.setReceiveTimeout(200);
        while (queue.receive(properties.queue()) != null) {
            // messages of the trainings created by earlier tests
        }
        queue.setReceiveTimeout(2000);
    }

    private List<TextMessage> receiveAll() {
        List<TextMessage> messages = new ArrayList<>();
        Message message;
        while ((message = queue.receive(properties.queue())) != null) {
            messages.add((TextMessage) message);
            queue.setReceiveTimeout(200);
        }
        queue.setReceiveTimeout(2000);
        return messages;
    }

    private static String text(TextMessage message) {
        try {
            return message.getText();
        } catch (JMSException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void addedTrainingIsSentAsJsonWithTheTransactionId() throws Exception {
        String transactionId = mvc.perform(json(post("/api/trainings"), john, TRAINING)
                        .header("X-Transaction-Id", "tx-add-training"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader("X-Transaction-Id");

        List<TextMessage> messages = receiveAll();
        assertEquals(1, messages.size());
        String body = text(messages.get(0));
        assertEquals("""
                {"trainerUsername":"Anna.Lee","trainerFirstName":"Anna","trainerLastName":"Lee","isActive":true,\
                "trainingDate":"2026-11-20","trainingDuration":60,"actionType":"ADD"}""", body);
        assertEquals(transactionId, messages.get(0).getStringProperty("transactionId"));
    }

    @Test
    void deletingATraineeSendsADeleteForEachOfTheirTrainings() throws Exception {
        addTraining(john, "Anna.Lee", "Yoga 1", "2026-11-20");
        addTraining(john, "David.Brown", "Lifting", "2026-12-01");
        assertEquals(2, receiveAll().size());

        mvc.perform(delete("/api/trainees/John.Smith").header("Authorization", john))
                .andExpect(status().isOk());

        List<String> bodies = receiveAll().stream().map(WorkloadMessagingIntegrationTest::text).toList();
        assertEquals(2, bodies.size());
        assertTrue(bodies.stream().allMatch(body -> body.contains("\"actionType\":\"DELETE\"")));
        assertTrue(bodies.stream().anyMatch(body -> body.contains("\"trainerUsername\":\"Anna.Lee\"")));
        assertTrue(bodies.stream().anyMatch(body -> body.contains("\"trainerUsername\":\"David.Brown\"")));
    }

    @Test
    void messageIsSentOnlyWhenTheTransactionCommits() {
        TrainerWorkloadEvent event = new TrainerWorkloadEvent("Anna.Lee", "Anna", "Lee", true,
                LocalDate.of(2026, 11, 20), 60, WorkloadAction.ADD);
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        queue.setReceiveTimeout(500);

        transaction.executeWithoutResult(status -> {
            publisher.publish(event);
            assertNull(queue.receive(properties.queue()), "visible before the commit");
        });
        assertNotNull(queue.receive(properties.queue()), "not sent after the commit");

        transaction.executeWithoutResult(status -> {
            publisher.publish(event);
            status.setRollbackOnly();
        });
        assertNull(queue.receive(properties.queue()), "sent although the transaction was rolled back");
    }
}
