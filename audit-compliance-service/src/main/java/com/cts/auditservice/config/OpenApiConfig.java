package com.cts.auditservice.config;

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
    public OpenAPI auditServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("BankSphere — Audit & Compliance Service API")
                        .version("v1.0.0")
                        .description("""
                                ## Audit & Compliance Microservice

                                Append-only audit log for **BankSphere** plus the cross-service
                                analytics dashboard (spend, revenue, loan portfolio, compliance
                                metrics, customer insights).

                                ### Data flow
                                - **Producers** (account / loan / transaction / customer / identity)
                                  publish `AuditEventMessage` to Kafka topic `banking.audit.events`.
                                - **Consumer** (this service) persists events to `audit_logs`,
                                  deduplicated on `eventId`.
                                - Entities are immutable — `@PreUpdate`/`@PreRemove` reject any
                                  mutation attempt.

                                ### Access control
                                `/api/v1/audit/**` endpoints are ADMIN-only (COMPLIANCE_OFFICER
                                role has been retired). `/api/analytics/**` is open to all staff
                                roles. There is also a REST fallback ingest at
                                `POST /api/v1/internal/audit` for services that publish over HTTP
                                instead of Kafka.
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
