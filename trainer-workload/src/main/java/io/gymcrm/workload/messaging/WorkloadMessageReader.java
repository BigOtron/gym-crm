package io.gymcrm.workload.messaging;

import io.gymcrm.workload.service.WorkloadUpdate;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

/** Turns a message from the queue into a {@link WorkloadUpdate}, or explains why it can't. */
@Component
public class WorkloadMessageReader {

    private final JsonMapper jsonMapper;
    private final Validator validator;

    public WorkloadMessageReader(JsonMapper jsonMapper, Validator validator) {
        this.jsonMapper = jsonMapper;
        this.validator = validator;
    }

    /**
     * @throws InvalidWorkloadMessageException if the message is not a JSON text message, or a field is
     *                                         missing or invalid
     */
    public WorkloadUpdate read(Message message) throws JMSException {
        if (!(message instanceof TextMessage text) || text.getText() == null) {
            throw new InvalidWorkloadMessageException("Not a text message with a JSON body");
        }

        WorkloadMessage body;
        try {
            body = jsonMapper.readValue(text.getText(), WorkloadMessage.class);
        } catch (JacksonException e) {
            throw new InvalidWorkloadMessageException("Unreadable JSON: " + e.getOriginalMessage(), e);
        }
        if (body == null) {
            throw new InvalidWorkloadMessageException("Unreadable JSON: empty body");
        }

        Set<ConstraintViolation<WorkloadMessage>> violations = validator.validate(body);
        if (!violations.isEmpty()) {
            // e.g. "trainerUsername must not be blank, trainingDuration must not be null"
            throw new InvalidWorkloadMessageException(violations.stream()
                    .sorted(Comparator.comparing(violation -> violation.getPropertyPath().toString()))
                    .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                    .collect(Collectors.joining(", ")));
        }
        return body.toUpdate();
    }
}
