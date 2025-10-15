package com.ecclesiaflow.springsecurity.web.mappers;

import com.ecclesiaflow.springsecurity.business.domain.token.UserTokens;
import com.ecclesiaflow.springsecurity.web.model.JwtAuthenticationResponse;
import com.ecclesiaflow.springsecurity.web.model.PasswordManagementResponse;
import com.ecclesiaflow.springsecurity.web.model.TemporaryTokenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitaires pour OpenApiModelMapper.
 * <p>
 * Teste la création des modèles OpenAPI à partir des objets métier.
 * </p>
 */
@DisplayName("OpenApiModelMapper - Tests de conversion")
class OpenApiModelMapperTest {

    private OpenApiModelMapper openApiModelMapper;
    private UserTokens userTokens;

    @BeforeEach
    void setUp() {
        openApiModelMapper = new OpenApiModelMapper();
        ReflectionTestUtils.setField(openApiModelMapper, "temporaryTokenExpiration", 900);

        userTokens = new UserTokens("access-token-123", "refresh-token-456");
    }

    // ====================================================================
    // Tests createJwtAuthenticationResponse
    // ====================================================================

    @Test
    @DisplayName("createJwtAuthenticationResponse - Devrait créer réponse avec tokens valides")
    void createJwtAuthenticationResponse_ShouldCreateResponseWithValidTokens() {
        // When
        JwtAuthenticationResponse response = openApiModelMapper.createJwtAuthenticationResponse(userTokens);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("access-token-123");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token-456");
    }

    @Test
    @DisplayName("createJwtAuthenticationResponse - Devrait gérer UserTokens null")
    void createJwtAuthenticationResponse_ShouldHandleNullUserTokens() {
        // When
        JwtAuthenticationResponse response = openApiModelMapper.createJwtAuthenticationResponse(null);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isNull();
        assertThat(response.getRefreshToken()).isNull();
    }

    @Test
    @DisplayName("createJwtAuthenticationResponse - Devrait gérer tokens vides")
    void createJwtAuthenticationResponse_ShouldHandleEmptyTokens() {
        // Given
        UserTokens emptyTokens = new UserTokens("", "");

        // When
        JwtAuthenticationResponse response = openApiModelMapper.createJwtAuthenticationResponse(emptyTokens);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEmpty();
        assertThat(response.getRefreshToken()).isEmpty();
    }

    @Test
    @DisplayName("createJwtAuthenticationResponse - Devrait gérer tokens avec null")
    void createJwtAuthenticationResponse_ShouldHandleTokensWithNullValues() {
        // Given
        UserTokens tokensWithNull = new UserTokens(null, "refresh-token");

        // When
        JwtAuthenticationResponse response = openApiModelMapper.createJwtAuthenticationResponse(tokensWithNull);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isNull();
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
    }

    @Test
    @DisplayName("createJwtAuthenticationResponse - Devrait maintenir l'intégrité des données")
    void createJwtAuthenticationResponse_ShouldMaintainDataIntegrity() {
        // Given
        String originalAccessToken = "access.token.with.special.chars!@#$%";
        String originalRefreshToken = "refresh.token.with.unicode.éàùç";
        UserTokens originalUserTokens = new UserTokens(originalAccessToken, originalRefreshToken);

        // When
        JwtAuthenticationResponse response = openApiModelMapper.createJwtAuthenticationResponse(originalUserTokens);

        // Then
        assertThat(response.getToken()).isEqualTo(originalAccessToken);
        assertThat(response.getRefreshToken()).isEqualTo(originalRefreshToken);
        assertThat(response.getToken()).containsSequence("special.chars!@#$%");
        assertThat(response.getRefreshToken()).containsSequence("unicode.éàùç");
    }

    @Test
    @DisplayName("createJwtAuthenticationResponse - Devrait gérer tokens très longs")
    void createJwtAuthenticationResponse_ShouldHandleVeryLongTokens() {
        // Given
        String longAccessToken = "a".repeat(1000);
        String longRefreshToken = "b".repeat(1500);
        UserTokens longTokens = new UserTokens(longAccessToken, longRefreshToken);

        // When
        JwtAuthenticationResponse response = openApiModelMapper.createJwtAuthenticationResponse(longTokens);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).hasSize(1000);
        assertThat(response.getRefreshToken()).hasSize(1500);
    }

    // ====================================================================
    // Tests createPasswordManagementResponse
    // ====================================================================

    @Test
    @DisplayName("createPasswordManagementResponse - Devrait créer réponse complète")
    void createPasswordManagementResponse_ShouldCreateCompleteResponse() {
        // Given
        String message = "Mot de passe défini avec succès";
        Long expiresIn = 60L;

        // When
        PasswordManagementResponse response = openApiModelMapper.createPasswordManagementResponse(
                message, userTokens, expiresIn
        );

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEqualTo(message);
        assertThat(response.getAccessToken()).isEqualTo("access-token-123");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token-456");
        assertThat(response.getExpiresIn()).isEqualTo(60L);
    }

    @Test
    @DisplayName("createPasswordManagementResponse - Devrait gérer UserTokens null")
    void createPasswordManagementResponse_ShouldHandleNullUserTokens() {
        // Given
        String message = "Message de test";
        Long expiresIn = 60L;

        // When
        PasswordManagementResponse response = openApiModelMapper.createPasswordManagementResponse(
                message, null, expiresIn
        );

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEqualTo(message);
        assertThat(response.getAccessToken()).isNull();
        assertThat(response.getRefreshToken()).isNull();
        assertThat(response.getExpiresIn()).isEqualTo(60L);
    }

    @Test
    @DisplayName("createPasswordManagementResponse - Devrait gérer message null")
    void createPasswordManagementResponse_ShouldHandleNullMessage() {
        // When
        PasswordManagementResponse response = openApiModelMapper.createPasswordManagementResponse(
                null, userTokens, 60L
        );

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token-123");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token-456");
    }

    @Test
    @DisplayName("createPasswordManagementResponse - Devrait gérer expiresIn null")
    void createPasswordManagementResponse_ShouldHandleNullExpiresIn() {
        // When
        PasswordManagementResponse response = openApiModelMapper.createPasswordManagementResponse(
                "Message", userTokens, null
        );

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getExpiresIn()).isNull();
    }

    @Test
    @DisplayName("createPasswordManagementResponse - Devrait gérer différentes valeurs expiresIn")
    void createPasswordManagementResponse_ShouldHandleDifferentExpiresInValues() {
        // When & Then
        PasswordManagementResponse response1 = openApiModelMapper.createPasswordManagementResponse(
                "Message", userTokens, 30L
        );
        assertThat(response1.getExpiresIn()).isEqualTo(30L);

        PasswordManagementResponse response2 = openApiModelMapper.createPasswordManagementResponse(
                "Message", userTokens, 3600L
        );
        assertThat(response2.getExpiresIn()).isEqualTo(3600L);

        PasswordManagementResponse response3 = openApiModelMapper.createPasswordManagementResponse(
                "Message", userTokens, 0L
        );
        assertThat(response3.getExpiresIn()).isEqualTo(0L);
    }

    @Test
    @DisplayName("createPasswordManagementResponse - Devrait gérer message vide")
    void createPasswordManagementResponse_ShouldHandleEmptyMessage() {
        // When
        PasswordManagementResponse response = openApiModelMapper.createPasswordManagementResponse(
                "", userTokens, 60L
        );

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEmpty();
    }

    // ====================================================================
    // Tests createTemporaryTokenResponse
    // ====================================================================

    @Test
    @DisplayName("createTemporaryTokenResponse - Devrait créer réponse avec token valide")
    void createTemporaryTokenResponse_ShouldCreateResponseWithValidToken() {
        // Given
        String token = "temp-token-123";

        // When
        TemporaryTokenResponse response = openApiModelMapper.createTemporaryTokenResponse(token);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTemporaryToken()).isEqualTo("temp-token-123");
        assertThat(response.getExpiresIn()).isEqualTo(900);
        assertThat(response.getMessage()).isEqualTo("Token temporaire généré avec succès");
    }

    @Test
    @DisplayName("createTemporaryTokenResponse - Devrait utiliser configuration expiration")
    void createTemporaryTokenResponse_ShouldUseExpirationConfiguration() {
        // Given
        ReflectionTestUtils.setField(openApiModelMapper, "temporaryTokenExpiration", 1800);
        String token = "temp-token";

        // When
        TemporaryTokenResponse response = openApiModelMapper.createTemporaryTokenResponse(token);

        // Then
        assertThat(response.getExpiresIn()).isEqualTo(1800);
    }

    @Test
    @DisplayName("createTemporaryTokenResponse - Devrait gérer token null")
    void createTemporaryTokenResponse_ShouldHandleNullToken() {
        // When
        TemporaryTokenResponse response = openApiModelMapper.createTemporaryTokenResponse(null);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTemporaryToken()).isNull();
        assertThat(response.getExpiresIn()).isEqualTo(900);
        assertThat(response.getMessage()).isEqualTo("Token temporaire généré avec succès");
    }

    @Test
    @DisplayName("createTemporaryTokenResponse - Devrait gérer token vide")
    void createTemporaryTokenResponse_ShouldHandleEmptyToken() {
        // When
        TemporaryTokenResponse response = openApiModelMapper.createTemporaryTokenResponse("");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTemporaryToken()).isEmpty();
        assertThat(response.getExpiresIn()).isEqualTo(900);
    }

    @Test
    @DisplayName("createTemporaryTokenResponse - Devrait gérer token très long")
    void createTemporaryTokenResponse_ShouldHandleVeryLongToken() {
        // Given
        String longToken = "token".repeat(200);

        // When
        TemporaryTokenResponse response = openApiModelMapper.createTemporaryTokenResponse(longToken);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTemporaryToken()).hasSize(1000);
    }

    @Test
    @DisplayName("createTemporaryTokenResponse - Devrait maintenir intégrité token avec caractères spéciaux")
    void createTemporaryTokenResponse_ShouldMaintainTokenIntegrityWithSpecialChars() {
        // Given
        String specialToken = "temp.token-with_special!@#chars";

        // When
        TemporaryTokenResponse response = openApiModelMapper.createTemporaryTokenResponse(specialToken);

        // Then
        assertThat(response.getTemporaryToken()).isEqualTo(specialToken);
        assertThat(response.getTemporaryToken()).containsSequence("special!@#chars");
    }

    // ====================================================================
    // Tests de création multiple pour vérifier l'indépendance
    // ====================================================================

    @Test
    @DisplayName("Devrait créer des objets indépendants lors de créations multiples")
    void shouldCreateIndependentObjectsOnMultipleCreations() {
        // When
        JwtAuthenticationResponse response1 = openApiModelMapper.createJwtAuthenticationResponse(userTokens);
        JwtAuthenticationResponse response2 = openApiModelMapper.createJwtAuthenticationResponse(userTokens);

        // Then
        assertThat(response1).isNotSameAs(response2);
        assertThat(response1.getToken()).isEqualTo(response2.getToken());
        assertThat(response1.getRefreshToken()).isEqualTo(response2.getRefreshToken());

        // Modifier une réponse ne doit pas affecter l'autre
        response1.setToken("modified-token");
        assertThat(response2.getToken()).isEqualTo("access-token-123");
    }
}
