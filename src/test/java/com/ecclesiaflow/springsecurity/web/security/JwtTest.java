package com.ecclesiaflow.springsecurity.web.security;

import com.ecclesiaflow.springsecurity.business.domain.token.Tokens;
import com.ecclesiaflow.springsecurity.io.entities.Member;
import com.ecclesiaflow.springsecurity.io.entities.Role;
import com.ecclesiaflow.springsecurity.web.exception.InvalidTokenException;
import com.ecclesiaflow.springsecurity.web.exception.JwtProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour Jwt
 * 
 * Teste les opérations JWT de haut niveau pour garantir la génération
 * et la validation correctes des tokens d'authentification.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Jwt - Tests d'opérations JWT")
class JwtTest {

    @Mock
    private JwtProcessor jwtProcessor;

    @InjectMocks
    private Jwt jwt;

    private Member testMember;

    @BeforeEach
    void setUp() {
        testMember = createTestMember();
    }

    @Test
    @DisplayName("Devrait générer des tokens utilisateur avec succès")
    void shouldGenerateUserTokensSuccessfully() throws JwtProcessingException {
        // Given
        String expectedAccessToken = "access.token.jwt";
        String expectedRefreshToken = "refresh.token.jwt";
        
        when(jwtProcessor.generateAccessToken(testMember)).thenReturn(expectedAccessToken);
        when(jwtProcessor.generateRefreshToken(testMember)).thenReturn(expectedRefreshToken);

        // When
        Tokens tokens = jwt.generateUserTokens(testMember);

        // Then
        assertThat(tokens).isNotNull();
        assertThat(tokens.getAccessToken()).isEqualTo(expectedAccessToken);
        assertThat(tokens.getRefreshToken()).isEqualTo(expectedRefreshToken);
        
        verify(jwtProcessor).generateAccessToken(testMember);
        verify(jwtProcessor).generateRefreshToken(testMember);
    }

    @Test
    @DisplayName("Devrait propager JwtProcessingException lors de la génération d'access token")
    void shouldPropagateJwtProcessingExceptionOnAccessTokenGeneration() throws JwtProcessingException {
        // Given
        when(jwtProcessor.generateAccessToken(testMember))
                .thenThrow(new JwtProcessingException("Erreur génération access token"));
        when(jwtProcessor.generateRefreshToken(testMember)).thenReturn("refresh.token");

        // When & Then
        assertThatThrownBy(() -> jwt.generateUserTokens(testMember))
                .isInstanceOf(JwtProcessingException.class)
                .hasMessage("Erreur génération access token");

        verify(jwtProcessor).generateAccessToken(testMember);
        verify(jwtProcessor, never()).generateRefreshToken(any());
    }

    @Test
    @DisplayName("Devrait propager JwtProcessingException lors de la génération de refresh token")
    void shouldPropagateJwtProcessingExceptionOnRefreshTokenGeneration() throws JwtProcessingException {
        // Given
        when(jwtProcessor.generateAccessToken(testMember)).thenReturn("access.token");
        when(jwtProcessor.generateRefreshToken(testMember))
                .thenThrow(new JwtProcessingException("Erreur génération refresh token"));

        // When & Then
        assertThatThrownBy(() -> jwt.generateUserTokens(testMember))
                .isInstanceOf(JwtProcessingException.class)
                .hasMessage("Erreur génération refresh token");

        verify(jwtProcessor).generateAccessToken(testMember);
        verify(jwtProcessor).generateRefreshToken(testMember);
    }

    @Test
    @DisplayName("Devrait valider et extraire l'email d'un refresh token valide")
    void shouldValidateAndExtractEmailFromValidRefreshToken() throws InvalidTokenException, JwtProcessingException {
        // Given
        String refreshToken = "valid.refresh.token";
        String expectedEmail = "test@example.com";
        
        when(jwtProcessor.isRefreshTokenValid(refreshToken)).thenReturn(true);
        when(jwtProcessor.extractUsername(refreshToken)).thenReturn(expectedEmail);

        // When
        String extractedEmail = jwt.validateAndExtractEmail(refreshToken);

        // Then
        assertThat(extractedEmail).isEqualTo(expectedEmail);
        
        verify(jwtProcessor).isRefreshTokenValid(refreshToken);
        verify(jwtProcessor).extractUsername(refreshToken);
    }

    @Test
    @DisplayName("Devrait lancer InvalidTokenException pour refresh token invalide")
    void shouldThrowInvalidTokenExceptionForInvalidRefreshToken() throws InvalidTokenException, JwtProcessingException {
        // Given
        String invalidRefreshToken = "invalid.refresh.token";
        
        when(jwtProcessor.isRefreshTokenValid(invalidRefreshToken)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> jwt.validateAndExtractEmail(invalidRefreshToken))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Le token de rafraîchissement est invalide ou expiré.");

        verify(jwtProcessor).isRefreshTokenValid(invalidRefreshToken);
        verify(jwtProcessor, never()).extractUsername(any());
    }

    @Test
    @DisplayName("Devrait propager JwtProcessingException lors de la validation")
    void shouldPropagateJwtProcessingExceptionOnValidation() throws InvalidTokenException, JwtProcessingException {
        // Given
        String refreshToken = "problematic.token";
        
        when(jwtProcessor.isRefreshTokenValid(refreshToken))
                .thenThrow(new JwtProcessingException("Erreur traitement token"));

        // When & Then
        assertThatThrownBy(() -> jwt.validateAndExtractEmail(refreshToken))
                .isInstanceOf(JwtProcessingException.class)
                .hasMessage("Erreur traitement token");

        verify(jwtProcessor).isRefreshTokenValid(refreshToken);
        verify(jwtProcessor, never()).extractUsername(any());
    }

    @Test
    @DisplayName("Devrait propager JwtProcessingException lors de l'extraction d'email")
    void shouldPropagateJwtProcessingExceptionOnEmailExtraction() throws InvalidTokenException, JwtProcessingException {
        // Given
        String refreshToken = "valid.but.problematic.token";
        
        when(jwtProcessor.isRefreshTokenValid(refreshToken)).thenReturn(true);
        when(jwtProcessor.extractUsername(refreshToken))
                .thenThrow(new JwtProcessingException("Erreur extraction email"));

        // When & Then
        assertThatThrownBy(() -> jwt.validateAndExtractEmail(refreshToken))
                .isInstanceOf(JwtProcessingException.class)
                .hasMessage("Erreur extraction email");

        verify(jwtProcessor).isRefreshTokenValid(refreshToken);
        verify(jwtProcessor).extractUsername(refreshToken);
    }

    @Test
    @DisplayName("Devrait rafraîchir le token pour un membre")
    void shouldRefreshTokenForMember() throws JwtProcessingException {
        // Given
        String oldRefreshToken = "old.refresh.token";
        String newAccessToken = "new.access.token";
        
        when(jwtProcessor.generateAccessToken(testMember)).thenReturn(newAccessToken);

        // When
        Tokens refreshedTokens = jwt.refreshTokenForMember(oldRefreshToken, testMember);

        // Then
        assertThat(refreshedTokens).isNotNull();
        assertThat(refreshedTokens.getAccessToken()).isEqualTo(newAccessToken);
        assertThat(refreshedTokens.getRefreshToken()).isEqualTo(oldRefreshToken);
        
        verify(jwtProcessor).generateAccessToken(testMember);
    }

    @Test
    @DisplayName("Devrait propager JwtProcessingException lors du rafraîchissement")
    void shouldPropagateJwtProcessingExceptionOnTokenRefresh() throws JwtProcessingException {
        // Given
        String refreshToken = "refresh.token";
        
        when(jwtProcessor.generateAccessToken(testMember))
                .thenThrow(new JwtProcessingException("Erreur génération nouveau token"));

        // When & Then
        assertThatThrownBy(() -> jwt.refreshTokenForMember(refreshToken, testMember))
                .isInstanceOf(JwtProcessingException.class)
                .hasMessage("Erreur génération nouveau token");

        verify(jwtProcessor).generateAccessToken(testMember);
    }

    @Test
    @DisplayName("Devrait gérer différents types de membres")
    void shouldHandleDifferentMemberTypes() throws JwtProcessingException {
        // Given
        Member adminMember = createTestMember();
        adminMember.setRole(Role.ADMIN);
        adminMember.setEmail("admin@example.com");
        
        when(jwtProcessor.generateAccessToken(any())).thenReturn("access.token");
        when(jwtProcessor.generateRefreshToken(any())).thenReturn("refresh.token");

        // When
        Tokens userTokens = jwt.generateUserTokens(testMember);
        Tokens adminTokens = jwt.generateUserTokens(adminMember);

        // Then
        assertThat(userTokens).isNotNull();
        assertThat(adminTokens).isNotNull();
        
        verify(jwtProcessor, times(2)).generateAccessToken(any());
        verify(jwtProcessor, times(2)).generateRefreshToken(any());
    }

    @Test
    @DisplayName("Devrait maintenir l'indépendance des opérations")
    void shouldMaintainOperationIndependence() throws JwtProcessingException, InvalidTokenException {
        // Given
        String refreshToken = "refresh.token";
        String email = "test@example.com";
        String newAccessToken = "new.access.token";
        
        when(jwtProcessor.isRefreshTokenValid(refreshToken)).thenReturn(true);
        when(jwtProcessor.extractUsername(refreshToken)).thenReturn(email);
        when(jwtProcessor.generateAccessToken(testMember)).thenReturn(newAccessToken);

        // When
        String extractedEmail = jwt.validateAndExtractEmail(refreshToken);
        Tokens refreshedTokens = jwt.refreshTokenForMember(refreshToken, testMember);

        // Then
        assertThat(extractedEmail).isEqualTo(email);
        assertThat(refreshedTokens.getAccessToken()).isEqualTo(newAccessToken);
        assertThat(refreshedTokens.getRefreshToken()).isEqualTo(refreshToken);
        
        // Vérifier que les opérations sont indépendantes
        verify(jwtProcessor).isRefreshTokenValid(refreshToken);
        verify(jwtProcessor).extractUsername(refreshToken);
        verify(jwtProcessor).generateAccessToken(testMember);
    }

    @Test
    @DisplayName("Devrait gérer les tokens null et vides")
    void shouldHandleNullAndEmptyTokens() throws InvalidTokenException, JwtProcessingException {
        // Given
        when(jwtProcessor.isRefreshTokenValid(null)).thenReturn(false);
        when(jwtProcessor.isRefreshTokenValid("")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> jwt.validateAndExtractEmail(null))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Le token de rafraîchissement est invalide ou expiré.");

        assertThatThrownBy(() -> jwt.validateAndExtractEmail(""))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Le token de rafraîchissement est invalide ou expiré.");
    }

    private Member createTestMember() {
        Member member = new Member();
        member.setId(UUID.randomUUID());
        member.setEmail("test@example.com");
        member.setFirstName("John");
        member.setLastName("Doe");
        member.setPassword("encodedPassword");
        member.setRole(Role.MEMBER);
        return member;
    }
}
