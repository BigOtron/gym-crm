package io.gymcrm.workload.messaging;

import io.gymcrm.workload.service.TrainerWorkloadService;
import io.gymcrm.workload.service.WorkloadUpdate;
import io.gymcrm.workload.web.logging.TransactionId;
import jakarta.validation.Validation;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;

import static io.gymcrm.workload.model.ActionType.ADD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class TrainerWorkloadListenerTest {

    private static final WorkloadUpdate UPDATE =
            new WorkloadUpdate("Anna.Lee", "Anna", "Lee", true, LocalDate.of(2026, 11, 20), 60, ADD);

    @Mock
    private TrainerWorkloadService service;

    @Mock
    private DeadLetterQueue deadLetters;

    private TrainerWorkloadListener listener() {
        WorkloadMessageReader reader = new WorkloadMessageReader(JsonMapper.builder().build(),
                Validation.buildDefaultValidatorFactory().getValidator());
        MessagingProperties properties = new MessagingProperties("updates", "updates.DLQ", 4,
                Duration.ofSeconds(1), 2);
        return new TrainerWorkloadListener(reader, service, deadLetters, properties);
    }

    /** A message as ActiveMQ delivers it for the {@code attempt}-th time. */
    private static ActiveMQTextMessage message(String body, int attempt) throws Exception {
        ActiveMQTextMessage message = WorkloadMessageReaderTest.text(body);
        message.setRedeliveryCounter(attempt - 1);
        return message;
    }

    @Test
    void appliesValidMessage() throws Exception {
        listener().onMessage(message(WorkloadMessageReaderTest.VALID, 1));

        verify(service).apply(UPDATE);
        verifyNoInteractions(deadLetters);
    }

    @Test
    void invalidMessageGoesStraightToTheDeadLetterQueue() throws Exception {
        ActiveMQTextMessage message = message("{\"trainerUsername\":\"Anna.Lee\"}", 1);

        listener().onMessage(message);

        verify(deadLetters).send(eq(message), startsWith("Invalid message: actionType "));
        verifyNoInteractions(service);
    }

    @Test
    void failureBeforeTheLastAttemptIsRethrownSoTheMessageIsRedelivered() throws Exception {
        IllegalStateException failure = new IllegalStateException("temporary");
        doThrow(failure).when(service).apply(any());

        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> listener().onMessage(message(WorkloadMessageReaderTest.VALID, 3))));

        verifyNoInteractions(deadLetters);
    }

    @Test
    void failureOnTheLastAttemptGoesToTheDeadLetterQueue() throws Exception {
        doThrow(new IllegalStateException("still broken")).when(service).apply(any());
        ActiveMQTextMessage message = message(WorkloadMessageReaderTest.VALID, 4);

        listener().onMessage(message);

        verify(deadLetters).send(message, "Failed 4 times: java.lang.IllegalStateException: still broken");
    }

    @Test
    void logsWithGymCrmsTransactionIdWhileProcessing() throws Exception {
        AtomicReference<String> seen = new AtomicReference<>();
        doAnswer(invocation -> {
            seen.set(TransactionId.current());
            return null;
        }).when(service).apply(any());
        ActiveMQTextMessage message = message(WorkloadMessageReaderTest.VALID, 1);
        message.setStringProperty(TrainerWorkloadListener.TRANSACTION_ID, "tx-from-gym-crm");

        listener().onMessage(message);

        assertEquals("tx-from-gym-crm", seen.get());
        assertNull(MDC.get(TransactionId.MDC_KEY), "cleared after the message");
    }

    @Test
    void createsATransactionIdWhenTheMessageHasNoValidOne() throws Exception {
        AtomicReference<String> seen = new AtomicReference<>();
        doAnswer(invocation -> {
            seen.set(TransactionId.current());
            return null;
        }).when(service).apply(any());
        ActiveMQTextMessage message = message(WorkloadMessageReaderTest.VALID, 1);
        message.setStringProperty(TrainerWorkloadListener.TRANSACTION_ID, "fake\nlog line");

        listener().onMessage(message);

        assertTrue(seen.get().matches("[0-9a-f-]{36}"), seen.get());
    }
}
