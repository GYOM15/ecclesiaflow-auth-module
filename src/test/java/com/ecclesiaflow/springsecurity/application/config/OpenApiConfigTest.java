package com.ecclesiaflow.springsecurity.application.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@ExtendWith(MockitoExtension.class)
@DisplayName("OpenApiConfig - Tests de configuration OpenAPI")
class OpenApiConfigTest {

    @InjectMocks
    private OpenApiConfig openApiConfig;

    @Test
    @DisplayName("Devrait configurer l'objet OpenAPI avec les informations et les schémas de sécurité corrects")
    void shouldConfigureOpenApiWithCorrectInfoAndSecuritySchemes() {
        // When
        OpenAPI openAPI = openApiConfig.ecclesiaFlowOpenAPI();

        // Then
        assertAll(
                () -> assertThat(openAPI.getInfo().getTitle()).isEqualTo("EcclesiaFlow Authentication API"),
                () -> assertThat(openAPI.getInfo().getDescription()).isEqualTo("API d'authentification et de gestion des membres pour EcclesiaFlow"),
                () -> assertThat(openAPI.getInfo().getVersion()).isEqualTo("1.0.0"),
                () -> assertThat(openAPI.getInfo().getContact().getName()).isEqualTo("EcclesiaFlow Team"),
                () -> assertThat(openAPI.getInfo().getContact().getEmail()).isEqualTo("support@ecclesiaflow.com"),
                () -> assertThat(openAPI.getInfo().getLicense().getName()).isEqualTo("MIT License"),
                () -> assertThat(openAPI.getInfo().getLicense().getUrl()).isEqualTo("https://opensource.org/licenses/MIT"),
                () -> {
                    // Vérification du SecurityRequirement
                    assertThat(openAPI.getSecurity()).hasSize(1);
                    SecurityRequirement securityRequirement = openAPI.getSecurity().get(0);
                    assertThat(securityRequirement).containsKey("Bearer Authentication");
                },
                () -> {
                    // Vérification du SecurityScheme
                    SecurityScheme securityScheme = openAPI.getComponents().getSecuritySchemes().get("Bearer Authentication");
                    assertThat(securityScheme).isNotNull();
                    assertThat(securityScheme.getType()).isEqualTo(SecurityScheme.Type.HTTP);
                    assertThat(securityScheme.getScheme()).isEqualTo("bearer");
                    assertThat(securityScheme.getBearerFormat()).isEqualTo("JWT");
                }
        );
    }
}