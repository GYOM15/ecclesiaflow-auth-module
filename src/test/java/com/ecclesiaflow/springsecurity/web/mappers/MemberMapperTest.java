package com.ecclesiaflow.springsecurity.web.mappers;

import com.ecclesiaflow.springsecurity.business.domain.password.SigninCredentials;
import com.ecclesiaflow.springsecurity.web.model.SigninRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitaires pour MemberMapper
 * 
 * Teste les conversions entre modèles OpenAPI et objets métier pour garantir
 * l'isolation correcte entre la couche web et la couche métier.
 */
@DisplayName("MemberMapper - Tests de conversion")
class MemberMapperTest {




    @Test
    @DisplayName("Devrait convertir SigninRequest vers SigninCredentials correctement")
    void shouldConvertSigninRequestToSigninCredentials() {
        // Given
        SigninRequest request = new SigninRequest();
        request.setEmail("user@example.com");
        request.setPassword("myPassword");

        // When
        SigninCredentials credentials = MemberMapper.fromSigninRequest(request);

        // Then
        assertThat(credentials).isNotNull();
        assertThat(credentials.email()).isEqualTo("user@example.com");
        assertThat(credentials.password()).isEqualTo("myPassword");
    }

    @Test
    @DisplayName("Devrait gérer SigninRequest avec des valeurs nulles")
    void shouldHandleSigninRequestWithNullValues() {
        // Given
        SigninRequest request = new SigninRequest();
        request.setEmail(null);
        request.setPassword(null);

        // When
        SigninCredentials credentials = MemberMapper.fromSigninRequest(request);

        // Then
        assertThat(credentials).isNotNull();
        assertThat(credentials.email()).isNull();
        assertThat(credentials.password()).isNull();
    }

    @Test
    @DisplayName("Devrait lancer NullPointerException pour SigninRequest null")
    void shouldThrowNullPointerExceptionForNullSigninRequest() {
        // When & Then
        assertThatThrownBy(() -> MemberMapper.fromSigninRequest(null))
                .isInstanceOf(NullPointerException.class);
    }
}
