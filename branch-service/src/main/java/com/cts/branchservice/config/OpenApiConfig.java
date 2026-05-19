package com.cts.branchservice.config;

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
    public OpenAPI branchServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("BankSphere — Branch Service API")
                        .version("v1.0.0")
                        .description("""
                                ## Branch & Employee Microservice

                                Manages branches, their operating hours, employees and the
                                branch-manager dashboard for **BankSphere**.

                                ### Authentication
                                Every request needs gateway-injected `X-User-Id`, `X-Role` and
                                (for branch-scoped actions) `X-Branch-Code` headers.
                                Easiest path: call the service through the API gateway at
                                `http://localhost:8090/api/v1/branches/...` with a Bearer JWT.
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
