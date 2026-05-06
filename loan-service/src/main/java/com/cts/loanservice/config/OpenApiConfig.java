package com.cts.loanservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        try {
            Server devServer = new Server();
            devServer.setUrl("http://localhost:8085");
            devServer.setDescription("Development environment");

            Contact contact = new Contact();
            contact.setEmail("support@banksphere.com");
            contact.setName("BankSphere Support");

            License mitLicense = new License()
                    .name("MIT");

            Info info = new Info()
                    .title("Loan Service API")
                    .version("1.0.0")
                    .description("Loan Management Microservice")
                    .contact(contact)
                    .license(mitLicense);

            return new OpenAPI()
                    .info(info)
                    .servers(List.of(devServer));
        } catch (Exception e) {
            // Return minimal OpenAPI if any error occurs
            return new OpenAPI()
                    .info(new Info()
                            .title("Loan Service API")
                            .version("1.0.0"));
        }
    }
}

