package com.cts.transactionservice.config;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
@Configuration
public class OpenApiConfig {
    @Value("${server.port:8082}")
    private String serverPort;
    private static final String SECURITY_SCHEME_NAME = "BearerAuth";
    @Bean
    public OpenAPI transactionServiceOpenAPI() {
        return new OpenAPI()
                .info(buildApiInfo())
                .servers(buildServers())
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(buildSecurityComponents())
                .externalDocs(new ExternalDocumentation()
                        .description("BankSphere Wiki & Architecture Guide")
                        .url("https://github.com/banksphere/docs"));
    }
    private Info buildApiInfo() {
        return new Info()
                .title("BankSphere — Transaction Service API")
                .version("v1.0.0")
                .description("""
                        ## Transaction Microservice
                        
                        Handles all financial transaction operations for the **BankSphere** banking platform.
                        
                        ### Capabilities
                        - **Initiate** transactions (Transfer, Deposit, Withdrawal, Payment, Refund, Reversal)
                        - **Idempotency** — duplicate requests are safely detected and rejected
                        - **Lifecycle management** — Cancel, Reverse, Mark as Success / Failed
                        - **Account statements** — paginated history with date-range filtering
                        - **Analytics** — daily limits, velocity checks, aggregation
                        
                        ### Authentication
                        All endpoints require a valid **JWT Bearer token** in the `Authorization` header.  
                        Internal lifecycle endpoints (`/success`, `/fail`) are additionally restricted
                        to the payment-processing engine at the API gateway level.
                        
                        ### Idempotency
                        Supply a unique `idempotencyKey` in the request body for every `POST` to
                        prevent duplicate transaction submission on network retries.
                        """)
                .contact(new Contact()
                        .name("BankSphere Platform Team")
                        .email("platform@banksphere.com")
                        .url("https://banksphere.com"))
                .license(new License()
                        .name("Proprietary — BankSphere Internal")
                        .url("https://banksphere.com/license"));
    }
    private List<Server> buildServers() {
        Server devServer = new Server()
                .url("http://localhost:" + serverPort)
                .description("Local Development Server");

        Server stagingServer = new Server()
                .url("https://staging-api.banksphere.com/transaction-service")
                .description("Staging / QA Server");

        Server prodServer = new Server()
                .url("https://api.banksphere.com/transaction-service")
                .description("Production Server");
        return List.of(devServer, stagingServer, prodServer);
    }
    private Components buildSecurityComponents() {
        return new Components()
                .addSecuritySchemes(SECURITY_SCHEME_NAME,
                        new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description(
                                        "Provide a valid JWT token obtained from the Auth Service. "
                                        + "Format: `Bearer <token>`"));
    }
}

