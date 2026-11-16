package io.gymcrm.web.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Level 1 of the logging (transaction level): gives every request a transaction id.
 * Must be the first filter, so everything after it, including the other filters, logs with the id.
 */
public class TransactionIdFilter extends OncePerRequestFilter {

    /** Only accept "safe" ids from callers: an id ends up in every log line, so no spaces or line breaks. */
    private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9-]{1,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String incoming = request.getHeader(TransactionId.HEADER);
        String id = incoming != null && VALID_ID.matcher(incoming).matches()
                ? incoming
                : UUID.randomUUID().toString();

        MDC.put(TransactionId.MDC_KEY, id);
        response.setHeader(TransactionId.HEADER, id);
        try {
            chain.doFilter(request, response);
        } finally {
            // Tomcat reuses threads; without this the next request on this thread would log our id.
            MDC.remove(TransactionId.MDC_KEY);
        }
    }
}
