package com.ecclesiaflow.springsecurity.web.controller;

import com.ecclesiaflow.springsecurity.web.delegate.PasswordManagementDelegate;
import com.ecclesiaflow.springsecurity.web.model.ChangePasswordRequest;
import com.ecclesiaflow.springsecurity.web.model.PasswordManagementResponse;
import com.ecclesiaflow.springsecurity.web.model.SetPasswordRequest;
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

@DisplayName("PasswordController - Tests avec pattern Delegate")
class PasswordControllerTest {

    @Mock
    private PasswordManagementDelegate passwordManagementDelegate;

    @InjectMocks
    private PasswordController passwordController;

    private SetPasswordRequest setPasswordRequest;
    private ChangePasswordRequest changePasswordRequest;
    private PasswordManagementResponse passwordManagementResponse;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup SetPasswordRequest
        setPasswordRequest = new SetPasswordRequest();
        setPasswordRequest.setPassword("NewStrongPassword1!");

        // Setup ChangePasswordRequest
        changePasswordRequest = new ChangePasswordRequest();
        changePasswordRequest.setEmail("user@example.com");
        changePasswordRequest.setCurrentPassword("oldPass");
        changePasswordRequest.setNewPassword("newPass");

        // Setup PasswordManagementResponse
        passwordManagementResponse = new PasswordManagementResponse()
                .message("Mot de passe défini avec succès")
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .expiresIn(60);
    }

    // ====================================================================
    // Tests setPassword - Cas de succès
    // ====================================================================

    @Test
    @DisplayName("setPassword - Devrait déléguer au PasswordManagementDelegate")
    void setPassword_ShouldDelegateToPasswordManagementDelegate() {
        // Given
        String authHeader = "Bearer temporary-token-123";
        when(passwordManagementDelegate.setPassword(authHeader, setPasswordRequest))
                .thenReturn(ResponseEntity.ok(passwordManagementResponse));

        // When
        ResponseEntity<PasswordManagementResponse> response = passwordController._authSetInitialPassword(authHeader, setPasswordRequest);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("access-token", response.getBody().getAccessToken());
        assertEquals("refresh-token", response.getBody().getRefreshToken());
        
        verify(passwordManagementDelegate).setPassword(authHeader, setPasswordRequest);
    }

    // ====================================================================
    // Tests changePassword
    // ====================================================================

    @Test
    @DisplayName("changePassword - Devrait déléguer au PasswordManagementDelegate")
    void changePassword_ShouldDelegateToPasswordManagementDelegate() {
        // Given
        when(passwordManagementDelegate.changePassword(changePasswordRequest))
                .thenReturn(ResponseEntity.ok(new PasswordManagementResponse().message("Mot de passe changé avec succès")));

        // When
        ResponseEntity<PasswordManagementResponse> response = passwordController._authChangePassword(changePasswordRequest);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Mot de passe changé avec succès", response.getBody().getMessage());
        
        verify(passwordManagementDelegate).changePassword(changePasswordRequest);
    }
}