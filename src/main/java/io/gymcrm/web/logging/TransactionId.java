package io.gymcrm.web.logging;

import org.slf4j.MDC;

/**
 * The id that ties together all log lines of one request. It is kept in SLF4J's MDC, so Logback
 * prints it in every line ({@code %X{transactionId}}), and sent back in the {@value #HEADER} header.
 */
public final class TransactionId {

    /** Request and response header. A caller may send its own id, which is then reused. */
    public static final String HEADER = "X-Transaction-Id";

    /** MDC key, used in the Logback pattern. */
    public static final String MDC_KEY = "transactionId";

    private TransactionId() {
    }

    /**
     * The current request's id, or {@code null} outside a request. Pass it on in the
     * {@value #HEADER} header when calling another service, so its logs can be matched with ours.
     */
    public static String current() {
        return MDC.get(MDC_KEY);
    }
}
