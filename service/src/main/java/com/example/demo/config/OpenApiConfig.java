package com.example.demo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI shaftTempOpenApi() {
        return new OpenAPI().info(
            new Info()
                .title("ShaftTemp API")
                .version("v1")
                .description("测温物联网系统接口文档")
                .contact(new Contact().name("ShaftTemp"))
        );
    }
}
