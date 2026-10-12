package com.dws.challenge.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("DWS Dev Challenge - Account Transfer Service")
                        .version("1.0.0")
                        .description("REST API exposing account management and concurrent fund transfer operations")
                        .license(new License().name("Apache 2.0")));
    }
}