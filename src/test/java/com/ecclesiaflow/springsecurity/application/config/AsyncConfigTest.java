package com.ecclesiaflow.springsecurity.application.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AsyncConfig.
 * <p>
 * Tests that asynchronous configuration is properly set up.
 * </p>
 */
@DisplayName("AsyncConfig - Unit Tests")
class AsyncConfigTest {

    @Test
    @DisplayName("Should have @Configuration annotation")
    void shouldHaveConfigurationAnnotation() {
        // Given
        Class<AsyncConfig> configClass = AsyncConfig.class;

        // When
        boolean hasConfiguration = configClass.isAnnotationPresent(Configuration.class);

        // Then
        assertThat(hasConfiguration)
                .as("AsyncConfig should have @Configuration")
                .isTrue();
    }

    @Test
    @DisplayName("Should have @EnableAsync annotation")
    void shouldHaveEnableAsyncAnnotation() {
        // Given
        Class<AsyncConfig> configClass = AsyncConfig.class;

        // When
        boolean hasEnableAsync = configClass.isAnnotationPresent(EnableAsync.class);

        // Then
        assertThat(hasEnableAsync)
                .as("AsyncConfig should have @EnableAsync to enable asynchronous execution")
                .isTrue();
    }

    @Test
    @DisplayName("Should be instantiable")
    void shouldBeInstantiable() {
        // When
        AsyncConfig asyncConfig = new AsyncConfig();

        // Then
        assertThat(asyncConfig)
                .as("AsyncConfig should be instantiable")
                .isNotNull();
    }

    @Test
    @DisplayName("Should configure @EnableAsync with default proxy mode")
    void shouldConfigureEnableAsyncWithProxyMode() {
        // Given
        Class<AsyncConfig> configClass = AsyncConfig.class;
        EnableAsync enableAsync = configClass.getAnnotation(EnableAsync.class);

        // Then
        assertThat(enableAsync).isNotNull();
        // By default, EnableAsync uses proxy mode
        assertThat(enableAsync.mode().name())
                .as("EnableAsync should use PROXY mode by default")
                .isEqualTo("PROXY");
    }
}
