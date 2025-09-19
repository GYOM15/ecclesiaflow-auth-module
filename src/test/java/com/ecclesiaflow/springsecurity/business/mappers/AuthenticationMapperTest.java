package com.ecclesiaflow.springsecurity.business.mappers;

import com.ecclesiaflow.springsecurity.business.domain.Tokens;
import com.ecclesiaflow.springsecurity.business.domain.RefreshTokenCredentials;
import com.ecclesiaflow.springsecurity.web.dto.JwtAuthenticationResponse;
import com.ecclesiaflow.springsecurity.web.dto.RefreshTokenRequest;
import com.ecclesiaflow.springsecurity.web.mappers.AuthenticationMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitaires pour AuthenticationMapper
 * 
 * Teste les conversions entre objets métier d'authentification et DTOs web
 * pour garantir l'isolation correcte entre les couches de l'application.
 */
@DisplayName("AuthenticationMapper - Tests de conversion")
class AuthenticationMapperTest {

    @Test
    @DisplayName("Devrait convertir Tokens vers JwtAuthenticationResponse correctement")
    void shouldConvertTokensToJwtAuthenticationResponse() {
        // Given
        String accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.access";
        String refreshToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.refresh";
        Tokens tokens = new Tokens(accessToken, refreshToken);

        // When
        JwtAuthenticationResponse response = AuthenticationMapper.toDto(tokens);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo(accessToken);
        assertThat(response.getRefreshToken()).isEqualTo(refreshToken);
    }

    @Test
    @DisplayName("Devrait gérer les tokens avec des valeurs nulles")
    void shouldHandleTokensWithNullValues() {
        // Given
        Tokens tokensWithNullAccess = new Tokens(null, "refreshToken");
        Tokens tokensWithNullRefresh = new Tokens("accessToken", null);
        Tokens tokensWithBothNull = new Tokens(null, null);

        // When
        JwtAuthenticationResponse response1 = AuthenticationMapper.toDto(tokensWithNullAccess);
        JwtAuthenticationResponse response2 = AuthenticationMapper.toDto(tokensWithNullRefresh);
        JwtAuthenticationResponse response3 = AuthenticationMapper.toDto(tokensWithBothNull);

        // Then
        assertThat(response1).isNotNull();
        assertThat(response1.getToken()).isNull();
        assertThat(response1.getRefreshToken()).isEqualTo("refreshToken");

        assertThat(response2).isNotNull();
        assertThat(response2.getToken()).isEqualTo("accessToken");
        assertThat(response2.getRefreshToken()).isNull();

        assertThat(response3).isNotNull();
        assertThat(response3.getToken()).isNull();
        assertThat(response3.getRefreshToken()).isNull();
    }

    @Test
    @DisplayName("Devrait lancer NullPointerException pour Tokens null")
    void shouldThrowNullPointerExceptionForNullTokens() {
        // When & Then
        assertThatThrownBy(() -> AuthenticationMapper.toDto(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Devrait convertir RefreshTokenRequest vers RefreshTokenCredentials correctement")
    void shouldConvertRefreshTokenRequestToCredentials() {
        // Given
        String refreshToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.refresh";
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setToken(refreshToken);

        // When
        RefreshTokenCredentials credentials = AuthenticationMapper.fromRefreshTokenRequest(request);

        // Then
        assertThat(credentials).isNotNull();
        assertThat(credentials.getRefreshToken()).isEqualTo(refreshToken);
    }

    @Test
    @DisplayName("Devrait gérer RefreshTokenRequest avec token null")
    void shouldHandleRefreshTokenRequestWithNullToken() {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setToken(null);

        // When
        RefreshTokenCredentials credentials = AuthenticationMapper.fromRefreshTokenRequest(request);

        // Then
        assertThat(credentials).isNotNull();
        assertThat(credentials.getRefreshToken()).isNull();
    }

    @Test
    @DisplayName("Devrait lancer NullPointerException pour RefreshTokenRequest null")
    void shouldThrowNullPointerExceptionForNullRefreshTokenRequest() {
        // When & Then
        assertThatThrownBy(() -> AuthenticationMapper.fromRefreshTokenRequest(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Devrait maintenir l'intégrité des données lors des conversions")
    void shouldMaintainDataIntegrityDuringConversions() {
        // Given
        String originalAccessToken = "access.token.with.special.chars!@#$%";
        String originalRefreshToken = "refresh.token.with.unicode.éàùç";
        Tokens originalTokens = new Tokens(originalAccessToken, originalRefreshToken);

        // When
        JwtAuthenticationResponse response = AuthenticationMapper.toDto(originalTokens);

        // Then
        assertThat(response.getToken()).isEqualTo(originalAccessToken);
        assertThat(response.getRefreshToken()).isEqualTo(originalRefreshToken);
        
        // Vérifier que les données ne sont pas modifiées
        assertThat(response.getToken()).containsSequence("special.chars!@#$%");
        assertThat(response.getRefreshToken()).containsSequence("unicode.éàùç");
    }

    @Test
    @DisplayName("Devrait créer des objets indépendants lors des conversions")
    void shouldCreateIndependentObjectsDuringConversions() {
        // Given
        String accessToken = "accessToken";
        String refreshToken = "refreshToken";
        Tokens tokens = new Tokens(accessToken, refreshToken);

        // When
        JwtAuthenticationResponse response1 = AuthenticationMapper.toDto(tokens);
        JwtAuthenticationResponse response2 = AuthenticationMapper.toDto(tokens);

        // Then
        assertThat(response1).isNotSameAs(response2);
        assertThat(response1.getToken()).isEqualTo(response2.getToken());
        assertThat(response1.getRefreshToken()).isEqualTo(response2.getRefreshToken());
        
        // Modifier une réponse ne doit pas affecter l'autre
        response1.setToken("modifiedToken");
        assertThat(response2.getToken()).isEqualTo(accessToken);
    }

    @Test
    @DisplayName("Devrait gérer les tokens très longs")
    void shouldHandleVeryLongTokens() {
        // Given
        String longAccessToken = "a".repeat(1000);
        String longRefreshToken = "b".repeat(1500);
        Tokens tokens = new Tokens(longAccessToken, longRefreshToken);

        // When
        JwtAuthenticationResponse response = AuthenticationMapper.toDto(tokens);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).hasSize(1000);
        assertThat(response.getRefreshToken()).hasSize(1500);
        assertThat(response.getToken()).isEqualTo(longAccessToken);
        assertThat(response.getRefreshToken()).isEqualTo(longRefreshToken);
    }

    @Test
    @DisplayName("Devrait gérer les tokens vides")
    void shouldHandleEmptyTokens() {
        // Given
        Tokens tokensWithEmptyStrings = new Tokens("", "");

        // When
        JwtAuthenticationResponse response = AuthenticationMapper.toDto(tokensWithEmptyStrings);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEmpty();
        assertThat(response.getRefreshToken()).isEmpty();
    }
}
