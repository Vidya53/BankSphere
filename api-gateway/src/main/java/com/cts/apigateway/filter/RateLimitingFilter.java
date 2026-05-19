package com.cts.apigateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Token-bucket rate limiter applied at the gateway before JWT validation.
 *
 * Limits: 100 requests per minute per IP address (configurable via constants).
 * Identified by X-Forwarded-For → client IP → bucket.
 *
 * NOTE: This is an in-process, single-instance implementation suitable for
 * development and single-node deployments. For multi-node production, replace
 * with Redis-backed rate limiting (e.g., spring-cloud-gateway's built-in
 * RequestRateLimiter filter with RedisRateLimiter).
 */
@Component
@Slf4j
public class RateLimitingFilter implements GlobalFilter, Ordered {

    private static final int MAX_REQUESTS_PER_WINDOW = 100;
    private static final long WINDOW_MS = 60_000L;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // IP → [request count, window start timestamp]
    private final ConcurrentHashMap<String, long[]> buckets = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String clientIp = resolveClientIp(exchange);
        if (isRateLimited(clientIp)) {
            log.warn("Rate limit exceeded for IP: {}", clientIp);
            return rateLimitResponse(exchange);
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -2; // Runs before JwtAuthenticationFilter (order = -1)
    }

    private boolean isRateLimited(String clientIp) {
        long now = System.currentTimeMillis();
        long[] bucket = buckets.compute(clientIp, (ip, existing) -> {
            if (existing == null || now - existing[1] >= WINDOW_MS) {
                return new long[]{1L, now};
            }
            existing[0]++;
            return existing;
        });
        return bucket[0] > MAX_REQUESTS_PER_WINDOW;
    }

    private String resolveClientIp(ServerWebExchange exchange) {
        String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        var remoteAddress = exchange.getRequest().getRemoteAddress();
        return remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : "unknown";
    }

    private Mono<Void> rateLimitResponse(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().add("Retry-After", "60");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("statusCode", 429);
        body.put("error", "RATE_LIMIT_EXCEEDED");
        body.put("message", "Too many requests. Please retry after 60 seconds.");
        body.put("timestamp", LocalDateTime.now().toString());

        byte[] bytes;
        try {
            bytes = MAPPER.writeValueAsBytes(body);
        } catch (JsonProcessingException e) {
            bytes = "{\"error\":\"RATE_LIMIT_EXCEEDED\"}".getBytes();
        }

        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(bytes))
        );
    }
}
