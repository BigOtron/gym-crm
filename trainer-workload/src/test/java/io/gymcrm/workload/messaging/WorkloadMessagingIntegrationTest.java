package io.gymcrm.workload.messaging;

import io.gymcrm.workload.service.TrainerWorkloadService;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * The listener on a real (in-memory) ActiveMQ broker: messages are sent to the queue the way Gym CRM sends
 * them, and dead letters are read back from the dead letter queue.
 */
@SpringBootTest(properties = "workload.messaging.redelivery-delay=10ms")
class WorkloadMessagingIntegrationTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    @Autowired
    private ConnectionFactory connectionFactory;

    @Autowired
    private MessagingProperties properties;

    @MockitoSpyBean
    private TrainerWorkloadService service;

    /** Our own template, not the application's: it is not transacted and waits for messages to receive. */
    private JmsTemplate jms;

    /** Every test uses its own trainer, so the shared in-memory data doesn't matter. */
    private final String trainer = "Trainer-" + UUID.randomUUID();

    @BeforeEach
    void setUp() {
        jms = new JmsTemplate(connectionFactory);
        jms.setReceiveTimeout(JmsTemplate.RECEIVE_TIMEOUT_NO_WAIT);
        while (jms.receive(properties.deadLetterQueue()) != null) {
            // dead letters of earlier tests
        }
        jms.setReceiveTimeout(TIMEOUT.toMillis());
    }

    private String training(String date, int minutes, String action) {
        return """
                {"trainerUsername":"%s","trainerFirstName":"Anna","trainerLastName":"Lee","isActive":true,
                 "trainingDate":"%s","trainingDuration":%d,"actionType":"%s"}""".formatted(trainer, date, minutes, action);
    }

    private void send(String body, String transactionId) {
        jms.send(properties.queue(), session -> {
            TextMessage message = session.createTextMessage(body);
            message.setStringProperty(TrainerWorkloadListener.TRANSACTION_ID, transactionId);
            return message;
        });
    }

    private int minutes(String month) {
        try {
            return service.getMinutes(trainer, YearMonth.parse(month));
        } catch (RuntimeException notYet) {
            return -1;
        }
    }

    private static void await(String what, BooleanSupplier condition) throws InterruptedException {
        Instant deadline = Instant.now().plus(TIMEOUT);
        while (!condition.getAsBoolean()) {
            if (Instant.now().isAfter(deadline)) {
                fail("Timed out waiting until " + what);
            }
            Thread.sleep(20);
        }
    }

    @Test
    void messagesFromTheQueueUpdateTheWorkloadInOrder() throws Exception {
        send(training("2026-11-03", 60, "ADD"), "tx-1");
        send(training("2026-11-20", 45, "ADD"), "tx-2");
        send(training("2026-11-03", 60, "DELETE"), "tx-3");

        await("the three messages are processed", () -> minutes("2026-11") == 45);
    }

    @Test
    void invalidMessageGoesToTheDeadLetterQueueWithTheReason() throws Exception {
        String body = training("2026-11-03", 60, "ADD").replace("\"trainingDate\":\"2026-11-03\",", "");
        send(body, "tx-invalid");

        Message dead = jms.receive(properties.deadLetterQueue());

        assertNotNull(dead, "nothing in the dead letter queue");
        assertEquals(body, ((TextMessage) dead).getText());
        assertTrue(dead.getStringProperty(DeadLetterQueue.REASON).startsWith("Invalid message: trainingDate "),
                dead.getStringProperty(DeadLetterQueue.REASON));
        assertEquals("queue://" + properties.queue(), dead.getStringProperty(DeadLetterQueue.ORIGINAL_QUEUE));
        assertNotNull(dead.getStringProperty(DeadLetterQueue.ORIGINAL_MESSAGE_ID));
        assertEquals("tx-invalid", dead.getStringProperty(TrainerWorkloadListener.TRANSACTION_ID));
        assertEquals(-1, minutes("2026-11"), "nothing was applied");
    }

    @Test
    void brokenJsonGoesToTheDeadLetterQueue() throws Exception {
        send("{not json", "tx-broken");

        Message dead = jms.receive(properties.deadLetterQueue());

        assertNotNull(dead, "nothing in the dead letter queue");
        assertTrue(dead.getStringProperty(DeadLetterQueue.REASON).startsWith("Invalid message: Unreadable JSON"));
    }

    @Test
    void failedMessageIsRetriedAndThenApplied() throws Exception {
        doThrow(new IllegalStateException("temporary")).doCallRealMethod()
                .when(service).apply(argThat(update -> update.trainerUsername().equals(trainer)));

        send(training("2026-11-03", 60, "ADD"), "tx-retry");

        await("the second attempt succeeds", () -> minutes("2026-11") == 60);
        verify(service, times(2)).apply(argThat(update -> update.trainerUsername().equals(trainer)));
    }

    @Test
    void messageThatFailsEveryAttemptGoesToTheDeadLetterQueue() throws Exception {
        doThrow(new IllegalStateException("still broken"))
                .when(service).apply(argThat(update -> update.trainerUsername().equals(trainer)));

        send(training("2026-11-03", 60, "ADD"), "tx-poison");
        Message dead = jms.receive(properties.deadLetterQueue());

        assertNotNull(dead, "nothing in the dead letter queue");
        assertEquals("Failed 4 times: java.lang.IllegalStateException: still broken",
                dead.getStringProperty(DeadLetterQueue.REASON));
        verify(service, times(properties.maxAttempts()))
                .apply(argThat(update -> update.trainerUsername().equals(trainer)));

        // The queue isn't blocked: the next message is processed.
        doCallRealMethod().when(service).apply(argThat(update -> update.trainerUsername().equals(trainer)));
        send(training("2026-11-20", 45, "ADD"), "tx-next");
        await("the next message is processed", () -> minutes("2026-11") == 45);
        verify(service, atLeast(5)).apply(argThat(update -> update.trainerUsername().equals(trainer)));
    }
}
