package io.gymcrm.web.logging;

import jakarta.servlet.Filter;

import java.util.List;

/**
 * The servlet filters, in the order they must run. {@code Application} registers them with Tomcat,
 * and the web tests add them to MockMvc, so both always use the same list.
 */
public final class WebFilters {

    private WebFilters() {
    }

    public static List<Filter> all() {
        return List.of(new TransactionIdFilter(), new RestCallLoggingFilter());
    }
}
