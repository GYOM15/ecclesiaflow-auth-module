package com.ecclesiaflow.springsecurity.application.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires pour AsyncConfig.
 * <p>
 * Vérifie que la configuration asynchrone est correctement mise en place.
 * </p>
 */
@DisplayName("AsyncConfig - Tests Unitaires")
class AsyncConfigTest {

    @Test
    @DisplayName("Devrait avoir l'annotation @Configuration")
    void shouldHaveConfigurationAnnotation() {
        // Given
        Class<AsyncConfig> configClass = AsyncConfig.class;

        // When
        boolean hasConfiguration = configClass.isAnnotationPresent(Configuration.class);

        // Then
        assertThat(hasConfiguration)
                .as("AsyncConfig devrait avoir @Configuration")
                .isTrue();
    }

    @Test
    @DisplayName("Devrait avoir l'annotation @EnableAsync")
    void shouldHaveEnableAsyncAnnotation() {
        // Given
        Class<AsyncConfig> configClass = AsyncConfig.class;

        // When
        boolean hasEnableAsync = configClass.isAnnotationPresent(EnableAsync.class);

        // Then
        assertThat(hasEnableAsync)
                .as("AsyncConfig devrait avoir @EnableAsync pour activer l'exécution asynchrone")
                .isTrue();
    }

    @Test
    @DisplayName("Devrait être instanciable")
    void shouldBeInstantiable() {
        // When
        AsyncConfig asyncConfig = new AsyncConfig();

        // Then
        assertThat(asyncConfig)
                .as("AsyncConfig devrait pouvoir être instancié")
                .isNotNull();
    }

    @Test
    @DisplayName("Devrait configurer @EnableAsync avec mode proxy par défaut")
    void shouldConfigureEnableAsyncWithProxyMode() {
        // Given
        Class<AsyncConfig> configClass = AsyncConfig.class;
        EnableAsync enableAsync = configClass.getAnnotation(EnableAsync.class);

        // Then
        assertThat(enableAsync).isNotNull();
        // Par défaut, EnableAsync utilise le mode proxy
        assertThat(enableAsync.mode().name())
                .as("EnableAsync devrait utiliser le mode PROXY par défaut")
                .isEqualTo("PROXY");
    }
}
