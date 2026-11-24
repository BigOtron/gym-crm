package io.gymcrm.workload.web.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TransactionIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String id = TransactionId.reuseOrCreate(request.getHeader(TransactionId.HEADER));

        MDC.put(TransactionId.MDC_KEY, id);
        response.setHeader(TransactionId.HEADER, id);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(TransactionId.MDC_KEY);
        }
    }
}
