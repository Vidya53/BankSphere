package com.cts.notificationservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI notificationServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("BankSphere — Notification Service API")
                        .version("v1.0.0")
                        .description("""
                                ## Notification Microservice

                                Consumes events from `banking.notification.events` (Kafka) and
                                dispatches them as email, SMS or push notifications. Exposes
                                ADMIN-only inspection endpoints for the notification log and
                                user-facing endpoints for delivery preferences.

                                ### Delivery pipeline
                                - **EMAIL** — Thymeleaf-rendered templates via SMTP
                                - **SMS**   — Twilio (stub by default)
                                - **PUSH**  — Firebase Cloud Messaging (stub by default)

                                DND windows and per-user rate limits are enforced before dispatch;
                                failed deliveries are retried every 5 minutes up to `max-attempts`
                                before landing on the DLT topic.
                                """)
                        .contact(new Contact()
                                .name("BankSphere Platform Team")
                                .email("platform@banksphere.com"))
                        .license(new License().name("Proprietary — BankSphere Internal")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME,
                        new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste a JWT issued by `POST /auth/login` via the API Gateway.")));
    }
}
