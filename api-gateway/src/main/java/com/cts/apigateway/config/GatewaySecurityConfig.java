package com.cts.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security WebFlux configuration for the API Gateway.
 *
 * Authentication and authorization are handled entirely by JwtAuthenticationFilter (GlobalFilter).
 * This config disables Spring Security's built-in auth so the two don't conflict.
 * All HTTP-level security (CORS, CSRF, session) is managed here; business-level access control
 * lives in JwtAuthenticationFilter and service-level @PreAuthorize annotations.
 */
@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                // Permit all at the Spring Security level — JwtAuthenticationFilter enforces access control
                .authorizeExchange(exchange -> exchange.anyExchange().permitAll())
                .build();
    }

    /**
     * CORS — allows the React dev server (Vite on 5173/5174) and configurable production
     * origins to call the gateway. Preflight responses include the headers the frontend
     * sends (Authorization, X-Correlation-ID, etc.) and exposes the correlation ID back
     * so the client can correlate requests in DevTools.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOriginPatterns(List.of(
                "http://localhost:[*]",
                "http://127.0.0.1:[*]"
        ));
        cfg.setAllowedMethods(List.of(
                HttpMethod.GET.name(),
                HttpMethod.POST.name(),
                HttpMethod.PUT.name(),
                HttpMethod.PATCH.name(),
                HttpMethod.DELETE.name(),
                HttpMethod.OPTIONS.name()
        ));
        cfg.setAllowedHeaders(List.of(
                HttpHeaders.AUTHORIZATION,
                HttpHeaders.CONTENT_TYPE,
                HttpHeaders.ACCEPT,
                "X-Correlation-ID",
                "X-Requested-With"
        ));
        cfg.setExposedHeaders(List.of("X-Correlation-ID"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    /**
     * Explicit CorsWebFilter bean — gives CORS the highest priority so preflight (OPTIONS)
     * requests never hit JwtAuthenticationFilter. Without this, a preflight from the
     * browser is treated as an unauthenticated request and rejected.
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        return new CorsWebFilter(corsConfigurationSource());
    }
}
