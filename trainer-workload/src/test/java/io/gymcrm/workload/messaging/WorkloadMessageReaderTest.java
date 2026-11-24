package io.gymcrm.workload.messaging;

import io.gymcrm.workload.service.WorkloadUpdate;
import jakarta.validation.Validation;
import org.apache.activemq.command.ActiveMQBytesMessage;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.List;

import static io.gymcrm.workload.model.ActionType.ADD;
import static io.gymcrm.workload.model.ActionType.DELETE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkloadMessageReaderTest {

    static final String VALID = """
            {"trainerUsername":"Anna.Lee","trainerFirstName":"Anna","trainerLastName":"Lee","isActive":true,
             "trainingDate":"2026-11-20","trainingDuration":60,"actionType":"ADD"}""";

    private final WorkloadMessageReader reader = new WorkloadMessageReader(JsonMapper.builder().build(),
            Validation.buildDefaultValidatorFactory().getValidator());

    static ActiveMQTextMessage text(String body) throws Exception {
        ActiveMQTextMessage message = new ActiveMQTextMessage();
        message.setText(body);
        return message;
    }

    @Test
    void readsAddedTraining() throws Exception {
        assertEquals(new WorkloadUpdate("Anna.Lee", "Anna", "Lee", true, LocalDate.of(2026, 11, 20), 60, ADD),
                reader.read(text(VALID)));
    }

    @Test
    void readsDeletedTrainingOfAnInactiveTrainer() throws Exception {
        WorkloadUpdate update = reader.read(text(VALID.replace("\"ADD\"", "\"DELETE\"").replace("true", "false")));

        assertEquals(DELETE, update.actionType());
        assertEquals(false, update.active());
    }

    @Test
    void ignoresUnknownFields() throws Exception {
        assertEquals(ADD, reader.read(text(VALID.replace("{", "{\"comment\":\"new field\","))).actionType());
    }

    @Test
    void namesEveryMissingField() {
        InvalidWorkloadMessageException e = assertThrows(InvalidWorkloadMessageException.class,
                () -> reader.read(text("""
                        {"trainerUsername":" ","trainingDuration":0}""")));

        // Every field, sorted by name (the validator's own order isn't fixed; the texts depend on the locale).
        String reason = e.getMessage();
        int previous = -1;
        for (String field : List.of("actionType", "isActive", "trainerFirstName", "trainerLastName",
                "trainerUsername", "trainingDate", "trainingDuration")) {
            int position = reason.indexOf(field + " ");
            assertTrue(position > previous, field + " missing or out of order in: " + reason);
            previous = position;
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"{", "[]", "\"text\"", "not json"})
    void rejectsSomethingThatIsNotAWorkloadObject(String body) {
        InvalidWorkloadMessageException e = assertThrows(InvalidWorkloadMessageException.class,
                () -> reader.read(text(body)));

        assertTrue(e.getMessage().startsWith("Unreadable JSON: "), e.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"actionType\":\"UPDATE\"", "\"trainingDate\":\"20.11.2026\"", "\"trainingDuration\":\"long\""})
    void rejectsUnreadableValues(String field) {
        String name = field.substring(0, field.indexOf(':'));
        String broken = VALID.replaceFirst(name + ":[^,}]+", field);

        assertThrows(InvalidWorkloadMessageException.class, () -> reader.read(text(broken)));
    }

    @Test
    void rejectsEmptyBody() {
        assertEquals("Unreadable JSON: empty body",
                assertThrows(InvalidWorkloadMessageException.class, () -> reader.read(text("null"))).getMessage());
        assertEquals("Not a text message with a JSON body",
                assertThrows(InvalidWorkloadMessageException.class, () -> reader.read(text(null))).getMessage());
    }

    @Test
    void rejectsOtherMessageTypes() {
        assertEquals("Not a text message with a JSON body",
                assertThrows(InvalidWorkloadMessageException.class,
                        () -> reader.read(new ActiveMQBytesMessage())).getMessage());
    }
}
