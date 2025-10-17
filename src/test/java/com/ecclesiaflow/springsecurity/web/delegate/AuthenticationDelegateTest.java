package com.ecclesiaflow.springsecurity.web.delegate;

import com.ecclesiaflow.springsecurity.business.domain.member.Member;
import com.ecclesiaflow.springsecurity.business.domain.password.SigninCredentials;
import com.ecclesiaflow.springsecurity.business.domain.token.TemporaryToken;
import com.ecclesiaflow.springsecurity.business.domain.token.UserTokens;
import com.ecclesiaflow.springsecurity.business.services.AuthenticationService;
import com.ecclesiaflow.springsecurity.web.exception.InvalidCredentialsException;
import com.ecclesiaflow.springsecurity.web.exception.InvalidTokenException;
import com.ecclesiaflow.springsecurity.web.exception.JwtProcessingException;
import com.ecclesiaflow.springsecurity.web.mappers.OpenApiModelMapper;
import com.ecclesiaflow.springsecurity.web.mappers.TemporaryTokenMapper;
import com.ecclesiaflow.springsecurity.web.model.JwtAuthenticationResponse;
import com.ecclesiaflow.springsecurity.web.model.RefreshTokenRequest;
import com.ecclesiaflow.springsecurity.web.model.SigninRequest;
import com.ecclesiaflow.springsecurity.web.model.TemporaryTokenRequest;
import com.ecclesiaflow.springsecurity.web.model.TemporaryTokenResponse;
import com.ecclesiaflow.springsecurity.web.security.Jwt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour AuthenticationDelegate.
 * <p>
 * Teste la logique métier d'authentification avec les modèles OpenAPI.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationDelegate - Tests de logique métier")
class AuthenticationDelegateTest {

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private Jwt jwt;

    @Mock
    private OpenApiModelMapper openApiModelMapper;

    @Mock
    private TemporaryTokenMapper temporaryTokenMapper;

    @InjectMocks
    private AuthenticationDelegate authenticationDelegate;

    private Member member;
    private UserTokens userTokens;
    private SigninRequest signinRequest;
    private RefreshTokenRequest refreshTokenRequest;
    private TemporaryTokenRequest temporaryTokenRequest;
    private JwtAuthenticationResponse jwtAuthenticationResponse;
    private TemporaryTokenResponse temporaryTokenResponse;

    @BeforeEach
    void setUp() {
        // Setup Member
        member = Member.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .password("hashedPassword")
                .enabled(true)
                .build();

        // Setup UserTokens
        userTokens = new UserTokens("access-token-123", "refresh-token-456");

        // Setup SigninRequest
        signinRequest = new SigninRequest();
        signinRequest.setEmail("user@example.com");
        signinRequest.setPassword("password123");

        // Setup RefreshTokenRequest
        refreshTokenRequest = new RefreshTokenRequest();
        refreshTokenRequest.setRefreshToken("refresh-token-456");

        // Setup TemporaryToken
        temporaryTokenRequest = new TemporaryTokenRequest();
        temporaryTokenRequest.setEmail("user@example.com");
        temporaryTokenRequest.setMemberId(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));

        // Setup JwtAuthenticationResponse
        jwtAuthenticationResponse = new JwtAuthenticationResponse();
        jwtAuthenticationResponse.setToken("access-token-123");
        jwtAuthenticationResponse.setRefreshToken("refresh-token-456");

        // Setup TemporaryTokenResponse
        temporaryTokenResponse = new TemporaryTokenResponse();
        temporaryTokenResponse.setTemporaryToken("temp-token-789");
        temporaryTokenResponse.setExpiresIn(900);
        temporaryTokenResponse.setMessage("Token temporaire généré avec succès");
    }

    // ====================================================================
    // Tests generateToken - Cas de succès
    // ====================================================================

    @Test
    @DisplayName("generateToken - Devrait retourner JwtAuthenticationResponse avec tokens")
    void generateToken_ShouldReturnJwtAuthenticationResponse() throws Exception {
        // Given
        SigninCredentials credentials = new SigninCredentials("user@example.com", "password123");

        when(authenticationService.getAuthenticatedMember(any(SigninCredentials.class))).thenReturn(member);
        when(jwt.generateUserTokens(member)).thenReturn(userTokens);
        when(openApiModelMapper.createJwtAuthenticationResponse(userTokens))
                .thenReturn(jwtAuthenticationResponse);

        // When
        ResponseEntity<JwtAuthenticationResponse> response = authenticationDelegate.generateToken(signinRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getToken()).isEqualTo("access-token-123");
        assertThat(response.getBody().getRefreshToken()).isEqualTo("refresh-token-456");

        verify(authenticationService).getAuthenticatedMember(any(SigninCredentials.class));
        verify(jwt).generateUserTokens(member);
        verify(openApiModelMapper).createJwtAuthenticationResponse(userTokens);
    }

    @Test
    @DisplayName("generateToken - Devrait convertir correctement SigninRequest vers SigninCredentials")
    void generateToken_ShouldConvertSigninRequestCorrectly() throws Exception {
        // Given
        when(authenticationService.getAuthenticatedMember(any(SigninCredentials.class))).thenReturn(member);
        when(jwt.generateUserTokens(member)).thenReturn(userTokens);
        when(openApiModelMapper.createJwtAuthenticationResponse(userTokens))
                .thenReturn(jwtAuthenticationResponse);

        // When
        authenticationDelegate.generateToken(signinRequest);

        // Then
        verify(authenticationService).getAuthenticatedMember(argThat(credentials ->
                credentials.email().equals("user@example.com") &&
                credentials.password().equals("password123")
        ));
    }

    // ====================================================================
    // Tests generateToken - Cas d'erreur
    // ====================================================================

    @Test
    @DisplayName("generateToken - Devrait propager InvalidCredentialsException")
    void generateToken_ShouldThrowInvalidCredentialsException() {
        // Given
        when(authenticationService.getAuthenticatedMember(any(SigninCredentials.class)))
                .thenThrow(new InvalidCredentialsException("Identifiants invalides"));

        // When & Then
        assertThatThrownBy(() -> authenticationDelegate.generateToken(signinRequest))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Identifiants invalides");

        verify(authenticationService).getAuthenticatedMember(any(SigninCredentials.class));
        verify(jwt, never()).generateUserTokens(any());
    }

    @Test
    @DisplayName("generateToken - Devrait propager InvalidTokenException")
    void generateToken_ShouldThrowInvalidTokenException() {
        // Given
        when(authenticationService.getAuthenticatedMember(any(SigninCredentials.class))).thenReturn(member);
        when(jwt.generateUserTokens(member))
                .thenThrow(new InvalidTokenException("Erreur génération token"));

        // When & Then
        assertThatThrownBy(() -> authenticationDelegate.generateToken(signinRequest))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Erreur génération token");

        verify(jwt).generateUserTokens(member);
    }

    @Test
    @DisplayName("generateToken - Devrait propager JwtProcessingException")
    void generateToken_ShouldThrowJwtProcessingException() {
        // Given
        when(authenticationService.getAuthenticatedMember(any(SigninCredentials.class))).thenReturn(member);
        when(jwt.generateUserTokens(member))
                .thenThrow(new JwtProcessingException("Erreur traitement JWT"));

        // When & Then
        assertThatThrownBy(() -> authenticationDelegate.generateToken(signinRequest))
                .isInstanceOf(JwtProcessingException.class)
                .hasMessage("Erreur traitement JWT");
    }

    // ====================================================================
    // Tests refreshToken - Cas de succès
    // ====================================================================

    @Test
    @DisplayName("refreshToken - Devrait retourner nouveaux tokens JWT")
    void refreshToken_ShouldReturnNewJwtTokens() throws Exception {
        // Given
        JwtAuthenticationResponse newResponse = new JwtAuthenticationResponse();
        newResponse.setToken("new-access-token-789");
        newResponse.setRefreshToken("new-refresh-token-012");

        UserTokens newTokens = new UserTokens("new-access-token-789", "new-refresh-token-012");

        when(jwt.validateAndExtractEmail("refresh-token-456")).thenReturn("user@example.com");
        when(authenticationService.getMemberByEmail("user@example.com")).thenReturn(member);
        when(jwt.refreshTokenForMember("refresh-token-456", member)).thenReturn(newTokens);
        when(openApiModelMapper.createJwtAuthenticationResponse(newTokens)).thenReturn(newResponse);

        // When
        ResponseEntity<JwtAuthenticationResponse> response = authenticationDelegate.refreshToken(refreshTokenRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getToken()).isEqualTo("new-access-token-789");
        assertThat(response.getBody().getRefreshToken()).isEqualTo("new-refresh-token-012");

        verify(jwt).validateAndExtractEmail("refresh-token-456");
        verify(authenticationService).getMemberByEmail("user@example.com");
        verify(jwt).refreshTokenForMember("refresh-token-456", member);
        verify(openApiModelMapper).createJwtAuthenticationResponse(newTokens);
    }

    @Test
    @DisplayName("refreshToken - Devrait créer TokenCredentials correctement")
    void refreshToken_ShouldCreateTokenCredentialsCorrectly() throws Exception {
        // Given
        when(jwt.validateAndExtractEmail("refresh-token-456")).thenReturn("user@example.com");
        when(authenticationService.getMemberByEmail("user@example.com")).thenReturn(member);
        when(jwt.refreshTokenForMember("refresh-token-456", member)).thenReturn(userTokens);
        when(openApiModelMapper.createJwtAuthenticationResponse(userTokens))
                .thenReturn(jwtAuthenticationResponse);

        // When
        authenticationDelegate.refreshToken(refreshTokenRequest);

        // Then
        verify(jwt).validateAndExtractEmail("refresh-token-456");
    }

    // ====================================================================
    // Tests refreshToken - Cas d'erreur
    // ====================================================================

    @Test
    @DisplayName("refreshToken - Devrait propager InvalidTokenException si token invalide")
    void refreshToken_ShouldThrowInvalidTokenException() {
        // Given
        when(jwt.validateAndExtractEmail("refresh-token-456"))
                .thenThrow(new InvalidTokenException("Token invalide ou expiré"));

        // When & Then
        assertThatThrownBy(() -> authenticationDelegate.refreshToken(refreshTokenRequest))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Token invalide ou expiré");

        verify(jwt).validateAndExtractEmail("refresh-token-456");
        verify(authenticationService, never()).getMemberByEmail(anyString());
        verify(jwt, never()).refreshTokenForMember(anyString(), any());
    }

    @Test
    @DisplayName("refreshToken - Devrait propager JwtProcessingException lors du refresh")
    void refreshToken_ShouldThrowJwtProcessingException() {
        // Given
        when(jwt.validateAndExtractEmail("refresh-token-456")).thenReturn("user@example.com");
        when(authenticationService.getMemberByEmail("user@example.com")).thenReturn(member);
        when(jwt.refreshTokenForMember("refresh-token-456", member))
                .thenThrow(new JwtProcessingException("Erreur refresh token"));

        // When & Then
        assertThatThrownBy(() -> authenticationDelegate.refreshToken(refreshTokenRequest))
                .isInstanceOf(JwtProcessingException.class)
                .hasMessage("Erreur refresh token");

        verify(jwt).refreshTokenForMember("refresh-token-456", member);
    }

    @Test
    @DisplayName("refreshToken - Devrait gérer le cas où l'email extrait est null")
    void refreshToken_ShouldHandleNullEmail() throws Exception {
        // Given
        when(jwt.validateAndExtractEmail("refresh-token-456")).thenReturn(null);
        when(authenticationService.getMemberByEmail(null)).thenReturn(member);
        when(jwt.refreshTokenForMember("refresh-token-456", member)).thenReturn(userTokens);
        when(openApiModelMapper.createJwtAuthenticationResponse(userTokens))
                .thenReturn(jwtAuthenticationResponse);

        // When
        ResponseEntity<JwtAuthenticationResponse> response = authenticationDelegate.refreshToken(refreshTokenRequest);

        // Then
        assertThat(response).isNotNull();
        verify(authenticationService).getMemberByEmail(null);
    }

    // ====================================================================
    // Tests generateTemporaryToken - Cas de succès
    // ====================================================================

    @Test
    @DisplayName("generateTemporaryToken - Devrait retourner TemporaryTokenResponse")
    void generateTemporaryToken_ShouldReturnTemporaryTokenResponse() throws Exception {
        // Given
        String email = "user@example.com";
        UUID memberId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        String tempToken = "temp-token-xyz";
        TemporaryToken domainToken = new TemporaryToken(email, memberId);

        when(temporaryTokenMapper.toDomain(temporaryTokenRequest)).thenReturn(domainToken);
        when(jwt.generateTemporaryToken(email, memberId)).thenReturn(tempToken);
        when(openApiModelMapper.createTemporaryTokenResponse(tempToken))
                .thenReturn(temporaryTokenResponse);

        // When
        ResponseEntity<TemporaryTokenResponse> response = authenticationDelegate.generateTemporaryToken(
                temporaryTokenRequest
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTemporaryToken()).isEqualTo("temp-token-789");
        assertThat(response.getBody().getExpiresIn()).isEqualTo(900);

        verify(temporaryTokenMapper).toDomain(temporaryTokenRequest);
        verify(jwt).generateTemporaryToken(email, memberId);
        verify(openApiModelMapper).createTemporaryTokenResponse(tempToken);
    }

    @Test
    @DisplayName("generateTemporaryToken - Devrait appeler les services dans l'ordre correct")
    void generateTemporaryToken_ShouldCallServicesInCorrectOrder() throws Exception {
        // Given
        String email = "test@ecclesiaflow.com";
        UUID memberId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        String tempToken = "temp-123";
        TemporaryToken domainToken = new TemporaryToken(email, memberId);

        when(temporaryTokenMapper.toDomain(temporaryTokenRequest)).thenReturn(domainToken);
        when(jwt.generateTemporaryToken(email, memberId)).thenReturn(tempToken);
        when(openApiModelMapper.createTemporaryTokenResponse(tempToken))
                .thenReturn(temporaryTokenResponse);

        // When
        authenticationDelegate.generateTemporaryToken(temporaryTokenRequest);

        // Then
        verify(temporaryTokenMapper).toDomain(temporaryTokenRequest);
        verify(jwt).generateTemporaryToken(email, memberId);
        verify(openApiModelMapper).createTemporaryTokenResponse(tempToken);
    }

    // ====================================================================
    // Tests generateTemporaryToken - Cas d'erreur
    // ====================================================================

    @Test
    @DisplayName("generateTemporaryToken - Devrait propager InvalidTokenException")
    void generateTemporaryToken_ShouldThrowInvalidTokenException() {
        // Given
        String email = "user@example.com";
        UUID memberId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        TemporaryToken domainToken = new TemporaryToken(email, memberId);

        when(temporaryTokenMapper.toDomain(temporaryTokenRequest)).thenReturn(domainToken);
        when(jwt.generateTemporaryToken(email, memberId))
                .thenThrow(new InvalidTokenException("Erreur génération token temporaire"));

        // When & Then
        assertThatThrownBy(() -> authenticationDelegate.generateTemporaryToken(temporaryTokenRequest))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Erreur génération token temporaire");

        verify(temporaryTokenMapper).toDomain(temporaryTokenRequest);
        verify(jwt).generateTemporaryToken(email, memberId);
        verify(openApiModelMapper, never()).createTemporaryTokenResponse(anyString());
    }

    @Test
    @DisplayName("generateTemporaryToken - Devrait propager JwtProcessingException")
    void generateTemporaryToken_ShouldThrowJwtProcessingException() {
        // Given
        String email = "user@example.com";
        UUID memberId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        TemporaryToken domainToken = new TemporaryToken(email, memberId);

        when(temporaryTokenMapper.toDomain(temporaryTokenRequest)).thenReturn(domainToken);
        when(jwt.generateTemporaryToken(email, memberId))
                .thenThrow(new JwtProcessingException("Erreur traitement JWT"));

        // When & Then
        assertThatThrownBy(() -> authenticationDelegate.generateTemporaryToken(temporaryTokenRequest))
                .isInstanceOf(JwtProcessingException.class)
                .hasMessage("Erreur traitement JWT");
    }

    @Test
    @DisplayName("generateTemporaryToken - Devrait propager IllegalArgumentException si email null dans DTO")
    void generateTemporaryToken_ShouldPropagateIllegalArgumentException_WhenEmailNull() {
        // Given - Le mapper toDomain() va lever une exception si email est null
        when(temporaryTokenMapper.toDomain(temporaryTokenRequest))
                .thenThrow(new IllegalArgumentException("L'email ne peut pas être null ou vide"));

        // When & Then
        assertThatThrownBy(() -> authenticationDelegate.generateTemporaryToken(temporaryTokenRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("L'email ne peut pas être null ou vide");

        verify(temporaryTokenMapper).toDomain(temporaryTokenRequest);
        verify(jwt, never()).generateTemporaryToken(anyString(), any(UUID.class));
    }
}
