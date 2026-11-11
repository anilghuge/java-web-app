package com.banking.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI bankingOpenAPI() {
        final String scheme = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Enterprise Banking Management System API")
                        .description("""
                                Production-style banking APIs for customers, accounts, transactions,
                                beneficiaries, loans, cards, analytics and administration.

                                **Demo credentials**
                                - Admin: `admin@bank.com` / `Admin@123`
                                - Manager: `manager@bank.com` / `Manager@123`
                                - Employee: `employee@bank.com` / `Employee@123`
                                - Customer: `alice@example.com` / `Customer@123`

                                Use **Authorize** with the JWT access token returned by `/api/auth/login`.
                                All monetary amounts use INR and two-decimal precision.
                                """)
                        .version("1.0.0")
                        .contact(new Contact().name("Banking Platform Team").email("engineering@bank.com"))
                        .license(new License().name("MIT")))
                .servers(List.of(new Server().url("/").description("Current server")))
                .components(new Components().addSecuritySchemes(scheme,
                        new SecurityScheme()
                                .name(scheme)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste the access token from /api/auth/login")))
                .addSecurityItem(new SecurityRequirement().addList(scheme));
    }
}
