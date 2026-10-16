package io.gymcrm.web.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RestCallLoggingFilter extends OncePerRequestFilter {

    private static final int MAX_BODY = 2000;

    /** {@code "password":"x"}, {@code "oldPassword":"x"}, {@code "newPassword":"x"} → {@code "...":"***"} */
    private static final Pattern PASSWORD = Pattern.compile("(\"(?:password|oldPassword|newPassword)\"\\s*:\\s*)\"(?:[^\"\\\\]|\\\\.)*\"");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        ContentCachingRequestWrapper cachedRequest = new ContentCachingRequestWrapper(request, MAX_BODY + 1);
        ContentCachingResponseWrapper cachedResponse = new ContentCachingResponseWrapper(response);
        String endpoint = request.getMethod() + " " + request.getRequestURI()
                + (request.getQueryString() == null ? "" : "?" + request.getQueryString());
        long start = System.nanoTime();

        log.info("Request {}", endpoint);
        try {
            chain.doFilter(cachedRequest, cachedResponse);
        } finally {
            long millis = (System.nanoTime() - start) / 1_000_000;
            int status = cachedResponse.getStatus();
            String requestBody = body(cachedRequest.getContentAsByteArray());
            String responseBody = body(cachedResponse.getContentAsByteArray());

            String message = "Response {} {} in {} ms, request body: {}, response body: {}";
            if (status >= 500) {
                log.error(message, endpoint, status, millis, requestBody, responseBody);
            } else if (status >= 400) {
                log.warn(message, endpoint, status, millis, requestBody, responseBody);
            } else {
                log.info(message, endpoint, status, millis, requestBody, responseBody);
            }
            cachedResponse.copyBodyToResponse();
        }
    }

    static String body(byte[] content) {
        if (content.length == 0) {
            return "<empty>";
        }
        String text = new String(content, 0, Math.min(content.length, MAX_BODY), StandardCharsets.UTF_8);
        text = PASSWORD.matcher(text).replaceAll("$1\"***\"");
        return content.length > MAX_BODY ? text + "...(truncated)" : text;
    }
}
