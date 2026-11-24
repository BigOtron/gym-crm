package io.gymcrm.integration.workload;

import io.gymcrm.exceptions.ServiceUnavailableException;
import io.gymcrm.web.logging.TransactionId;
import jakarta.jms.TextMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;
import tools.jackson.databind.json.JsonMapper;

/**
 * Sends each event as a JSON text message to the ActiveMQ queue the Trainer Workload service listens on.
 * <p>
 * The JmsTemplate uses a transacted JMS session (spring.jms.template.session.transacted). Inside the
 * service's database transaction, Spring ties that session to the transaction: the message reaches the
 * queue only after the database commit, and is dropped if the transaction rolls back. So a training that
 * isn't saved is never reported, and deleting a trainee reports all of their trainings or none.
 * <p>
 * If the broker can't be reached, sending fails at once with a {@link ServiceUnavailableException} (HTTP
 * 503), which rolls the database transaction back too. If only the workload service is down, nothing fails
 * here: the messages wait in the queue until it is back.
 */
@Slf4j
public class JmsTrainerWorkloadPublisher implements TrainerWorkloadPublisher {

    /** Message property with our transaction id, so one request can be followed in both services' logs. */
    static final String TRANSACTION_ID = "transactionId";

    private final JmsTemplate jmsTemplate;
    private final JsonMapper jsonMapper;
    private final String queue;

    public JmsTrainerWorkloadPublisher(JmsTemplate jmsTemplate, JsonMapper jsonMapper, String queue) {
        this.jmsTemplate = jmsTemplate;
        this.jsonMapper = jsonMapper;
        this.queue = queue;
    }

    @Override
    public void publish(TrainerWorkloadEvent event) {
        String body = jsonMapper.writeValueAsString(event);
        String transactionId = TransactionId.current();
        try {
            jmsTemplate.send(queue, session -> {
                TextMessage message = session.createTextMessage(body);
                if (transactionId != null) {
                    message.setStringProperty(TRANSACTION_ID, transactionId);
                }
                return message;
            });
        } catch (JmsException e) {
            log.error("Could not send {} of a training of trainer {} to queue {}: {}", event.actionType(),
                    event.trainerUsername(), queue, e.getMessage());
            throw new ServiceUnavailableException("The message broker is unavailable, please try again later", e);
        }
        log.info("Queued {} of a {} min training on {} for trainer {} (sent when the transaction commits)",
                event.actionType(), event.trainingDuration(), event.trainingDate(), event.trainerUsername());
    }
}
