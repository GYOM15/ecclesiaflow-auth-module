package com.ecclesiaflow.springsecurity.web.controller;

import com.ecclesiaflow.springsecurity.web.delegate.AuthenticationDelegate;
import com.ecclesiaflow.springsecurity.web.model.JwtAuthenticationResponse;
import com.ecclesiaflow.springsecurity.web.model.RefreshTokenRequest;
import com.ecclesiaflow.springsecurity.web.model.SigninRequest;
import com.ecclesiaflow.springsecurity.web.model.TemporaryTokenRequest;
import com.ecclesiaflow.springsecurity.web.model.TemporaryTokenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("AuthenticationController - Tests avec pattern Delegate")
class AuthenticationControllerTest {

    @Mock
    private AuthenticationDelegate authenticationDelegate;

    @InjectMocks
    private AuthenticationController authenticationController;

    private SigninRequest signinRequest;
    private RefreshTokenRequest refreshTokenRequest;
    private TemporaryTokenRequest temporaryTokenRequest;
    private JwtAuthenticationResponse jwtResponse;
    private TemporaryTokenResponse temporaryTokenResponse;

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

        // Setup TemporaryToken
        temporaryTokenRequest = new TemporaryTokenRequest();
        temporaryTokenRequest.setEmail("user@example.com");

        // Setup JWT response
        jwtResponse = new JwtAuthenticationResponse();
        jwtResponse.setToken("access-token-123");
        jwtResponse.setRefreshToken("refresh-token-456");

        // Setup Temporary Token response
        temporaryTokenResponse = new TemporaryTokenResponse();
        temporaryTokenResponse.setTemporaryToken("temp-token-789");
        temporaryTokenResponse.setExpiresIn(900);
        temporaryTokenResponse.setMessage("Token temporaire généré avec succès");
    }

    // ====================================================================
    // Tests generateToken
    // ====================================================================

    @Test
    @DisplayName("generateToken - Devrait déléguer au AuthenticationDelegate")
    void generateToken_ShouldDelegateToAuthenticationDelegate() throws Exception {
        // Given
        when(authenticationDelegate.generateToken(signinRequest))
                .thenReturn(ResponseEntity.ok(jwtResponse));

        // When
        ResponseEntity<JwtAuthenticationResponse> response = authenticationController._authGenerateToken(signinRequest);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("access-token-123", response.getBody().getToken());
        assertEquals("refresh-token-456", response.getBody().getRefreshToken());

        verify(authenticationDelegate).generateToken(signinRequest);
    }

    // ====================================================================
    // Tests refreshToken
    // ====================================================================

    @Test
    @DisplayName("refreshToken - Devrait déléguer au AuthenticationDelegate")
    void refreshToken_ShouldDelegateToAuthenticationDelegate() throws Exception {
        // Given
        when(authenticationDelegate.refreshToken(refreshTokenRequest))
                .thenReturn(ResponseEntity.ok(jwtResponse));

        // When
        ResponseEntity<JwtAuthenticationResponse> response = authenticationController._authRefreshToken(refreshTokenRequest);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("access-token-123", response.getBody().getToken());
        assertEquals("refresh-token-456", response.getBody().getRefreshToken());

        verify(authenticationDelegate).refreshToken(refreshTokenRequest);
    }

    // ====================================================================
    // Tests generateTemporaryToken
    // ====================================================================

    @Test
    @DisplayName("generateTemporaryToken - Devrait déléguer au AuthenticationDelegate")
    void generateTemporaryToken_ShouldDelegateToAuthenticationDelegate() throws Exception {
        // Given
        when(authenticationDelegate.generateTemporaryToken(temporaryTokenRequest))
                .thenReturn(ResponseEntity.ok(temporaryTokenResponse));

        // When
        ResponseEntity<TemporaryTokenResponse> response = authenticationController._authGenerateTemporaryToken(temporaryTokenRequest);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("temp-token-789", response.getBody().getTemporaryToken());
        assertEquals(900, response.getBody().getExpiresIn());

        verify(authenticationDelegate).generateTemporaryToken(temporaryTokenRequest);
    }
}