package com.cts.customerservices.config;

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
    public OpenAPI customerServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("BankSphere — Customer Service API")
                        .version("v1.0.0")
                        .description("""
                                ## Customer Microservice

                                Manages customer profiles, KYC submissions, support tickets, the CSR
                                dashboard and loan-eligibility evaluation for the **BankSphere** platform.

                                ### Calling endpoints directly
                                When you call this service on its own port (bypassing the gateway) the
                                gateway-injected headers are missing. To try secured endpoints from
                                Swagger UI, set the following headers in *Authorize* or via cURL:

                                - `X-User-Id` — caller's identity-service user id
                                - `X-Role`    — `CUSTOMER` / `CSR` / `BRANCH_MANAGER` / `LOAN_OFFICER` / `ADMIN`
                                - `X-Branch-Code` — staff branch (where applicable)

                                ### Going through the gateway
                                Hit `http://localhost:8090/customers/...` with `Authorization: Bearer <jwt>`
                                instead — the gateway will validate the JWT and inject those headers for you.
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
