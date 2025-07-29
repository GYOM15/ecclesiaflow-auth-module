package com.ecclesiaflow.springsecurity.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration OpenAPI pour la documentation de l'API EcclesiaFlow Auth
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ecclesiaFlowOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("EcclesiaFlow Authentication API")
                        .description("API d'authentification et de gestion des membres pour EcclesiaFlow")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("EcclesiaFlow Team")
                                .email("support@ecclesiaflow.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .addSecurityItem(new SecurityRequirement()
                        .addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication", createAPIKeyScheme()));
    }

    private SecurityScheme createAPIKeyScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .bearerFormat("JWT")
                .scheme("bearer");
    }
}
