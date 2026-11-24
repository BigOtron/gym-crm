package io.gymcrm.workload.messaging;

import io.gymcrm.workload.web.logging.TransactionId;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

/**
 * Parks messages that can't be processed, so they neither block the queue nor get lost. Each one keeps
 * its original body and gets the reason, so someone can fix the cause and send it again.
 */
@Slf4j
@Component
public class DeadLetterQueue {

    /** Message properties of a dead letter. */
    static final String REASON = "deadLetterReason";
    static final String ORIGINAL_QUEUE = "originalQueue";
    static final String ORIGINAL_MESSAGE_ID = "originalMessageId";

    private final JmsTemplate jmsTemplate;
    private final String queue;

    public DeadLetterQueue(JmsTemplate jmsTemplate, MessagingProperties properties) {
        this.jmsTemplate = jmsTemplate;
        this.queue = properties.deadLetterQueue();
    }

    /**
     * Called from the listener: the JmsTemplate takes part in the listener's JMS transaction, so the copy is
     * sent and the original removed from the queue together, or neither is.
     */
    public void send(Message original, String reason) throws JMSException {
        String body = original instanceof TextMessage text ? text.getText() : null;
        String messageId = original.getJMSMessageID();
        String originalQueue = String.valueOf(original.getJMSDestination());
        String transactionId = TransactionId.current();

        jmsTemplate.send(queue, session -> {
            TextMessage copy = session.createTextMessage(body);
            copy.setStringProperty(REASON, reason);
            copy.setStringProperty(ORIGINAL_QUEUE, originalQueue);
            copy.setStringProperty(ORIGINAL_MESSAGE_ID, messageId);
            if (transactionId != null) {
                copy.setStringProperty(TrainerWorkloadListener.TRANSACTION_ID, transactionId);
            }
            return copy;
        });
        log.warn("Moved message {} to the dead letter queue {}: {}", messageId, queue, reason);
    }
}
