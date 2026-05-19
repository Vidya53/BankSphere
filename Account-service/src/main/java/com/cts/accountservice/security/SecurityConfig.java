package com.cts.accountservice.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Service-level Spring Security configuration.
 *
 * Authentication is the gateway's responsibility — this config:
 *   1. Enables @PreAuthorize (method-level RBAC).
 *   2. Runs HeaderAuthenticationFilter to translate gateway-injected
 *      X-User-Id / X-Role headers into a Spring Security Authentication.
 *   3. Denies direct access to /api/v1/internal/** as belt-and-suspenders.
 *
 * Services never validate JWTs themselves — they trust the headers the
 * gateway injects after it validates the access token.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final HeaderAuthenticationFilter headerAuthenticationFilter;

    /** Hashes transaction PINs before persisting. */
    @Bean
    public PasswordEncoder pinEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(headerAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        // /api/v1/internal/** is blocked at the gateway level for all external traffic.
                        // Services calling these endpoints via Feign carry user context headers,
                        // so authenticated() allows internal service-to-service calls while still
                        // blocking unauthenticated direct access.
                        .requestMatchers("/api/v1/internal/**").authenticated()
                        .requestMatchers("/actuator/**").permitAll()
                        // Swagger / OpenAPI — open so developers can browse the API contract
                        // without first acquiring a JWT. Try-it-out still needs gateway headers.
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html",
                                         "/v3/api-docs/**", "/v3/api-docs.yaml",
                                         "/swagger-resources/**", "/webjars/**").permitAll()
                        // Everything else: authenticated (header filter sets the Authentication)
                        .anyRequest().authenticated()
                )
                .build();
    }
}
