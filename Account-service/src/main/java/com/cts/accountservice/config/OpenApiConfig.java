package com.cts.accountservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("BankSphere - Account Service API")
                        .version("1.0")
                        .description("Account Service for BankSphere Core Banking Application.\n\n" +
                                "**Testing Headers (pass via Swagger or Postman):**\n" +
                                "- `X-User-Id` — User/Customer ID (default: CUST001)\n" +
                                "- `X-Branch-Code` — Branch code (default: BR001)\n" +
                                "- `X-Customer-Name` — Name (default: Test User)\n" +
                                "- `X-Email` — Email\n" +
                                "- `X-Phone` — Phone\n" +
                                "- `X-Role` — Role: CUSTOMER/CSR/BRANCH_MANAGER (default: CUSTOMER)")
                        .contact(new Contact().name("BankSphere Team").email("support@banksphere.com")));
    }
}
