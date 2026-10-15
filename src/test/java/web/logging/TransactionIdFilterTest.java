package web.logging;

import io.gymcrm.web.logging.TransactionId;
import io.gymcrm.web.logging.TransactionIdFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionIdFilterTest {

    private final TransactionIdFilter filter = new TransactionIdFilter();
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/training-types");
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    /** Runs the filter and returns the transaction id that was current while the request was processed. */
    private String run() throws Exception {
        AtomicReference<String> seen = new AtomicReference<>();
        filter.doFilter(request, response, (req, res) -> seen.set(TransactionId.current()));
        return seen.get();
    }

    @Test
    void generatesIdAndReturnsItInTheResponse() throws Exception {
        String id = run();

        assertTrue(id.matches("[0-9a-f-]{36}"), id);
        assertEquals(id, response.getHeader(TransactionId.HEADER));
    }

    @Test
    void everyRequestGetsItsOwnId() throws Exception {
        String first = run();
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest(), secondResponse, (req, res) -> { });

        assertNotEquals(first, secondResponse.getHeader(TransactionId.HEADER));
    }

    @Test
    void reusesValidIdFromCaller() throws Exception {
        request.addHeader(TransactionId.HEADER, "upstream-123");

        assertEquals("upstream-123", run());
        assertEquals("upstream-123", response.getHeader(TransactionId.HEADER));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "has space", "line\nbreak", "x{}"})
    void replacesUnsafeIdFromCaller(String unsafe) throws Exception {
        request.addHeader(TransactionId.HEADER, unsafe);

        String id = run();

        assertNotEquals(unsafe, id);
        assertTrue(id.matches("[0-9a-f-]{36}"), id);
    }

    @Test
    void replacesTooLongIdFromCaller() throws Exception {
        request.addHeader(TransactionId.HEADER, "a".repeat(65));

        assertEquals(36, run().length());
    }

    @Test
    void clearsIdAfterRequestEvenOnError() {
        assertThrows(IllegalStateException.class, () -> filter.doFilter(request, response, (req, res) -> {
            throw new IllegalStateException("boom");
        }));

        assertNull(TransactionId.current());
    }
}
