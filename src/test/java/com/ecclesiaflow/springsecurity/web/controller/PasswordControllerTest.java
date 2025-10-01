package com.ecclesiaflow.springsecurity.web.controller;

import com.ecclesiaflow.springsecurity.business.domain.token.UserTokens;
import com.ecclesiaflow.springsecurity.business.domain.member.Member;
import com.ecclesiaflow.springsecurity.web.dto.PasswordManagementResponse;
import com.ecclesiaflow.springsecurity.web.mappers.PasswordManagementMapper;
import com.ecclesiaflow.springsecurity.business.services.PasswordService;
import com.ecclesiaflow.springsecurity.business.services.AuthenticationService;
import com.ecclesiaflow.springsecurity.web.payloads.ChangePasswordRequest;
import com.ecclesiaflow.springsecurity.web.payloads.SetPasswordRequest;
import com.ecclesiaflow.springsecurity.web.security.Jwt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PasswordControllerTest {

    @Mock
    private PasswordService passwordService;

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private PasswordController passwordController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void setPassword_ShouldReturnPasswordManagementResponse_WhenSuccess() {
        // Arrange
        String authorizationHeader = "Bearer temporary-token-123";
        SetPasswordRequest request = new SetPasswordRequest();
        request.setPassword("NewStrongPassword1!");

        String tempToken = "temporary-token-123";
        String email = "user@example.com";
        Member member = Member.builder().id(null).email(email).build();
        UserTokens userTokens = new UserTokens("access-token", "refresh-token");
        long expiresIn = 60L;

        PasswordManagementResponse expectedResponse = PasswordManagementResponse.builder()
                .message("Mot de passe défini avec succès. Vous êtes maintenant connecté.")
                .accessToken(userTokens.accessToken())
                .refreshToken(userTokens.refreshToken())
                .expiresIn(expiresIn)
                .build();

        try (MockedStatic<PasswordManagementMapper> mapperMockedStatic = mockStatic(PasswordManagementMapper.class)) {
            mapperMockedStatic.when(() -> PasswordManagementMapper.toDtoWithTokens(
                    "Mot de passe défini avec succès. Vous êtes maintenant connecté.",
                    userTokens,
                    expiresIn
            )).thenReturn(expectedResponse);

            when(authenticationService.getEmailFromValidatedTempToken(tempToken)).thenReturn(email);
            doNothing().when(passwordService).setInitialPassword(email, request.getPassword());
            when(authenticationService.getMemberByEmail(email)).thenReturn(member);
            when(jwt.generateUserTokens(member)).thenReturn(userTokens);

            setAccessTokenExpiration(passwordController, expiresIn * 1000);

            // Act
            ResponseEntity<PasswordManagementResponse> response = passwordController.setPassword(authorizationHeader, request);

            // Assert
            assertNotNull(response);
            assertEquals(200, response.getStatusCode().value());
            assertEquals(expectedResponse, response.getBody());

            verify(authenticationService).getEmailFromValidatedTempToken(tempToken);
            verify(passwordService).setInitialPassword(email, request.getPassword());
            verify(authenticationService).getMemberByEmail(email);
            verify(jwt).generateUserTokens(member);
        }
    }

    @Test
    void setPassword_ShouldThrowRuntimeException_WhenAuthorizationHeaderIsMissing() {
        // Arrange
        String badHeader = "BadHeader token";

        SetPasswordRequest request = new SetPasswordRequest();
        request.setPassword("password");

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> passwordController.setPassword(badHeader, request));

        assertEquals("Header Authorization manquant ou format invalide. Format attendu: Bearer {token}", ex.getMessage());
    }

    @Test
    void setPassword_ShouldThrowException_WhenTokenValidationFails() {
        // Arrange
        String authorizationHeader = "Bearer invalid-token";
        SetPasswordRequest request = new SetPasswordRequest();
        request.setPassword("password");

        when(authenticationService.getEmailFromValidatedTempToken("invalid-token"))
                .thenThrow(new RuntimeException("Token invalide"));

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> passwordController.setPassword(authorizationHeader, request));

        assertEquals("Token invalide", ex.getMessage());
    }

    @Test
    void setPassword_ShouldThrowException_WhenSetInitialPasswordFails() {
        // Arrange
        String authorizationHeader = "Bearer temp-token";
        SetPasswordRequest request = new SetPasswordRequest();
        request.setPassword("password");

        when(authenticationService.getEmailFromValidatedTempToken("temp-token")).thenReturn("user@example.com");
        doThrow(new RuntimeException("Erreur lors de la définition du mot de passe"))
                .when(passwordService).setInitialPassword(anyString(), anyString());

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> passwordController.setPassword(authorizationHeader, request));

        assertEquals("Erreur lors de la définition du mot de passe", ex.getMessage());
    }

    @Test
    void changePassword_ShouldReturnOk_WhenSuccess() {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setEmail("user@example.com");
        request.setCurrentPassword("oldPass");
        request.setNewPassword("newPass");

        doNothing().when(passwordService).changePassword(anyString(), anyString(), anyString());

        // Act
        ResponseEntity<Void> response = passwordController.changePassword(request);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());

        verify(passwordService).changePassword(request.getEmail(), request.getCurrentPassword(), request.getNewPassword());
    }

    @Test
    void changePassword_ShouldThrowException_WhenChangePasswordFails() {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setEmail("user@example.com");
        request.setCurrentPassword("oldPass");
        request.setNewPassword("newPass");

        doThrow(new RuntimeException("Mot de passe actuel incorrect"))
                .when(passwordService).changePassword(anyString(), anyString(), anyString());

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> passwordController.changePassword(request));

        assertEquals("Mot de passe actuel incorrect", ex.getMessage());
    }

    private void setAccessTokenExpiration(PasswordController controller, long expiration) {
        try {
            java.lang.reflect.Field field = PasswordController.class.getDeclaredField("accessTokenExpiration");
            field.setAccessible(true);
            field.set(controller, expiration);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
