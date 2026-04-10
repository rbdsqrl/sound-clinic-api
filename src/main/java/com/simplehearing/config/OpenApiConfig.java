package com.simplehearing.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI simpleHearingOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Simple Hearing API")
                .description("API contract for the Simple Hearing backend.")
                .version("1.0.0")
            );
    }
}
