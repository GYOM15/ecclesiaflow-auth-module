package com.ecclesiaflow.springsecurity.web.delegate;

import com.ecclesiaflow.springsecurity.business.domain.member.Member;
import com.ecclesiaflow.springsecurity.business.domain.token.UserTokens;
import com.ecclesiaflow.springsecurity.business.exceptions.MemberNotFoundException;
import com.ecclesiaflow.springsecurity.business.services.AuthenticationService;
import com.ecclesiaflow.springsecurity.business.services.PasswordService;
import com.ecclesiaflow.springsecurity.web.constants.Messages;
import com.ecclesiaflow.springsecurity.web.exception.InvalidRequestException;
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

    @Mock
    private jakarta.servlet.http.HttpServletRequest httpServletRequest;

    @InjectMocks
    private PasswordManagementDelegate passwordManagementDelegate;

    private Member member;
    private UserTokens userTokens;
    private SetPasswordRequest setPasswordRequest;
    private ChangePasswordRequest changePasswordRequest;
    private PasswordManagementResponse passwordManagementResponse;

    private UUID memberId;
    private String email;

    @BeforeEach
    void setUp() {
        // Configuration de l'expiration du token
        ReflectionTestUtils.setField(passwordManagementDelegate, "accessTokenExpiration", 60000L);

        // Setup common data
        memberId = UUID.randomUUID();
        email = "user@example.com";

        // Setup Member
        member = Member.builder()
                .id(UUID.randomUUID())
                .memberId(memberId)
                .email(email)
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
        changePasswordRequest.setCurrentPassword("OldPassword123!");
        changePasswordRequest.setNewPassword("NewPassword456!");

        // Setup PasswordManagementResponse
        passwordManagementResponse = new PasswordManagementResponse()
                .message("Mot de passe défini avec succès. Vous êtes maintenant connecté.")
                .accessToken("access-token-123")
                .refreshToken("refresh-token-456")
                .expiresIn(60);
    }

    // ====================================================================
    // Tests setPassword - Cas de succès
    // ====================================================================

    // ====================================================================
    // Tests setPassword - Cas d'erreur (extractTokenFromHeader)
    // ====================================================================

    @Test
    @DisplayName("setPassword - Devrait lancer InvalidRequestException si header Authorization est null")
    void setPassword_ShouldThrowRuntimeException_WhenAuthHeaderIsNull() {
        // When & Then
        assertThatThrownBy(() -> passwordManagementDelegate.setPassword(null, setPasswordRequest))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining(Messages.INVALID_AUTH_HEADER);

        verifyNoInteractions(authenticationService, passwordService, jwt, openApiModelMapper);
    }

    @Test
    @DisplayName("setPassword - Devrait lancer InvalidRequestException si header ne commence pas par 'Bearer '")
    void setPassword_ShouldThrowRuntimeException_WhenHeaderDoesNotStartWithBearer() {
        // When & Then
        assertThatThrownBy(() -> passwordManagementDelegate.setPassword("Basic token123", setPasswordRequest))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining(Messages.INVALID_AUTH_HEADER);

        verifyNoInteractions(authenticationService, passwordService, jwt, openApiModelMapper);
    }

    @Test
    @DisplayName("setPassword - Devrait lancer InvalidRequestException si header est vide")
    void setPassword_ShouldThrowRuntimeException_WhenHeaderIsEmpty() {
        // When & Then
        assertThatThrownBy(() -> passwordManagementDelegate.setPassword("", setPasswordRequest))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining(Messages.INVALID_AUTH_HEADER);
    }

    @Test
    @DisplayName("setPassword - Devrait lancer InvalidRequestException si header est 'Bearer' sans token")
    void setPassword_ShouldThrowRuntimeException_WhenHeaderIsBearerWithoutToken() {
        // When & Then
        assertThatThrownBy(() -> passwordManagementDelegate.setPassword("Bearer", setPasswordRequest))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining(Messages.INVALID_AUTH_HEADER);
    }

    @Test
    @DisplayName("setPassword - Devrait accepter header 'Bearer ' avec token")
    void setPassword_ShouldAcceptBearerWithSpace() {
        // Given
        String authHeader = "Bearer valid-token";
        String token = "valid-token";

        // Mock JWT validation
        when(jwt.extractEmailFromTemporaryToken(token)).thenReturn(email);
        when(jwt.validateTemporaryToken(token, email)).thenReturn(true);
        when(jwt.extractPurpose(token)).thenReturn("password_setup");
        when(jwt.extractMemberId(token)).thenReturn(memberId);
        // Le membre n'existe pas encore (lance MemberNotFoundException)
        when(authenticationService.getMemberByEmail(email))
                .thenThrow(new MemberNotFoundException("Membre introuvable"));
        when(passwordService.setInitialPassword(anyString(), anyString(), any())).thenReturn(member);
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
    // Tests setPassword - Validation token errors
    // ====================================================================

    @Test
    @DisplayName("setPassword - Devrait lever exception si validateTemporaryToken échoue")
    void setPassword_ShouldThrowException_WhenValidateTemporaryTokenFails() {
        // Given
        String authHeader = "Bearer invalid-token";
        String token = "invalid-token";

        when(jwt.extractEmailFromTemporaryToken(token)).thenReturn(email);
        when(jwt.validateTemporaryToken(token, email)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> passwordManagementDelegate.setPassword(authHeader, setPasswordRequest))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining(Messages.PASSWORD_SETUP_ERROR);

        verify(jwt).extractEmailFromTemporaryToken(token);
        verify(jwt).validateTemporaryToken(token, email);
        verifyNoInteractions(passwordService);
    }

    @Test
    @DisplayName("setPassword - Devrait lever exception si purpose ne correspond pas")
    void setPassword_ShouldThrowException_WhenPurposeDoesNotMatch() {
        // Given
        String authHeader = "Bearer token";
        String token = "token";

        when(jwt.extractEmailFromTemporaryToken(token)).thenReturn(email);
        when(jwt.validateTemporaryToken(token, email)).thenReturn(true);
        when(jwt.extractPurpose(token)).thenReturn("password_reset"); // Mauvais purpose

        // When & Then
        assertThatThrownBy(() -> passwordManagementDelegate.setPassword(authHeader, setPasswordRequest))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining(Messages.PASSWORD_SETUP_ERROR);

        verify(jwt).extractPurpose(token);
        verifyNoInteractions(passwordService);
    }

    @Test
    @DisplayName("setPassword - Devrait lever exception si membre déjà enabled")
    void setPassword_ShouldThrowException_WhenMemberAlreadyEnabled() {
        // Given
        String authHeader = "Bearer token";
        String token = "token";
        Member enabledMember = member.toBuilder().enabled(true).build();

        when(jwt.extractEmailFromTemporaryToken(token)).thenReturn(email);
        when(jwt.validateTemporaryToken(token, email)).thenReturn(true);
        when(jwt.extractPurpose(token)).thenReturn("password_setup");
        when(jwt.extractMemberId(token)).thenReturn(memberId);
        // Le membre existe déjà dans la DB avec enabled=true
        when(authenticationService.getMemberByEmail(email)).thenReturn(enabledMember);

        // When & Then
        assertThatThrownBy(() -> passwordManagementDelegate.setPassword(authHeader, setPasswordRequest))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining(Messages.PASSWORD_SETUP_ERROR);

        verifyNoInteractions(passwordService);
    }

    // ====================================================================
    // Tests resetPassword - Cas de succès
    // ====================================================================

    @Test
    @DisplayName("resetPassword - Devrait valider token password_reset et appeler getMemberByEmail")
    void resetPassword_ShouldValidatePasswordResetTokenAndCallGetMemberByEmail() {
        // Given
        String authHeader = "Bearer reset-token";
        String token = "reset-token";
        SetPasswordRequest resetRequest = new SetPasswordRequest();
        resetRequest.setPassword("NewResetPassword123!");

        // Mock JWT validation pour password_reset
        when(jwt.extractEmailFromTemporaryToken(token)).thenReturn(email);
        when(jwt.validateTemporaryToken(token, email)).thenReturn(true);
        when(jwt.extractPurpose(token)).thenReturn("password_reset");
        when(authenticationService.getMemberByEmail(email)).thenReturn(member);
        when(jwt.isTokenValidForPasswordUpdate(token, member)).thenReturn(true);
        when(passwordService.resetPasswordWithToken(email, resetRequest.getPassword())).thenReturn(member);
        when(jwt.generateUserTokens(member)).thenReturn(userTokens);
        when(openApiModelMapper.createPasswordManagementResponse(anyString(), any(), anyLong()))
                .thenReturn(passwordManagementResponse);

        // When
        ResponseEntity<PasswordManagementResponse> response =
                passwordManagementDelegate.resetPassword(authHeader, resetRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(authenticationService).getMemberByEmail(email);
        verify(jwt).isTokenValidForPasswordUpdate(token, member);
        verify(passwordService).resetPasswordWithToken(email, resetRequest.getPassword());
        verify(jwt, never()).extractMemberId(anyString()); // Ne pas extraire memberId pour reset
    }

    @Test
    @DisplayName("resetPassword - Devrait lever exception si isTokenValidForPasswordUpdate échoue")
    void resetPassword_ShouldThrowException_WhenTokenValidForPasswordUpdateFails() {
        // Given
        String authHeader = "Bearer reset-token";
        String token = "reset-token";
        SetPasswordRequest resetRequest = new SetPasswordRequest();
        resetRequest.setPassword("NewPassword!");

        when(jwt.extractEmailFromTemporaryToken(token)).thenReturn(email);
        when(jwt.validateTemporaryToken(token, email)).thenReturn(true);
        when(jwt.extractPurpose(token)).thenReturn("password_reset");
        when(authenticationService.getMemberByEmail(email)).thenReturn(member);
        when(jwt.isTokenValidForPasswordUpdate(token, member)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> passwordManagementDelegate.resetPassword(authHeader, resetRequest))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining(Messages.PASSWORD_SETUP_ERROR);

        verify(jwt).isTokenValidForPasswordUpdate(token, member);
        verify(passwordService, never()).resetPasswordWithToken(anyString(), anyString());
    }

    @Test
    @DisplayName("setPassword - Devrait propager exception générique lors de validation")
    void setPassword_ShouldThrowInvalidRequestException_OnValidationError() {
        // Given
        String authHeader = "Bearer token";
        String token = "token";

        doThrow(new RuntimeException("JWT parsing error"))
                .when(jwt).extractEmailFromTemporaryToken(token);

        // When & Then
        assertThatThrownBy(() -> passwordManagementDelegate.setPassword(authHeader, setPasswordRequest))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining(Messages.PASSWORD_SETUP_ERROR);
        
        verify(jwt).extractEmailFromTemporaryToken(token);
        verifyNoInteractions(passwordService);
    }

    // ====================================================================
    // Tests resetPassword - Gestion erreurs
    // ====================================================================

    @Test
    @DisplayName("resetPassword - Devrait lever exception si member n'existe pas pour password_reset")
    void resetPassword_ShouldThrowException_WhenMemberNotFoundForReset() {
        // Given
        String authHeader = "Bearer reset-token";
        String token = "reset-token";
        SetPasswordRequest resetRequest = new SetPasswordRequest();
        resetRequest.setPassword("NewPassword!");

        when(jwt.extractEmailFromTemporaryToken(token)).thenReturn(email);
        when(jwt.validateTemporaryToken(token, email)).thenReturn(true);
        when(jwt.extractPurpose(token)).thenReturn("password_reset");
        when(authenticationService.getMemberByEmail(email)).thenReturn(null);

        // When & Then - Le membre n'existe pas, l'exception est levée
        assertThatThrownBy(() -> passwordManagementDelegate.resetPassword(authHeader, resetRequest))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining(Messages.PASSWORD_SETUP_ERROR);

        verify(authenticationService).getMemberByEmail(email);
        verify(passwordService, never()).resetPasswordWithToken(anyString(), anyString());
    }

    // ====================================================================
    // Tests changePassword - Cas de succès
    // ====================================================================


    // ====================================================================
    // Tests changePassword - Cas de succès
    // ====================================================================

    @Test
    @DisplayName("changePassword - Devrait retourner 200 OK en cas de succès")
    void changePassword_ShouldReturnOk_WhenSuccess() {
        // Given
        String authHeader = "Bearer access-token";
        when(httpServletRequest.getHeader("Authorization")).thenReturn(authHeader);
        when(jwt.extractEmail("access-token")).thenReturn(email);
        when(authenticationService.getMemberByEmail(email)).thenReturn(member);
        when(jwt.isTokenValidForPasswordUpdate("access-token", member)).thenReturn(true);
        when(passwordService.changePassword(
                email,
                changePasswordRequest.getCurrentPassword(),
                changePasswordRequest.getNewPassword()
        )).thenReturn(member);
        when(jwt.generateUserTokens(member)).thenReturn(userTokens);
        when(openApiModelMapper.createPasswordManagementResponse(anyString(), any(), anyLong()))
                .thenReturn(passwordManagementResponse);

        // When
        ResponseEntity<PasswordManagementResponse> response = passwordManagementDelegate.changePassword(changePasswordRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        verify(passwordService).changePassword(
                email,
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
        request.setCurrentPassword(currentPwd);
        request.setNewPassword(newPwd);

        String authHeader = "Bearer token";
        when(httpServletRequest.getHeader("Authorization")).thenReturn(authHeader);
        when(jwt.extractEmail("token")).thenReturn(email);
        when(authenticationService.getMemberByEmail(email)).thenReturn(member);
        when(jwt.isTokenValidForPasswordUpdate("token", member)).thenReturn(true);
        when(passwordService.changePassword(email, currentPwd, newPwd)).thenReturn(member);
        when(jwt.generateUserTokens(member)).thenReturn(userTokens);
        when(openApiModelMapper.createPasswordManagementResponse(anyString(), any(), anyLong()))
                .thenReturn(passwordManagementResponse);

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
        String authHeader = "Bearer token";
        when(httpServletRequest.getHeader("Authorization")).thenReturn(authHeader);
        when(jwt.extractEmail("token")).thenReturn(email);
        when(authenticationService.getMemberByEmail(email)).thenReturn(member);
        when(jwt.isTokenValidForPasswordUpdate("token", member)).thenReturn(true);
        doThrow(new RuntimeException("Mot de passe actuel incorrect"))
                .when(passwordService).changePassword(
                        email,
                        changePasswordRequest.getCurrentPassword(),
                        changePasswordRequest.getNewPassword()
                );

        // When & Then
        assertThatThrownBy(() -> passwordManagementDelegate.changePassword(changePasswordRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Mot de passe actuel incorrect");

        verify(passwordService).changePassword(
                email,
                changePasswordRequest.getCurrentPassword(),
                changePasswordRequest.getNewPassword()
        );
    }

    @Test
    @DisplayName("changePassword - Devrait propager exception si utilisateur non trouvé")
    void changePassword_ShouldThrowException_WhenUserNotFound() {
        // Given
        String authHeader = "Bearer token";
        when(httpServletRequest.getHeader("Authorization")).thenReturn(authHeader);
        when(jwt.extractEmail("token")).thenReturn(email);
        when(authenticationService.getMemberByEmail(email)).thenReturn(member);
        when(jwt.isTokenValidForPasswordUpdate("token", member)).thenReturn(true);
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
        String authHeader = "Bearer token";
        when(httpServletRequest.getHeader("Authorization")).thenReturn(authHeader);
        when(jwt.extractEmail("token")).thenReturn(email);
        when(authenticationService.getMemberByEmail(email)).thenReturn(member);
        when(jwt.isTokenValidForPasswordUpdate("token", member)).thenReturn(true);
        doThrow(new RuntimeException("Erreur interne du service"))
                .when(passwordService).changePassword(anyString(), anyString(), anyString());

        // When & Then
        assertThatThrownBy(() -> passwordManagementDelegate.changePassword(changePasswordRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Erreur interne du service");
    }

    // ====================================================================
    // Tests requestPasswordReset
    // ====================================================================

    @Test
    @DisplayName("requestPasswordReset - Devrait retourner message générique si membre existe")
    void requestPasswordReset_ShouldReturnGenericMessage_WhenMemberExists() {
        // Given
        com.ecclesiaflow.springsecurity.web.model.ForgotPasswordRequest forgotRequest = 
            new com.ecclesiaflow.springsecurity.web.model.ForgotPasswordRequest();
        forgotRequest.setEmail(email);
        when(passwordService.requestPasswordReset(email)).thenReturn(java.util.Optional.of(member));

        // When
        ResponseEntity<com.ecclesiaflow.springsecurity.web.model.ForgotPasswordResponse> response = 
            passwordManagementDelegate.requestPasswordReset(forgotRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Un lien de réinitialisation a été envoyé.");
        verify(passwordService).requestPasswordReset(email);
    }

    @Test
    @DisplayName("requestPasswordReset - Devrait retourner message générique si membre n'existe pas")
    void requestPasswordReset_ShouldReturnGenericMessage_WhenMemberNotExists() {
        // Given
        com.ecclesiaflow.springsecurity.web.model.ForgotPasswordRequest forgotRequest = 
            new com.ecclesiaflow.springsecurity.web.model.ForgotPasswordRequest();
        forgotRequest.setEmail("unknown@example.com");
        when(passwordService.requestPasswordReset("unknown@example.com")).thenReturn(java.util.Optional.empty());

        // When
        ResponseEntity<com.ecclesiaflow.springsecurity.web.model.ForgotPasswordResponse> response = 
            passwordManagementDelegate.requestPasswordReset(forgotRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Un lien de réinitialisation a été envoyé.");
        verify(passwordService).requestPasswordReset("unknown@example.com");
    }

    // ====================================================================
    // Tests resetPassword
    // ====================================================================

    @Test
    @DisplayName("resetPassword - Devrait réinitialiser mot de passe et retourner tokens")
    void resetPassword_ShouldResetPasswordAndReturnTokens() {
        // Given
        String authHeader = "Bearer reset-token-123";
        String token = "reset-token-123";
        Member enabledMember = member.toBuilder().enabled(true).build();

        // Mock JWT validation pour password_reset
        when(jwt.extractEmailFromTemporaryToken(token)).thenReturn(email);
        when(jwt.validateTemporaryToken(token, email)).thenReturn(true);
        when(jwt.extractPurpose(token)).thenReturn("password_reset");
        when(authenticationService.getMemberByEmail(email)).thenReturn(enabledMember);
        when(jwt.isTokenValidForPasswordUpdate(token, enabledMember)).thenReturn(true);

        // Mock service calls
        when(passwordService.resetPasswordWithToken(email, setPasswordRequest.getPassword()))
                .thenReturn(enabledMember);
        when(jwt.generateUserTokens(enabledMember)).thenReturn(userTokens);
        when(openApiModelMapper.createPasswordManagementResponse(
                anyString(),
                eq(userTokens),
                anyLong()
        )).thenReturn(passwordManagementResponse);

        // When
        ResponseEntity<PasswordManagementResponse> response = passwordManagementDelegate.resetPassword(
                authHeader, setPasswordRequest
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getAccessToken()).isEqualTo("access-token-123");
        verify(passwordService).resetPasswordWithToken(email, setPasswordRequest.getPassword());
        verify(jwt).generateUserTokens(enabledMember);
    }


    @Test
    @DisplayName("resetPassword - Devrait lever exception si token invalide")
    void resetPassword_ShouldThrowException_WhenTokenInvalid() {
        // Given
        String authHeader = "Bearer invalid-reset-token";
        String token = "invalid-reset-token";

        when(jwt.extractEmailFromTemporaryToken(token)).thenReturn(email);
        when(jwt.validateTemporaryToken(token, email)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> passwordManagementDelegate.resetPassword(authHeader, setPasswordRequest))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining(Messages.PASSWORD_SETUP_ERROR);
        
        verify(passwordService, never()).resetPasswordWithToken(anyString(), anyString());
    }

    @Test
    @DisplayName("resetPassword - Devrait lever exception si member n'existe pas")
    void resetPassword_ShouldThrowException_WhenMemberNotFound() {
        // Given
        String authHeader = "Bearer reset-token-123";
        String token = "reset-token-123";

        when(jwt.extractEmailFromTemporaryToken(token)).thenReturn(email);
        when(jwt.validateTemporaryToken(token, email)).thenReturn(true);
        when(jwt.extractPurpose(token)).thenReturn("password_reset");
        when(authenticationService.getMemberByEmail(email)).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> passwordManagementDelegate.resetPassword(authHeader, setPasswordRequest))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Erreur lors de la définition du mot de passe");
        
        verify(passwordService, never()).resetPasswordWithToken(anyString(), anyString());
    }

    @Test
    @DisplayName("resetPassword - Devrait lever exception si purpose incorrect")
    void resetPassword_ShouldThrowException_WhenPurposeIncorrect() {
        // Given
        String authHeader = "Bearer setup-token-123";
        String token = "setup-token-123";

        when(jwt.extractEmailFromTemporaryToken(token)).thenReturn(email);
        when(jwt.validateTemporaryToken(token, email)).thenReturn(true);
        when(jwt.extractPurpose(token)).thenReturn("password_setup"); // Wrong purpose

        // When & Then
        assertThatThrownBy(() -> passwordManagementDelegate.resetPassword(authHeader, setPasswordRequest))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining(Messages.PASSWORD_SETUP_ERROR);
        
        verify(passwordService, never()).resetPasswordWithToken(anyString(), anyString());
    }

    // ====================================================================
    // Tests validatePasswordSetupToken (indirect via setPassword)
    // ====================================================================

    @Test
    @DisplayName("validatePasswordSetupToken - Devrait accepter membre existant non activé")
    void validatePasswordSetupToken_ShouldAcceptExistingDisabledMember() {
        // Given
        String authHeader = "Bearer setup-token";
        String token = "setup-token";
        Member disabledMember = member.toBuilder().enabled(false).build();

        when(jwt.extractEmailFromTemporaryToken(token)).thenReturn(email);
        when(jwt.validateTemporaryToken(token, email)).thenReturn(true);
        when(jwt.extractPurpose(token)).thenReturn("password_setup");
        when(jwt.extractMemberId(token)).thenReturn(memberId);
        when(authenticationService.getMemberByEmail(email)).thenReturn(disabledMember);
        when(passwordService.setInitialPassword(anyString(), anyString(), any())).thenReturn(member);
        when(jwt.generateUserTokens(any())).thenReturn(userTokens);
        when(openApiModelMapper.createPasswordManagementResponse(anyString(), any(), anyLong()))
                .thenReturn(passwordManagementResponse);

        // When
        ResponseEntity<PasswordManagementResponse> response = passwordManagementDelegate.setPassword(
                authHeader, setPasswordRequest
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(passwordService).setInitialPassword(email, setPasswordRequest.getPassword(), memberId);
    }

    @Test
    @DisplayName("validatePasswordSetupToken - Devrait lever exception si membre déjà activé")
    void validatePasswordSetupToken_ShouldThrowException_WhenMemberAlreadyEnabled() {
        // Given
        String authHeader = "Bearer setup-token";
        String token = "setup-token";
        Member enabledMember = member.toBuilder().enabled(true).build();

        when(jwt.extractEmailFromTemporaryToken(token)).thenReturn(email);
        when(jwt.validateTemporaryToken(token, email)).thenReturn(true);
        when(jwt.extractPurpose(token)).thenReturn("password_setup");
        when(jwt.extractMemberId(token)).thenReturn(memberId);
        when(authenticationService.getMemberByEmail(email)).thenReturn(enabledMember);

        // When & Then
        assertThatThrownBy(() -> passwordManagementDelegate.setPassword(authHeader, setPasswordRequest))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Erreur lors de la définition du mot de passe");
        
        verify(passwordService, never()).setInitialPassword(anyString(), anyString(), any());
    }

    // ====================================================================
    // Tests changePassword(ChangePasswordRequest) - Branches manquantes
    // ====================================================================

    @Test
    @DisplayName("changePassword(ChangePasswordRequest) - Devrait changer mot de passe et retourner tokens")
    void changePassword_WithRequest_ShouldChangePasswordAndReturnTokens() {
        // Given
        String authHeader = "Bearer access-token-123";
        Member enabledMember = member.toBuilder().enabled(true).build();

        when(httpServletRequest.getHeader("Authorization")).thenReturn(authHeader);
        when(jwt.extractEmail("access-token-123")).thenReturn(email);
        when(authenticationService.getMemberByEmail(email)).thenReturn(enabledMember);
        when(jwt.isTokenValidForPasswordUpdate("access-token-123", enabledMember)).thenReturn(true);
        when(passwordService.changePassword(email, "OldPassword123!", "NewPassword456!"))
                .thenReturn(enabledMember);
        when(jwt.generateUserTokens(enabledMember)).thenReturn(userTokens);
        when(openApiModelMapper.createPasswordManagementResponse(
                anyString(),
                eq(userTokens),
                anyLong()
        )).thenReturn(passwordManagementResponse);

        // When
        ResponseEntity<PasswordManagementResponse> response = 
            passwordManagementDelegate.changePassword(changePasswordRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getAccessToken()).isEqualTo("access-token-123");
        verify(passwordService).changePassword(email, "OldPassword123!", "NewPassword456!");
    }

    @Test
    @DisplayName("changePassword(ChangePasswordRequest) - Devrait lever exception si extraction email échoue")
    void changePassword_WithRequest_ShouldThrowException_WhenEmailExtractionFails() {
        // Given
        String authHeader = "Bearer invalid-token";
        
        when(httpServletRequest.getHeader("Authorization")).thenReturn(authHeader);
        when(jwt.extractEmail("invalid-token")).thenThrow(new RuntimeException("Invalid token"));

        // When & Then
        assertThatThrownBy(() -> passwordManagementDelegate.changePassword(changePasswordRequest))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Authentification requise. Veuillez vous reconnecter.");
        
        verify(passwordService, never()).changePassword(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("changePassword(ChangePasswordRequest) - Devrait lever exception si token invalide pour password update")
    void changePassword_WithRequest_ShouldThrowException_WhenTokenInvalidForPasswordUpdate() {
        // Given
        String authHeader = "Bearer access-token-123";
        Member enabledMember = member.toBuilder().enabled(true).build();

        when(httpServletRequest.getHeader("Authorization")).thenReturn(authHeader);
        when(jwt.extractEmail("access-token-123")).thenReturn(email);
        when(authenticationService.getMemberByEmail(email)).thenReturn(enabledMember);
        when(jwt.isTokenValidForPasswordUpdate("access-token-123", enabledMember)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> passwordManagementDelegate.changePassword(changePasswordRequest))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("session");
        
        verify(passwordService, never()).changePassword(anyString(), anyString(), anyString());
    }

    // Key fixes for your test failures:

    // 1. In setPassword tests, you need to handle the case where getMemberByEmail
//    throws MemberNotFoundException (not returns null)
    @Test
    @DisplayName("setPassword - Devrait définir le mot de passe et retourner les tokens (password_setup)")
    void setPassword_ShouldSetPasswordAndReturnTokens() {
        // Given
        String authHeader = "Bearer temp-token-123";
        String token = "temp-token-123";
        Member updatedMember = member.toBuilder().enabled(true).build();

        // Mock JWT validation pour password_setup
        when(jwt.extractEmailFromTemporaryToken(token)).thenReturn(email);
        when(jwt.validateTemporaryToken(token, email)).thenReturn(true);
        when(jwt.extractPurpose(token)).thenReturn("password_setup");
        when(jwt.extractMemberId(token)).thenReturn(memberId);

        // FIX: Le membre n'existe pas - throw MemberNotFoundException au lieu de return null
        when(authenticationService.getMemberByEmail(email))
                .thenThrow(new MemberNotFoundException("Member not found"));

        // Mock service calls
        when(passwordService.setInitialPassword(email, setPasswordRequest.getPassword(), memberId))
                .thenReturn(updatedMember);
        when(jwt.generateUserTokens(updatedMember)).thenReturn(userTokens);
        when(openApiModelMapper.createPasswordManagementResponse(
                anyString(),
                eq(userTokens),
                anyLong()
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

        // Verify key service interactions
        verify(passwordService).setInitialPassword(email, setPasswordRequest.getPassword(), memberId);
        verify(jwt).generateUserTokens(updatedMember);
    }

    // 2. Fix the token extraction test
    @Test
    @DisplayName("setPassword - Devrait extraire correctement le token du header")
    void setPassword_ShouldExtractTokenCorrectly() {
        // Given
        String authHeader = "Bearer my-temp-token";
        String token = "my-temp-token";

        // Mock JWT validation
        when(jwt.extractEmailFromTemporaryToken(token)).thenReturn(email);
        when(jwt.validateTemporaryToken(token, email)).thenReturn(true);
        when(jwt.extractPurpose(token)).thenReturn("password_setup");
        when(jwt.extractMemberId(token)).thenReturn(memberId);
        // FIX: Throw exception au lieu de return null
        when(authenticationService.getMemberByEmail(email))
                .thenThrow(new MemberNotFoundException("Not found"));
        when(passwordService.setInitialPassword(anyString(), anyString(), any())).thenReturn(member);
        when(jwt.generateUserTokens(any())).thenReturn(userTokens);
        when(openApiModelMapper.createPasswordManagementResponse(anyString(), any(), anyLong()))
                .thenReturn(passwordManagementResponse);

        // When
        ResponseEntity<PasswordManagementResponse> response = passwordManagementDelegate.setPassword(authHeader, setPasswordRequest);

        // Then - Vérifier que l'opération a réussi
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(passwordService).setInitialPassword(anyString(), anyString(), any());
    }

    // 3. Fix the expiresIn calculation test
    @Test
    @DisplayName("setPassword - Devrait calculer expiresIn correctement (division par 1000)")
    void setPassword_ShouldCalculateExpiresInCorrectly() {
        // Given
        ReflectionTestUtils.setField(passwordManagementDelegate, "accessTokenExpiration", 120000L);
        String authHeader = "Bearer token";
        String token = "token";

        // Mock JWT validation
        when(jwt.extractEmailFromTemporaryToken(token)).thenReturn(email);
        when(jwt.validateTemporaryToken(token, email)).thenReturn(true);
        when(jwt.extractPurpose(token)).thenReturn("password_setup");
        when(jwt.extractMemberId(token)).thenReturn(memberId);
        // FIX: Throw exception
        when(authenticationService.getMemberByEmail(email))
                .thenThrow(new MemberNotFoundException("Not found"));
        when(passwordService.setInitialPassword(anyString(), anyString(), any())).thenReturn(member);
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

    // 4. Fix the setInitialPassword failure test - the doThrow should happen BEFORE the method is called
    @Test
    @DisplayName("setPassword - Devrait propager exception si setInitialPassword échoue")
    void setPassword_ShouldThrowException_WhenSetInitialPasswordFails() {
        // Given
        String authHeader = "Bearer temp-token";
        String token = "temp-token";

        when(jwt.extractEmailFromTemporaryToken(token)).thenReturn(email);
        when(jwt.validateTemporaryToken(token, email)).thenReturn(true);
        when(jwt.extractPurpose(token)).thenReturn("password_setup");
        when(jwt.extractMemberId(token)).thenReturn(memberId);
        // FIX: Member not found (expected for password_setup)
        when(authenticationService.getMemberByEmail(email))
                .thenThrow(new MemberNotFoundException("Not found"));

        // FIX: Use when().thenThrow() instead of doThrow().when()
        when(passwordService.setInitialPassword(anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("Erreur lors de la définition du mot de passe"));

        // When & Then
        assertThatThrownBy(() -> passwordManagementDelegate.setPassword(authHeader, setPasswordRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Erreur lors de la définition du mot de passe");

        verify(passwordService).setInitialPassword(anyString(), anyString(), any());
    }

    // 5. Fix the resetPassword member not enabled test
    @Test
    @DisplayName("resetPassword - Devrait lever exception si membre non activé")
    void resetPassword_ShouldThrowException_WhenMemberNotEnabled() {
        // Given
        String authHeader = "Bearer reset-token-123";
        String token = "reset-token-123";
        Member disabledMember = member.toBuilder().enabled(false).build();

        when(jwt.extractEmailFromTemporaryToken(token)).thenReturn(email);
        when(jwt.validateTemporaryToken(token, email)).thenReturn(true);
        when(jwt.extractPurpose(token)).thenReturn("password_reset");
        when(authenticationService.getMemberByEmail(email)).thenReturn(disabledMember);
        // FIX: This should also return true, but the enabled check happens separately
        when(jwt.isTokenValidForPasswordUpdate(token, disabledMember)).thenReturn(true);

        // When & Then
        // FIX: Expect PASSWORD_SETUP_ERROR not PASSWORD_RESET_ERROR
        assertThatThrownBy(() -> passwordManagementDelegate.resetPassword(authHeader, setPasswordRequest))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining(Messages.PASSWORD_SETUP_ERROR);

        verify(passwordService, never()).resetPasswordWithToken(anyString(), anyString());
    }
}