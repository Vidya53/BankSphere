package com.cts.apigateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Handles circuit breaker fallback responses for all downstream services.
 * Routes that open their circuit breaker forward here via:
 *   fallbackUri: forward:/fallback/{service-name}
 */
@RestController
@Slf4j
public class FallbackController {

    @RequestMapping("/fallback/{service}")
    public Mono<ResponseEntity<Map<String, Object>>> fallback(ServerWebExchange exchange) {
        String path = exchange.getRequest().getURI().getPath();
        String service = path.substring(path.lastIndexOf('/') + 1);

        log.warn("Circuit breaker triggered for service: {}", service);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("statusCode", HttpStatus.SERVICE_UNAVAILABLE.value());
        body.put("error", "SERVICE_UNAVAILABLE");
        body.put("message", "The " + service + " is temporarily unavailable. Please try again shortly.");
        body.put("timestamp", LocalDateTime.now().toString());

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body));
    }
}
