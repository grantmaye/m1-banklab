package com.m1banklab.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI m1BanklabOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("m1-banklab API")
                        .version("0.1.0")
                        .description("Backend banking systems lab for account ledgers, transfers, fraud rules, and auditability.")
                        .license(new License().name("MIT")));
    }
}
