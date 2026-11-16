package web.dto;

import io.gymcrm.web.dto.TraineeProfileResponse;
import io.gymcrm.web.dto.TraineeUpdateRequest;
import io.gymcrm.web.dto.TrainerSummary;
import io.gymcrm.web.dto.TrainingTypeResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.http.MockHttpOutputMessage;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Checks the JSON produced and accepted by Spring MVC's own converter, so the API contract
 * (field names, date format) can't change silently.
 */
class JsonShapeTest {

    private final JacksonJsonHttpMessageConverter converter = new JacksonJsonHttpMessageConverter();

    private String write(Object value) throws Exception {
        MockHttpOutputMessage out = new MockHttpOutputMessage();
        converter.write(value, MediaType.APPLICATION_JSON, out);
        return out.getBodyAsString(StandardCharsets.UTF_8);
    }

    private <T> T read(Class<T> type, String json) throws Exception {
        MockHttpInputMessage in = new MockHttpInputMessage(json.getBytes(StandardCharsets.UTF_8));
        in.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return type.cast(converter.read(type, in));
    }

    @Test
    void traineeProfileJson() throws Exception {
        var profile = new TraineeProfileResponse("John.Smith", "John", "Smith", LocalDate.of(1995, 4, 12),
                "Tashkent", true, List.of(new TrainerSummary("Anna.Lee", "Anna", "Lee",
                new TrainingTypeResponse(6L, "Yoga"))));

        assertEquals("""
                {"username":"John.Smith","firstName":"John","lastName":"Smith","dateOfBirth":"1995-04-12",\
                "address":"Tashkent","isActive":true,"trainers":[{"username":"Anna.Lee","firstName":"Anna",\
                "lastName":"Lee","specialization":{"id":6,"trainingType":"Yoga"}}]}""", write(profile));
    }

    @Test
    void readsIsActiveAndIsoDates() throws Exception {
        TraineeUpdateRequest request = read(TraineeUpdateRequest.class, """
                {"firstName":"John","lastName":"Smith","dateOfBirth":"1995-04-12","isActive":false}""");

        assertEquals(new TraineeUpdateRequest("John", "Smith", LocalDate.of(1995, 4, 12), null, false), request);
    }

    @Test
    void unknownFieldsAreIgnored() throws Exception {
        // e.g. a client sending the username back: it is ignored, so it can't change the username.
        TraineeUpdateRequest request = read(TraineeUpdateRequest.class, """
                {"username":"Hacker","firstName":"John","lastName":"Smith","isActive":true}""");

        assertEquals("John", request.firstName());
    }
}
