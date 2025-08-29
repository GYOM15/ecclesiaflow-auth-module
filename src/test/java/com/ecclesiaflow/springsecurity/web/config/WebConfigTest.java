package com.ecclesiaflow.springsecurity.web.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires pour WebConfig
 * <p>
 * Teste la configuration des intercepteurs de l'API.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WebConfig - Tests de configuration des intercepteurs")
class WebConfigTest {

    @Mock
    private RateLimitingInterceptor rateLimitingInterceptor;

    @Mock
    private InterceptorRegistry registry;

    @Mock
    private InterceptorRegistration registration;

    @InjectMocks
    private WebConfig webConfig;

    @Captor
    private ArgumentCaptor<String[]> pathPatternsCaptor;

    @Test
    @DisplayName("Devrait ajouter l'intercepteur de limitation de débit avec les chemins d'accès corrects")
    void shouldAddRateLimitingInterceptorWithCorrectPathPatterns() {
        // Given
        when(registry.addInterceptor(rateLimitingInterceptor)).thenReturn(registration);

        // When
        webConfig.addInterceptors(registry);

        // Then
        // 1. Vérifie que l'intercepteur a bien été ajouté au registre
        verify(registry).addInterceptor(rateLimitingInterceptor);

        // 2. Capture les chemins d'accès passés à la méthode addPathPatterns
        verify(registration).addPathPatterns(pathPatternsCaptor.capture());
        String[] capturedPatterns = pathPatternsCaptor.getValue();

        // 3. Vérifie que les chemins d'accès capturés sont corrects
        assertThat(capturedPatterns)
                .isNotNull()
                .containsExactlyInAnyOrder(
                        "/ecclesiaflow/auth/**",
                        "/ecclesiaflow/members/signup"
                );
    }
}