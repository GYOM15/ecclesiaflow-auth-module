package com.ecclesiaflow.springsecurity.business.mappers;

import com.ecclesiaflow.springsecurity.business.domain.token.TokenCredentials;
import com.ecclesiaflow.springsecurity.business.domain.token.UserTokens;
import com.ecclesiaflow.springsecurity.web.dto.JwtAuthenticationResponse;
import com.ecclesiaflow.springsecurity.web.payloads.RefreshTokenRequest;
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
    @DisplayName("Devrait convertir UserTokens vers JwtAuthenticationResponse correctement")
    void shouldConvertTokensToJwtAuthenticationResponse() {
        // Given
        String accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.access";
        String refreshToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.refresh";
        UserTokens userTokens = new UserTokens(accessToken, refreshToken);

        // When
        JwtAuthenticationResponse response = AuthenticationMapper.toDto(userTokens);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo(accessToken);
        assertThat(response.getRefreshToken()).isEqualTo(refreshToken);
    }

    @Test
    @DisplayName("Devrait gérer les tokens avec des valeurs nulles")
    void shouldHandleTokensWithNullValues() {
        // Given
        UserTokens userTokensWithNullAccess = new UserTokens(null, "refreshToken");
        UserTokens userTokensWithNullRefresh = new UserTokens("accessToken", null);
        UserTokens userTokensWithBothNull = new UserTokens(null, null);

        // When
        JwtAuthenticationResponse response1 = AuthenticationMapper.toDto(userTokensWithNullAccess);
        JwtAuthenticationResponse response2 = AuthenticationMapper.toDto(userTokensWithNullRefresh);
        JwtAuthenticationResponse response3 = AuthenticationMapper.toDto(userTokensWithBothNull);

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
    @DisplayName("Devrait lancer NullPointerException pour UserTokens null")
    void shouldThrowNullPointerExceptionForNullTokens() {
        // When & Then
        assertThatThrownBy(() -> AuthenticationMapper.toDto(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Devrait convertir RefreshTokenRequest vers TokenCredentials correctement")
    void shouldConvertRefreshTokenRequestToCredentials() {
        // Given
        String refreshToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.refresh";
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(refreshToken);

        // When
        TokenCredentials credentials = AuthenticationMapper.fromRefreshTokenRequest(request);

        // Then
        assertThat(credentials).isNotNull();
        assertThat(credentials.token()).isEqualTo(refreshToken);
    }

    @Test
    @DisplayName("Devrait gérer RefreshTokenRequest avec token null")
    void shouldHandleRefreshTokenRequestWithNullToken() {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(null);

        // When
        TokenCredentials credentials = AuthenticationMapper.fromRefreshTokenRequest(request);

        // Then
        assertThat(credentials).isNotNull();
        assertThat(credentials.token()).isNull();
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
        UserTokens originalUserTokens = new UserTokens(originalAccessToken, originalRefreshToken);

        // When
        JwtAuthenticationResponse response = AuthenticationMapper.toDto(originalUserTokens);

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
        UserTokens userTokens = new UserTokens(accessToken, refreshToken);

        // When
        JwtAuthenticationResponse response1 = AuthenticationMapper.toDto(userTokens);
        JwtAuthenticationResponse response2 = AuthenticationMapper.toDto(userTokens);

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
        UserTokens userTokens = new UserTokens(longAccessToken, longRefreshToken);

        // When
        JwtAuthenticationResponse response = AuthenticationMapper.toDto(userTokens);

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
        UserTokens userTokensWithEmptyStrings = new UserTokens("", "");

        // When
        JwtAuthenticationResponse response = AuthenticationMapper.toDto(userTokensWithEmptyStrings);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEmpty();
        assertThat(response.getRefreshToken()).isEmpty();
    }
}
