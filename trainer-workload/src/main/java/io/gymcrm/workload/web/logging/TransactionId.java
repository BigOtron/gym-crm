package io.gymcrm.workload.web.logging;

import org.slf4j.MDC;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * The id that ties together all log lines of one request. It is kept in SLF4J's MDC, so Logback
 * prints it in every line ({@code %X{transactionId}}), and sent back in the {@value #HEADER} header.
 */
public final class TransactionId {

    /** Request and response header. A caller may send its own id, which is then reused. */
    public static final String HEADER = "X-Transaction-Id";

    /** MDC key, used in the Logback pattern. */
    public static final String MDC_KEY = "transactionId";

    /** Only short, plain ids from callers are reused, so nobody can write arbitrary text into our logs. */
    private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9-]{1,64}");

    private TransactionId() {
    }

    /**
     * The current request's id, or {@code null} outside a request. Pass it on in the
     * {@value #HEADER} header when calling another service, so its logs can be matched with ours.
     */
    public static String current() {
        return MDC.get(MDC_KEY);
    }

    /** The caller's id if it is a valid one, otherwise a new id. */
    public static String reuseOrCreate(String incoming) {
        return incoming != null && VALID_ID.matcher(incoming).matches() ? incoming : UUID.randomUUID().toString();
    }
}
