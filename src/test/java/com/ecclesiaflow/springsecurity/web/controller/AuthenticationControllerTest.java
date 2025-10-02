package com.ecclesiaflow.springsecurity.web.controller;

import com.ecclesiaflow.springsecurity.business.domain.token.UserTokens;
import com.ecclesiaflow.springsecurity.business.domain.password.SigninCredentials;
import com.ecclesiaflow.springsecurity.business.services.AuthenticationService;
import com.ecclesiaflow.springsecurity.business.domain.member.Member;
import com.ecclesiaflow.springsecurity.web.dto.JwtAuthenticationResponse;
import com.ecclesiaflow.springsecurity.web.dto.TemporaryTokenResponse;
import com.ecclesiaflow.springsecurity.web.payloads.RefreshTokenRequest;
import com.ecclesiaflow.springsecurity.web.payloads.SigninRequest;
import com.ecclesiaflow.springsecurity.web.payloads.TemporaryTokenRequest;
import com.ecclesiaflow.springsecurity.web.mappers.AuthenticationMapper;
import com.ecclesiaflow.springsecurity.web.mappers.MemberMapper;
import com.ecclesiaflow.springsecurity.web.mappers.TemporaryTokenMapper;
import com.ecclesiaflow.springsecurity.web.security.Jwt;
import com.ecclesiaflow.springsecurity.web.exception.InvalidCredentialsException;
import com.ecclesiaflow.springsecurity.web.exception.InvalidTokenException;
import com.ecclesiaflow.springsecurity.web.exception.JwtProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("AuthenticationController - Tests de couverture complète")
class AuthenticationControllerTest {

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private Jwt jwt;

    @Mock
    private TemporaryTokenMapper temporaryTokenMapper;

    @InjectMocks
    private AuthenticationController authenticationController;

    private SigninRequest signinRequest;
    private RefreshTokenRequest refreshTokenRequest;
    private TemporaryTokenRequest temporaryTokenRequest;
    private Member member;
    private SigninCredentials credentials;
    private UserTokens userTokens;
    private JwtAuthenticationResponse jwtResponse;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup SigninRequest
        signinRequest = new SigninRequest();
        signinRequest.setEmail("user@example.com");
        signinRequest.setPassword("password123");

        // Setup RefreshTokenRequest
        refreshTokenRequest = new RefreshTokenRequest();
        refreshTokenRequest.setRefreshToken("refresh-token-456");

        // Setup TemporaryTokenRequest
        temporaryTokenRequest = new TemporaryTokenRequest();
        temporaryTokenRequest.setEmail("user@example.com");

        // Setup credentials
        credentials = new SigninCredentials("user@example.com", "password123");

        // Setup Member
        member = Member.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .password("password123")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .enabled(true)
                .build();

        // Setup tokens
        userTokens = new UserTokens("access-token-123", "refresh-token-456");

        // Setup response
        jwtResponse = new JwtAuthenticationResponse();
        jwtResponse.setToken("access-token-123");
        jwtResponse.setRefreshToken("refresh-token-456");
    }

    // ====================================================================
    // Tests generateToken - Cas de succès
    // ====================================================================

    @Test
    @DisplayName("generateToken - Devrait retourner JwtAuthenticationResponse avec statut 200")
    void generateToken_ShouldReturnJwtAuthenticationResponse_OnSuccess() throws Exception {
        try (MockedStatic<AuthenticationMapper> mockedAuthMapper = mockStatic(AuthenticationMapper.class);
             MockedStatic<MemberMapper> mockedMemberMapper = mockStatic(MemberMapper.class)) {

            mockedMemberMapper.when(() -> MemberMapper.fromSigninRequest(signinRequest))
                    .thenReturn(credentials);
            mockedAuthMapper.when(() -> AuthenticationMapper.toDto(userTokens))
                    .thenReturn(jwtResponse);

            when(authenticationService.getAuthenticatedMember(any(SigninCredentials.class))).thenReturn(member);
            when(jwt.generateUserTokens(any(Member.class))).thenReturn(userTokens);

            ResponseEntity<JwtAuthenticationResponse> response = authenticationController.generateToken(signinRequest);

            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("access-token-123", response.getBody().getToken());
            assertEquals("refresh-token-456", response.getBody().getRefreshToken());

            verify(authenticationService).getAuthenticatedMember(credentials);
            verify(jwt).generateUserTokens(member);
        }
    }

    @Test
    @DisplayName("generateToken - Devrait appeler les mappers correctement")
    void generateToken_ShouldCallMappersCorrectly() throws Exception {
        try (MockedStatic<AuthenticationMapper> mockedAuthMapper = mockStatic(AuthenticationMapper.class);
             MockedStatic<MemberMapper> mockedMemberMapper = mockStatic(MemberMapper.class)) {

            mockedMemberMapper.when(() -> MemberMapper.fromSigninRequest(signinRequest))
                    .thenReturn(credentials);
            mockedAuthMapper.when(() -> AuthenticationMapper.toDto(userTokens))
                    .thenReturn(jwtResponse);

            when(authenticationService.getAuthenticatedMember(credentials)).thenReturn(member);
            when(jwt.generateUserTokens(member)).thenReturn(userTokens);

            authenticationController.generateToken(signinRequest);

            mockedMemberMapper.verify(() -> MemberMapper.fromSigninRequest(signinRequest));
            mockedAuthMapper.verify(() -> AuthenticationMapper.toDto(userTokens));
        }
    }

    // ====================================================================
    // Tests generateToken - Cas d'erreur
    // ====================================================================

    @Test
    @DisplayName("generateToken - Devrait propager InvalidCredentialsException")
    void generateToken_ShouldThrowInvalidCredentialsException_OnInvalidCredentials() throws Exception {
        try (MockedStatic<MemberMapper> mockedMemberMapper = mockStatic(MemberMapper.class)) {

            mockedMemberMapper.when(() -> MemberMapper.fromSigninRequest(signinRequest))
                    .thenReturn(credentials);

            when(authenticationService.getAuthenticatedMember(credentials))
                    .thenThrow(new InvalidCredentialsException("Identifiants invalides"));

            assertThrows(InvalidCredentialsException.class,
                    () -> authenticationController.generateToken(signinRequest));

            verify(authenticationService).getAuthenticatedMember(credentials);
            verify(jwt, never()).generateUserTokens(any());
        }
    }

    @Test
    @DisplayName("generateToken - Devrait propager InvalidTokenException")
    void generateToken_ShouldThrowInvalidTokenException_OnTokenError() throws Exception {
        try (MockedStatic<MemberMapper> mockedMemberMapper = mockStatic(MemberMapper.class)) {

            mockedMemberMapper.when(() -> MemberMapper.fromSigninRequest(signinRequest))
                    .thenReturn(credentials);

            when(authenticationService.getAuthenticatedMember(credentials)).thenReturn(member);
            when(jwt.generateUserTokens(member))
                    .thenThrow(new InvalidTokenException("Erreur génération token"));

            assertThrows(InvalidTokenException.class,
                    () -> authenticationController.generateToken(signinRequest));

            verify(jwt).generateUserTokens(member);
        }
    }

    @Test
    @DisplayName("generateToken - Devrait propager JwtProcessingException")
    void generateToken_ShouldThrowJwtProcessingException_OnProcessingError() throws Exception {
        try (MockedStatic<MemberMapper> mockedMemberMapper = mockStatic(MemberMapper.class)) {

            mockedMemberMapper.when(() -> MemberMapper.fromSigninRequest(signinRequest))
                    .thenReturn(credentials);

            when(authenticationService.getAuthenticatedMember(credentials)).thenReturn(member);
            when(jwt.generateUserTokens(member))
                    .thenThrow(new JwtProcessingException("Erreur traitement JWT"));

            assertThrows(JwtProcessingException.class,
                    () -> authenticationController.generateToken(signinRequest));
        }
    }

    // ====================================================================
    // Tests refreshToken - Cas de succès
    // ====================================================================

    @Test
    @DisplayName("refreshToken - Devrait retourner nouveau JwtAuthenticationResponse")
    void refreshToken_ShouldReturnNewJwtAuthenticationResponse_OnSuccess() throws Exception {
        JwtAuthenticationResponse newResponse = new JwtAuthenticationResponse();
        newResponse.setToken("new-access-token-789");
        newResponse.setRefreshToken("new-refresh-token-012");

        UserTokens newTokens = new UserTokens("new-access-token-789", "new-refresh-token-012");

        try (MockedStatic<AuthenticationMapper> mockedMapper = mockStatic(AuthenticationMapper.class)) {
            mockedMapper.when(() -> AuthenticationMapper.toDto(newTokens))
                    .thenReturn(newResponse);

            when(jwt.validateAndExtractEmail("refresh-token-456")).thenReturn("user@example.com");
            when(authenticationService.getMemberByEmail("user@example.com")).thenReturn(member);
            when(jwt.refreshTokenForMember("refresh-token-456", member)).thenReturn(newTokens);

            ResponseEntity<JwtAuthenticationResponse> response = authenticationController.refreshToken(refreshTokenRequest);

            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("new-access-token-789", response.getBody().getToken());
            assertEquals("new-refresh-token-012", response.getBody().getRefreshToken());

            verify(jwt).validateAndExtractEmail("refresh-token-456");
            verify(authenticationService).getMemberByEmail("user@example.com");
            verify(jwt).refreshTokenForMember("refresh-token-456", member);
        }
    }

    @Test
    @DisplayName("refreshToken - Devrait créer TokenCredentials correctement")
    void refreshToken_ShouldCreateTokenCredentialsCorrectly() throws Exception {
        try (MockedStatic<AuthenticationMapper> mockedMapper = mockStatic(AuthenticationMapper.class)) {
            mockedMapper.when(() -> AuthenticationMapper.toDto(any(UserTokens.class)))
                    .thenReturn(jwtResponse);

            when(jwt.validateAndExtractEmail("refresh-token-456")).thenReturn("user@example.com");
            when(authenticationService.getMemberByEmail("user@example.com")).thenReturn(member);
            when(jwt.refreshTokenForMember("refresh-token-456", member)).thenReturn(userTokens);

            authenticationController.refreshToken(refreshTokenRequest);

            verify(jwt).validateAndExtractEmail("refresh-token-456");
        }
    }

    // ====================================================================
    // Tests refreshToken - Cas d'erreur
    // ====================================================================

    @Test
    @DisplayName("refreshToken - Devrait propager InvalidTokenException si token invalide")
    void refreshToken_ShouldThrowInvalidTokenException_OnInvalidToken() throws Exception {
        when(jwt.validateAndExtractEmail("refresh-token-456"))
                .thenThrow(new InvalidTokenException("Token invalide ou expiré"));

        assertThrows(InvalidTokenException.class,
                () -> authenticationController.refreshToken(refreshTokenRequest));

        verify(jwt).validateAndExtractEmail("refresh-token-456");
        verify(authenticationService, never()).getMemberByEmail(anyString());
        verify(jwt, never()).refreshTokenForMember(anyString(), any());
    }

    @Test
    @DisplayName("refreshToken - Devrait propager JwtProcessingException lors du refresh")
    void refreshToken_ShouldThrowJwtProcessingException_OnRefreshError() throws Exception {
        when(jwt.validateAndExtractEmail("refresh-token-456")).thenReturn("user@example.com");
        when(authenticationService.getMemberByEmail("user@example.com")).thenReturn(member);
        when(jwt.refreshTokenForMember("refresh-token-456", member))
                .thenThrow(new JwtProcessingException("Erreur refresh token"));

        assertThrows(JwtProcessingException.class,
                () -> authenticationController.refreshToken(refreshTokenRequest));

        verify(jwt).refreshTokenForMember("refresh-token-456", member);
    }

    @Test
    @DisplayName("refreshToken - Devrait gérer le cas où l'email extrait est null")
    void refreshToken_ShouldHandleNullEmail() throws Exception {
        when(jwt.validateAndExtractEmail("refresh-token-456")).thenReturn(null);
        when(authenticationService.getMemberByEmail(null)).thenReturn(member);
        when(jwt.refreshTokenForMember("refresh-token-456", member)).thenReturn(userTokens);

        try (MockedStatic<AuthenticationMapper> mockedMapper = mockStatic(AuthenticationMapper.class)) {
            mockedMapper.when(() -> AuthenticationMapper.toDto(userTokens))
                    .thenReturn(jwtResponse);

            ResponseEntity<JwtAuthenticationResponse> response = authenticationController.refreshToken(refreshTokenRequest);

            assertNotNull(response);
            verify(authenticationService).getMemberByEmail(null);
        }
    }

    // ====================================================================
    // Tests generateTemporaryToken - Cas de succès
    // ====================================================================

    @Test
    @DisplayName("generateTemporaryToken - Devrait retourner TemporaryTokenResponse")
    void generateTemporaryToken_ShouldReturnTemporaryTokenResponse_OnSuccess() throws Exception {
        String tempToken = "temp-token-xyz";
        TemporaryTokenResponse tempResponse = TemporaryTokenResponse.builder().build();
        tempResponse.setTemporaryToken(tempToken);

        when(temporaryTokenMapper.extractEmail(temporaryTokenRequest)).thenReturn("user@example.com");
        when(jwt.generateTemporaryToken("user@example.com")).thenReturn(tempToken);
        when(temporaryTokenMapper.toResponse(tempToken)).thenReturn(tempResponse);

        ResponseEntity<TemporaryTokenResponse> response = authenticationController.generateTemporaryToken(temporaryTokenRequest);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(tempToken, response.getBody().getTemporaryToken());

        verify(temporaryTokenMapper).extractEmail(temporaryTokenRequest);
        verify(jwt).generateTemporaryToken("user@example.com");
        verify(temporaryTokenMapper).toResponse(tempToken);
    }

    @Test
    @DisplayName("generateTemporaryToken - Devrait appeler mapper dans l'ordre correct")
    void generateTemporaryToken_ShouldCallMapperInCorrectOrder() throws Exception {
        String email = "test@ecclesiaflow.com";
        String tempToken = "temp-123";
        TemporaryTokenResponse tempResponse = TemporaryTokenResponse.builder().build();

        when(temporaryTokenMapper.extractEmail(temporaryTokenRequest)).thenReturn(email);
        when(jwt.generateTemporaryToken(email)).thenReturn(tempToken);
        when(temporaryTokenMapper.toResponse(tempToken)).thenReturn(tempResponse);

        authenticationController.generateTemporaryToken(temporaryTokenRequest);

        verify(temporaryTokenMapper).extractEmail(temporaryTokenRequest);
        verify(jwt).generateTemporaryToken(email);
        verify(temporaryTokenMapper).toResponse(tempToken);
    }

    // ====================================================================
    // Tests generateTemporaryToken - Cas d'erreur
    // ====================================================================

    @Test
    @DisplayName("generateTemporaryToken - Devrait propager InvalidTokenException")
    void generateTemporaryToken_ShouldThrowInvalidTokenException_OnTokenError() throws Exception {
        when(temporaryTokenMapper.extractEmail(temporaryTokenRequest)).thenReturn("user@example.com");
        when(jwt.generateTemporaryToken("user@example.com"))
                .thenThrow(new InvalidTokenException("Erreur génération token temporaire"));

        assertThrows(InvalidTokenException.class,
                () -> authenticationController.generateTemporaryToken(temporaryTokenRequest));

        verify(temporaryTokenMapper).extractEmail(temporaryTokenRequest);
        verify(jwt).generateTemporaryToken("user@example.com");
        verify(temporaryTokenMapper, never()).toResponse(anyString());
    }

    @Test
    @DisplayName("generateTemporaryToken - Devrait propager JwtProcessingException")
    void generateTemporaryToken_ShouldThrowJwtProcessingException_OnProcessingError() throws Exception {
        when(temporaryTokenMapper.extractEmail(temporaryTokenRequest)).thenReturn("user@example.com");
        when(jwt.generateTemporaryToken("user@example.com"))
                .thenThrow(new JwtProcessingException("Erreur traitement JWT"));

        assertThrows(JwtProcessingException.class,
                () -> authenticationController.generateTemporaryToken(temporaryTokenRequest));
    }

    @Test
    @DisplayName("generateTemporaryToken - Devrait gérer email null du mapper")
    void generateTemporaryToken_ShouldHandleNullEmailFromMapper() throws Exception {
        String tempToken = "temp-token";
        TemporaryTokenResponse tempResponse = TemporaryTokenResponse.builder()
                .temporaryToken("temp-token")
                .expiresIn(1800)
                .message("Token temporaire généré")
                .build();

        when(temporaryTokenMapper.extractEmail(temporaryTokenRequest)).thenReturn(null);
        when(jwt.generateTemporaryToken(null)).thenReturn(tempToken);
        when(temporaryTokenMapper.toResponse(tempToken)).thenReturn(tempResponse);

        ResponseEntity<TemporaryTokenResponse> response = authenticationController.generateTemporaryToken(temporaryTokenRequest);

        assertNotNull(response);
        verify(jwt).generateTemporaryToken(null);
    }

    @Test
    @DisplayName("generateTemporaryToken - Devrait gérer email vide du mapper")
    void generateTemporaryToken_ShouldHandleEmptyEmailFromMapper() throws Exception {
        String tempToken = "temp-token";
        TemporaryTokenResponse tempResponse = TemporaryTokenResponse.builder()
                .temporaryToken("temp-token")
                .expiresIn(1800)
                .message("Token temporaire généré")
                .build();

        when(temporaryTokenMapper.extractEmail(temporaryTokenRequest)).thenReturn("");
        when(jwt.generateTemporaryToken("")).thenReturn(tempToken);
        when(temporaryTokenMapper.toResponse(tempToken)).thenReturn(tempResponse);

        ResponseEntity<TemporaryTokenResponse> response = authenticationController.generateTemporaryToken(temporaryTokenRequest);

        assertNotNull(response);
        verify(jwt).generateTemporaryToken("");
    }
}