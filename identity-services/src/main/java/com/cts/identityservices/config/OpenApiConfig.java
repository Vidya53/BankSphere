package com.cts.identityservices.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI identityServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("BankSphere Identity Service API")
                        .description("Authentication and user identity management endpoints for the BankSphere platform.")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("BankSphere Team")
                                .email("support@banksphere.local"))
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}