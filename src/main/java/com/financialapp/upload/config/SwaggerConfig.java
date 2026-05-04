package com.financialapp.upload.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI uploadOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Upload Service API")
                        .description("Handles PDF and CSV statement imports")
                        .version("1.0.0"));
    }
}
