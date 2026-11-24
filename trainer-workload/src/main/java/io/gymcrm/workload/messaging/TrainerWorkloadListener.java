package io.gymcrm.workload.messaging;

import io.gymcrm.workload.service.TrainerWorkloadService;
import io.gymcrm.workload.service.WorkloadUpdate;
import io.gymcrm.workload.web.logging.TransactionId;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

/**
 * Applies the added and deleted trainings Gym CRM sends to the workload queue.
 * <p>
 * Each message is processed in a JMS transaction: it is removed from the queue only if this method
 * returns normally. Then:
 * <ul>
 *   <li>valid message: the minutes are added or removed;</li>
 *   <li>invalid message (not JSON, required information missing): straight to the dead letter queue, as
 *       retrying can't fix it;</li>
 *   <li>processing fails: the exception rolls the transaction back and ActiveMQ delivers the message
 *       again a little later; on the last attempt it goes to the dead letter queue instead.</li>
 * </ul>
 * One consumer (concurrency 1) processes the messages in the order Gym CRM sent them, so a training's
 * DELETE can't overtake its ADD.
 */
@Slf4j
@Component
public class TrainerWorkloadListener {

    /** Message property with Gym CRM's transaction id, so one request can be followed across both services. */
    public static final String TRANSACTION_ID = "transactionId";

    /** Set by ActiveMQ: 1 on the first delivery, 2 on the first retry, ... */
    static final String DELIVERY_COUNT = "JMSXDeliveryCount";

    private final WorkloadMessageReader reader;
    private final TrainerWorkloadService service;
    private final DeadLetterQueue deadLetters;
    private final int maxAttempts;

    public TrainerWorkloadListener(WorkloadMessageReader reader, TrainerWorkloadService service,
                                   DeadLetterQueue deadLetters, MessagingProperties properties) {
        this.reader = reader;
        this.service = service;
        this.deadLetters = deadLetters;
        this.maxAttempts = properties.maxAttempts();
    }

    @JmsListener(destination = "${workload.messaging.queue}", concurrency = "1")
    public void onMessage(Message message) throws JMSException {
        MDC.put(TransactionId.MDC_KEY, TransactionId.reuseOrCreate(message.getStringProperty(TRANSACTION_ID)));
        try {
            int attempt = Math.max(1, message.getIntProperty(DELIVERY_COUNT));
            log.debug("Received message {} (attempt {} of {})", message.getJMSMessageID(), attempt, maxAttempts);

            WorkloadUpdate update;
            try {
                update = reader.read(message);
            } catch (InvalidWorkloadMessageException e) {
                deadLetters.send(message, "Invalid message: " + e.getMessage());
                return;
            }

            try {
                service.apply(update);
            } catch (RuntimeException e) {
                if (attempt < maxAttempts) {
                    log.warn("Attempt {} of {} to process message {} failed, it will be retried: {}", attempt,
                            maxAttempts, message.getJMSMessageID(), e.toString());
                    throw e;
                }
                log.error("Processing message {} failed {} times", message.getJMSMessageID(), attempt, e);
                deadLetters.send(message, "Failed " + attempt + " times: " + e);
            }
        } finally {
            MDC.remove(TransactionId.MDC_KEY);
        }
    }
}
