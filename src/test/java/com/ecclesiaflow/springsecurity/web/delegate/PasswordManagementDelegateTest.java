package com.ecclesiaflow.springsecurity.web.delegate;

import com.ecclesiaflow.springsecurity.business.domain.member.Member;
import com.ecclesiaflow.springsecurity.business.domain.token.UserTokens;
import com.ecclesiaflow.springsecurity.business.services.AuthenticationService;
import com.ecclesiaflow.springsecurity.business.services.PasswordService;
import com.ecclesiaflow.springsecurity.web.mappers.OpenApiModelMapper;
import com.ecclesiaflow.springsecurity.web.model.ChangePasswordRequest;
import com.ecclesiaflow.springsecurity.web.model.PasswordManagementResponse;
import com.ecclesiaflow.springsecurity.web.model.SetPasswordRequest;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour PasswordManagementDelegate.
 * <p>
 * Teste la logique métier de gestion des mots de passe avec les modèles OpenAPI.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordManagementDelegate - Tests de logique métier")
class PasswordManagementDelegateTest {

    @Mock
    private PasswordService passwordService;

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private Jwt jwt;

    @Mock
    private OpenApiModelMapper openApiModelMapper;

    @InjectMocks
    private PasswordManagementDelegate passwordManagementDelegate;

    private Member member;
    private UserTokens userTokens;
    private SetPasswordRequest setPasswordRequest;
    private ChangePasswordRequest changePasswordRequest;
    private PasswordManagementResponse passwordManagementResponse;

    @BeforeEach
    void setUp() {
        // Configuration de l'expiration du token
        ReflectionTestUtils.setField(passwordManagementDelegate, "accessTokenExpiration", 60000L);

        // Setup Member
        member = Member.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .password("hashedPassword")
                .enabled(true)
                .build();

        // Setup UserTokens
        userTokens = new UserTokens("access-token-123", "refresh-token-456");

        // Setup SetPasswordRequest
        setPasswordRequest = new SetPasswordRequest();
        setPasswordRequest.setPassword("NewStrongPassword1!");

        // Setup ChangePasswordRequest
        changePasswordRequest = new ChangePasswordRequest();
        changePasswordRequest.setEmail("user@example.com");
        changePasswordRequest.setCurrentPassword("OldPassword123!");
        changePasswordRequest.setNewPassword("NewPassword456!");

        // Setup PasswordManagementResponse
        passwordManagementResponse = new PasswordManagementResponse()
                .message("Mot de passe défini avec succès. Vous êtes maintenant connecté.")
                .accessToken("access-token-123")
                .refreshToken("refresh-token-456")
                .expiresIn(60L);
    }

    // ====================================================================
    // Tests setPassword - Cas de succès
    // ====================================================================

    @Test
    @DisplayName("setPassword - Devrait définir le mot de passe et retourner les tokens")
    void setPassword_ShouldSetPasswordAndReturnTokens() {
        // Given
        String authHeader = "Bearer temp-token-123";
        String email = "user@example.com";

        when(authenticationService.getEmailFromValidatedTempToken("temp-token-123")).thenReturn(email);
        doNothing().when(passwordService).setInitialPassword(email, "NewStrongPassword1!");
        when(authenticationService.getMemberByEmail(email)).thenReturn(member);
        when(jwt.generateUserTokens(member)).thenReturn(userTokens);
        when(openApiModelMapper.createPasswordManagementResponse(
                "Mot de passe défini avec succès. Vous êtes maintenant connecté.",
                userTokens,
                60L
        )).thenReturn(passwordManagementResponse);

        // When
        ResponseEntity<PasswordManagementResponse> response = passwordManagementDelegate.setPassword(
                authHeader, setPasswordRequest
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getAccessToken()).isEqualTo("access-token-123");
        assertThat(response.getBody().getRefreshToken()).isEqualTo("refresh-token-456");
        assertThat(response.getBody().getExpiresIn()).isEqualTo(60L);

        // Verify interactions
        verify(authenticationService).getEmailFromValidatedTempToken("temp-token-123");
        verify(passwordService).setInitialPassword(email, "NewStrongPassword1!");
        verify(authenticationService).getMemberByEmail(email);
        verify(jwt).generateUserTokens(member);
        verify(openApiModelMapper).createPasswordManagementResponse(anyString(), eq(userTokens), eq(60L));
    }

    @Test
    @DisplayName("setPassword - Devrait extraire correctement le token du header")
    void setPassword_ShouldExtractTokenCorrectly() {
        // Given
        String authHeader = "Bearer my-temp-token";
        String email = "test@example.com";

        when(authenticationService.getEmailFromValidatedTempToken("my-temp-token")).thenReturn(email);
        doNothing().when(passwordService).setInitialPassword(anyString(), anyString());
        when(authenticationService.getMemberByEmail(email)).thenReturn(member);
        when(jwt.generateUserTokens(member)).thenReturn(userTokens);
        when(openApiModelMapper.createPasswordManagementResponse(anyString(), any(), anyLong()))
                .thenReturn(passwordManagementResponse);

        // When
        passwordManagementDelegate.setPassword(authHeader, setPasswordRequest);

        // Then
        verify(authenticationService).getEmailFromValidatedTempToken("my-temp-token");
    }

    @Test
    @DisplayName("setPassword - Devrait calculer expiresIn correctement (division par 1000)")
    void setPassword_ShouldCalculateExpiresInCorrectly() {
        // Given
        ReflectionTestUtils.setField(passwordManagementDelegate, "accessTokenExpiration", 120000L);
        String authHeader = "Bearer token";
        String email = "user@example.com";

        when(authenticationService.getEmailFromValidatedTempToken(anyString())).thenReturn(email);
        doNothing().when(passwordService).setInitialPassword(anyString(), anyString());
        when(authenticationService.getMemberByEmail(email)).thenReturn(member);
        when(jwt.generateUserTokens(member)).thenReturn(userTokens);
        when(openApiModelMapper.createPasswordManagementResponse(anyString(), any(), eq(120L)))
                .thenReturn(passwordManagementResponse);

        // When
        passwordManagementDelegate.setPassword(authHeader, setPasswordRequest);

        // Then
        verify(openApiModelMapper).createPasswordManagementResponse(
                anyString(),
                any(UserTokens.class),
                eq(120L) // 120000 / 1000
        );
    }

    // ====================================================================
    // Tests setPassword - Cas d'erreur (extractTokenFromHeader)
    // ====================================================================

    @Test
    @DisplayName("setPassword - Devrait lancer RuntimeException si header Authorization est null")
    void setPassword_ShouldThrowRuntimeException_WhenAuthHeaderIsNull() {
        // When & Then
        assertThatThrownBy(() -> passwordManagementDelegate.setPassword(null, setPasswordRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Header Authorization manquant ou format invalide. Format attendu: Bearer {token}");

        verifyNoInteractions(authenticationService, passwordService, jwt, openApiModelMapper);
    }

    @Test
    @DisplayName("setPassword - Devrait lancer RuntimeException si header ne commence pas par 'Bearer '")
    void setPassword_ShouldThrowRuntimeException_WhenHeaderDoesNotStartWithBearer() {
        // When & Then
        assertThatThrownBy(() -> passwordManagementDelegate.setPassword("Basic token123", setPasswordRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Header Authorization manquant ou format invalide. Format attendu: Bearer {token}");

        verifyNoInteractions(authenticationService, passwordService, jwt, openApiModelMapper);
    }

    @Test
    @DisplayName("setPassword - Devrait lancer RuntimeException si header est vide")
    void setPassword_ShouldThrowRuntimeException_WhenHeaderIsEmpty() {
        // When & Then
        assertThatThrownBy(() -> passwordManagementDelegate.setPassword("", setPasswordRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Header Authorization manquant ou format invalide. Format attendu: Bearer {token}");
    }

    @Test
    @DisplayName("setPassword - Devrait lancer RuntimeException si header est 'Bearer' sans token")
    void setPassword_ShouldThrowRuntimeException_WhenHeaderIsBearerWithoutToken() {
        // When & Then
        assertThatThrownBy(() -> passwordManagementDelegate.setPassword("Bearer", setPasswordRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Header Authorization manquant ou format invalide. Format attendu: Bearer {token}");
    }

    @Test
    @DisplayName("setPassword - Devrait accepter header 'Bearer ' avec token")
    void setPassword_ShouldAcceptBearerWithSpace() {
        // Given
        String authHeader = "Bearer valid-token";
        String email = "user@example.com";

        when(authenticationService.getEmailFromValidatedTempToken("valid-token")).thenReturn(email);
        doNothing().when(passwordService).setInitialPassword(anyString(), anyString());
        when(authenticationService.getMemberByEmail(email)).thenReturn(member);
        when(jwt.generateUserTokens(member)).thenReturn(userTokens);
        when(openApiModelMapper.createPasswordManagementResponse(anyString(), any(), anyLong()))
                .thenReturn(passwordManagementResponse);

        // When
        ResponseEntity<PasswordManagementResponse> result = passwordManagementDelegate.setPassword(
                authHeader, setPasswordRequest
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ====================================================================
    // Tests setPassword - Cas d'erreur métier
    // ====================================================================

    @Test
    @DisplayName("setPassword - Devrait propager exception si validation du token échoue")
    void setPassword_ShouldThrowException_WhenTokenValidationFails() {
        // Given
        String authHeader = "Bearer invalid-token";

        when(authenticationService.getEmailFromValidatedTempToken("invalid-token"))
                .thenThrow(new RuntimeException("Token invalide"));

        // When & Then
        assertThatThrownBy(() -> passwordManagementDelegate.setPassword(authHeader, setPasswordRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Token invalide");

        verify(authenticationService).getEmailFromValidatedTempToken("invalid-token");
        verifyNoInteractions(passwordService);
    }

    @Test
    @DisplayName("setPassword - Devrait propager exception si setInitialPassword échoue")
    void setPassword_ShouldThrowException_WhenSetInitialPasswordFails() {
        // Given
        String authHeader = "Bearer temp-token";
        String email = "user@example.com";

        when(authenticationService.getEmailFromValidatedTempToken("temp-token")).thenReturn(email);
        doThrow(new RuntimeException("Erreur lors de la définition du mot de passe"))
                .when(passwordService).setInitialPassword(email, setPasswordRequest.getPassword());

        // When & Then
        assertThatThrownBy(() -> passwordManagementDelegate.setPassword(authHeader, setPasswordRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Erreur lors de la définition du mot de passe");

        verify(passwordService).setInitialPassword(email, setPasswordRequest.getPassword());
    }

    @Test
    @DisplayName("setPassword - Devrait propager exception si getMemberByEmail échoue")
    void setPassword_ShouldThrowException_WhenGetMemberByEmailFails() {
        // Given
        String authHeader = "Bearer temp-token";
        String email = "user@example.com";

        when(authenticationService.getEmailFromValidatedTempToken("temp-token")).thenReturn(email);
        doNothing().when(passwordService).setInitialPassword(email, setPasswordRequest.getPassword());
        when(authenticationService.getMemberByEmail(email))
                .thenThrow(new RuntimeException("Membre non trouvé"));

        // When & Then
        assertThatThrownBy(() -> passwordManagementDelegate.setPassword(authHeader, setPasswordRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Membre non trouvé");

        verify(authenticationService).getMemberByEmail(email);
    }

    @Test
    @DisplayName("setPassword - Devrait propager exception si generateUserTokens échoue")
    void setPassword_ShouldThrowException_WhenGenerateUserTokensFails() {
        // Given
        String authHeader = "Bearer temp-token";
        String email = "user@example.com";

        when(authenticationService.getEmailFromValidatedTempToken("temp-token")).thenReturn(email);
        doNothing().when(passwordService).setInitialPassword(email, setPasswordRequest.getPassword());
        when(authenticationService.getMemberByEmail(email)).thenReturn(member);
        when(jwt.generateUserTokens(member))
                .thenThrow(new RuntimeException("Erreur génération tokens"));

        // When & Then
        assertThatThrownBy(() -> passwordManagementDelegate.setPassword(authHeader, setPasswordRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Erreur génération tokens");

        verify(jwt).generateUserTokens(member);
    }

    // ====================================================================
    // Tests changePassword - Cas de succès
    // ====================================================================

    @Test
    @DisplayName("changePassword - Devrait retourner 200 OK en cas de succès")
    void changePassword_ShouldReturnOk_WhenSuccess() {
        // Given
        doNothing().when(passwordService).changePassword(
                changePasswordRequest.getEmail(),
                changePasswordRequest.getCurrentPassword(),
                changePasswordRequest.getNewPassword()
        );

        // When
        ResponseEntity<Void> response = passwordManagementDelegate.changePassword(changePasswordRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNull();

        verify(passwordService).changePassword(
                changePasswordRequest.getEmail(),
                changePasswordRequest.getCurrentPassword(),
                changePasswordRequest.getNewPassword()
        );
    }

    @Test
    @DisplayName("changePassword - Devrait passer les bons paramètres au service")
    void changePassword_ShouldPassCorrectParametersToService() {
        // Given
        String email = "test@ecclesiaflow.com";
        String currentPwd = "Current123!";
        String newPwd = "NewPassword456!";

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setEmail(email);
        request.setCurrentPassword(currentPwd);
        request.setNewPassword(newPwd);

        doNothing().when(passwordService).changePassword(email, currentPwd, newPwd);

        // When
        passwordManagementDelegate.changePassword(request);

        // Then
        verify(passwordService).changePassword(email, currentPwd, newPwd);
    }

    // ====================================================================
    // Tests changePassword - Cas d'erreur
    // ====================================================================

    @Test
    @DisplayName("changePassword - Devrait propager exception si mot de passe actuel incorrect")
    void changePassword_ShouldThrowException_WhenCurrentPasswordIncorrect() {
        // Given
        doThrow(new RuntimeException("Mot de passe actuel incorrect"))
                .when(passwordService).changePassword(
                        changePasswordRequest.getEmail(),
                        changePasswordRequest.getCurrentPassword(),
                        changePasswordRequest.getNewPassword()
                );

        // When & Then
        assertThatThrownBy(() -> passwordManagementDelegate.changePassword(changePasswordRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Mot de passe actuel incorrect");

        verify(passwordService).changePassword(
                changePasswordRequest.getEmail(),
                changePasswordRequest.getCurrentPassword(),
                changePasswordRequest.getNewPassword()
        );
    }

    @Test
    @DisplayName("changePassword - Devrait propager exception si utilisateur non trouvé")
    void changePassword_ShouldThrowException_WhenUserNotFound() {
        // Given
        doThrow(new RuntimeException("Utilisateur non trouvé"))
                .when(passwordService).changePassword(anyString(), anyString(), anyString());

        // When & Then
        assertThatThrownBy(() -> passwordManagementDelegate.changePassword(changePasswordRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Utilisateur non trouvé");
    }

    @Test
    @DisplayName("changePassword - Devrait propager exception générique du service")
    void changePassword_ShouldThrowException_OnServiceFailure() {
        // Given
        doThrow(new RuntimeException("Erreur interne du service"))
                .when(passwordService).changePassword(anyString(), anyString(), anyString());

        // When & Then
        assertThatThrownBy(() -> passwordManagementDelegate.changePassword(changePasswordRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Erreur interne du service");
    }
}
