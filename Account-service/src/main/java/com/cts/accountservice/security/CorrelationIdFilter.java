package com.cts.accountservice.security;

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
import java.util.UUID;

/**
 * Reads X-Correlation-ID from the incoming request (injected by the gateway) and
 * stores it in SLF4J MDC so every log line in the request thread carries the ID.
 * Also captures X-User-Id for log correlation without re-parsing the security context.
 *
 * Runs before the Spring Security filter chain (HIGHEST_PRECEDENCE) to ensure the
 * correlation ID is present in MDC before any downstream logging occurs.
 * MDC is cleaned up in the finally block to prevent thread-pool leakage.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";
    public static final String USER_ID_MDC_KEY = "userId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        String userId = request.getHeader("X-User-Id");

        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        if (userId != null && !userId.isBlank()) {
            MDC.put(USER_ID_MDC_KEY, userId);
        }

        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CORRELATION_ID_MDC_KEY);
            MDC.remove(USER_ID_MDC_KEY);
        }
    }
}
