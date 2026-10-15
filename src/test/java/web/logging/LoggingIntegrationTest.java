package web.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import io.gymcrm.web.logging.TransactionId;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import support.LogCapture;
import support.WebTestBase;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LoggingIntegrationTest extends WebTestBase {

    @Test
    void everyResponseHasATransactionId() throws Exception {
        mvc.perform(get("/api/training-types").header("Authorization", john))
                .andExpect(header().string(TransactionId.HEADER, org.hamcrest.Matchers.matchesPattern("[0-9a-f-]{36}")));
        // Also for errors that never reach a controller.
        mvc.perform(get("/api/nothing-here"))
                .andExpect(status().isNotFound())
                .andExpect(header().exists(TransactionId.HEADER));
    }

    @Test
    void allLogLinesOfARequestShareItsTransactionId() throws Exception {
        try (LogCapture logs = new LogCapture("io.gymcrm")) {
            MvcResult result = mvc.perform(json(put("/api/trainees/John.Smith"), john, """
                            {"firstName":"Johnny","lastName":"Smith","isActive":true}"""))
                    .andExpect(status().isOk())
                    .andReturn();
            String id = result.getResponse().getHeader(TransactionId.HEADER);

            List<ILoggingEvent> events = logs.events();
            Set<String> ids = events.stream()
                    .map(e -> e.getMDCPropertyMap().get(TransactionId.MDC_KEY))
                    .collect(Collectors.toSet());
            assertEquals(Set.of(id), ids, "every line should carry the request's id");

            // Lines from different layers: the filter, the facade/services and the logging filter again.
            List<String> loggers = events.stream().map(ILoggingEvent::getLoggerName).distinct().toList();
            assertTrue(loggers.contains("io.gymcrm.web.logging.RestCallLoggingFilter"), loggers.toString());
            assertTrue(loggers.contains("io.gymcrm.services.implementations.TraineeServiceImpl"), loggers.toString());
        }
    }

    @Test
    void idFromCallerIsUsed() throws Exception {
        mvc.perform(get("/api/training-types").header("Authorization", john).header(TransactionId.HEADER, "upstream-42"))
                .andExpect(header().string(TransactionId.HEADER, "upstream-42"));
    }
}
