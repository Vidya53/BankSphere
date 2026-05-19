package com.cts.apigateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Central JWT authentication and authorization filter for the API Gateway.
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │  REQUEST FLOW                                                               │
 * │                                                                             │
 * │  Client ──► Gateway Filter ──► (validates JWT) ──► Downstream Service      │
 * │                    │                                       ▲                │
 * │                    │  strips Authorization header          │                │
 * │                    │  injects: X-User-Id, X-Role,          │                │
 * │                    │           X-Branch-Code, X-Email,     │                │
 * │                    │           X-Customer-Name             │                │
 * │                    └──────────────────────────────────────►┘                │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * Enforcement order:
 *  1. Block /api/v1/internal/** — internal endpoints are unreachable from the gateway
 *  2. Pass /auth/** and /actuator/health without a token
 *  3. Require and validate Bearer access token
 *     — EXPIRED → 401 TOKEN_EXPIRED  (client should call POST /auth/refresh)
 *     — INVALID  → 401 TOKEN_INVALID  (client should re-login)
 *  4. Enforce coarse-grained route-level RBAC via RouteAuthorizationConfig
 *     — 403 FORBIDDEN if role not permitted for this path
 *  5. Strip Authorization header; inject authenticated user context as headers
 *     — Services trust these headers and use them for fine-grained @PreAuthorize
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final AntPathMatcher PM = new AntPathMatcher();
    private static final String INTERNAL_PATTERN = "/api/v1/internal/**";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final GatewayJwtUtil jwtUtil;
    private final RouteAuthorizationConfig routeAuth;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path   = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod().name();

        // ── Correlation ID: generate if not present, echo on all responses ────
        String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        final String finalCorrelationId = correlationId;
        exchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, finalCorrelationId);

        // ── CORS preflight — let CorsWebFilter handle it, skip auth entirely ──
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return chain.filter(exchange);
        }

        // ── Step 1: Hard-block internal endpoints ─────────────────────────────
        if (PM.match(INTERNAL_PATTERN, path)) {
            log.warn("Blocked gateway access to internal endpoint: {} {}", method, path);
            return errorResponse(exchange, HttpStatus.FORBIDDEN, "FORBIDDEN",
                    "Internal endpoints are not accessible through the public gateway");
        }

        // ── Step 2: Public paths pass through with correlation ID only ────────
        if (routeAuth.isPublic(path)) {
            ServerHttpRequest withCorrelationId = exchange.getRequest().mutate()
                    .header(CORRELATION_ID_HEADER, finalCorrelationId)
                    .build();
            return chain.filter(exchange.mutate().request(withCorrelationId).build());
        }

        // ── Step 3: Extract and validate Bearer token ─────────────────────────
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return errorResponse(exchange, HttpStatus.UNAUTHORIZED, "TOKEN_MISSING",
                    "Authorization header is required: Authorization: Bearer <access_token>");
        }

        String token = authHeader.substring(7).trim();
        GatewayJwtUtil.TokenStatus status = jwtUtil.validate(token);

        if (status == GatewayJwtUtil.TokenStatus.EXPIRED) {
            log.debug("Expired access token for path: {} {}", method, path);
            return errorResponse(exchange, HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED",
                    "Access token has expired. Call POST /auth/refresh with your refresh token to obtain a new pair.");
        }
        if (status == GatewayJwtUtil.TokenStatus.INVALID) {
            log.warn("Invalid access token rejected for path: {} {}", method, path);
            return errorResponse(exchange, HttpStatus.UNAUTHORIZED, "TOKEN_INVALID",
                    "Access token is invalid or has been tampered with. Please log in again.");
        }

        // ── Step 4: Coarse-grained RBAC at the gateway level ─────────────────
        Claims claims = jwtUtil.extractAllClaims(token);
        String role = claims.get("role", String.class);

        if (!routeAuth.isAuthorized(path, role)) {
            log.warn("Access denied: role={} method={} path={}", role, method, path);
            return errorResponse(exchange, HttpStatus.FORBIDDEN, "FORBIDDEN",
                    "Your role (" + role + ") does not have access to this resource.");
        }

        // ── Step 5: Forward with injected identity headers ────────────────────
        String userId     = claims.getSubject();
        String email      = claims.get("email",      String.class);
        String fullName   = claims.get("fullName",   String.class);
        String branchCode = claims.get("branchCode", String.class);

        ServerHttpRequest mutated = exchange.getRequest().mutate()
                .header("X-User-Id",           userId)
                .header("X-Username",          email)
                .header("X-Role",              role)
                .header("X-Branch-Code",       branchCode != null ? branchCode : "")
                .header("X-Email",             email)
                .header("X-Customer-Name",     fullName != null ? fullName : "")
                .header(CORRELATION_ID_HEADER, finalCorrelationId)
                // Strip the JWT — services operate on trusted headers only
                .headers(h -> h.remove(HttpHeaders.AUTHORIZATION))
                .build();

        log.debug("Authenticated: userId={} role={} correlationId={} path={}", userId, role, finalCorrelationId, path);
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    @Override
    public int getOrder() {
        return -1; // Execute before all gateway route filters
    }

    // ── Error response helpers ─────────────────────────────────────────────────

    private Mono<Void> errorResponse(ServerWebExchange exchange, HttpStatus status,
                                     String errorCode, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success",    false);
        body.put("statusCode", status.value());
        body.put("error",      errorCode);
        body.put("message",    message);
        body.put("timestamp",  LocalDateTime.now().toString());

        byte[] bytes;
        try {
            bytes = MAPPER.writeValueAsBytes(body);
        } catch (JsonProcessingException e) {
            bytes = ("{\"error\":\"" + errorCode + "\"}").getBytes();
        }

        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(bytes))
        );
    }
}
