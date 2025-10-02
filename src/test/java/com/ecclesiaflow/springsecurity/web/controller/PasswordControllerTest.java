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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("PasswordController - Tests de couverture complète")
class PasswordControllerTest {

    @Mock
    private PasswordService passwordService;

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private PasswordController passwordController;

    private SetPasswordRequest setPasswordRequest;
    private ChangePasswordRequest changePasswordRequest;
    private Member member;
    private UserTokens userTokens;
    private static final long ACCESS_TOKEN_EXPIRATION = 60000L; // 60 secondes en ms

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

        // Setup Member
        member = Member.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .build();

        // Setup UserTokens
        userTokens = new UserTokens("access-token", "refresh-token");

        // Set accessTokenExpiration via reflection
        setAccessTokenExpiration(passwordController, ACCESS_TOKEN_EXPIRATION);
    }

    // ====================================================================
    // Tests setPassword - Cas de succès
    // ====================================================================

    @Test
    @DisplayName("setPassword - Devrait retourner PasswordManagementResponse avec tokens")
    void setPassword_ShouldReturnPasswordManagementResponse_WhenSuccess() {
        String authHeader = "Bearer temporary-token-123";
        String tempToken = "temporary-token-123";
        String email = "user@example.com";

        PasswordManagementResponse expectedResponse = PasswordManagementResponse.builder()
                .message("Mot de passe défini avec succès. Vous êtes maintenant connecté.")
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .expiresIn(60L)
                .build();

        try (MockedStatic<PasswordManagementMapper> mapperMock = mockStatic(PasswordManagementMapper.class)) {
            mapperMock.when(() -> PasswordManagementMapper.toDtoWithTokens(
                    "Mot de passe défini avec succès. Vous êtes maintenant connecté.",
                    userTokens,
                    60L
            )).thenReturn(expectedResponse);

            when(authenticationService.getEmailFromValidatedTempToken(tempToken)).thenReturn(email);
            doNothing().when(passwordService).setInitialPassword(email, setPasswordRequest.getPassword());
            when(authenticationService.getMemberByEmail(email)).thenReturn(member);
            when(jwt.generateUserTokens(member)).thenReturn(userTokens);

            ResponseEntity<PasswordManagementResponse> response = passwordController.setPassword(authHeader, setPasswordRequest);

            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("access-token", response.getBody().getAccessToken());
            assertEquals("refresh-token", response.getBody().getRefreshToken());
            assertEquals(60L, response.getBody().getExpiresIn());

            verify(authenticationService).getEmailFromValidatedTempToken(tempToken);
            verify(passwordService).setInitialPassword(email, setPasswordRequest.getPassword());
            verify(authenticationService).getMemberByEmail(email);
            verify(jwt).generateUserTokens(member);
        }
    }

    @Test
    @DisplayName("setPassword - Devrait extraire correctement le token du header")
    void setPassword_ShouldExtractTokenCorrectly() {
        String authHeader = "Bearer my-temp-token";
        String extractedToken = "my-temp-token";
        String email = "test@example.com";

        PasswordManagementResponse response = PasswordManagementResponse.builder()
                .message("Success")
                .accessToken("token")
                .refreshToken("refresh")
                .expiresIn(60L)
                .build();

        try (MockedStatic<PasswordManagementMapper> mapperMock = mockStatic(PasswordManagementMapper.class)) {
            mapperMock.when(() -> PasswordManagementMapper.toDtoWithTokens(anyString(), any(), anyLong()))
                    .thenReturn(response);

            when(authenticationService.getEmailFromValidatedTempToken(extractedToken)).thenReturn(email);
            doNothing().when(passwordService).setInitialPassword(anyString(), anyString());
            when(authenticationService.getMemberByEmail(email)).thenReturn(member);
            when(jwt.generateUserTokens(member)).thenReturn(userTokens);

            passwordController.setPassword(authHeader, setPasswordRequest);

            verify(authenticationService).getEmailFromValidatedTempToken(extractedToken);
        }
    }

    @Test
    @DisplayName("setPassword - Devrait calculer expiresIn correctement (division par 1000)")
    void setPassword_ShouldCalculateExpiresInCorrectly() {
        String authHeader = "Bearer token";
        String email = "user@example.com";

        setAccessTokenExpiration(passwordController, 120000L); // 120 secondes en ms

        PasswordManagementResponse response = PasswordManagementResponse.builder()
                .message("Success")
                .accessToken("token")
                .refreshToken("refresh")
                .expiresIn(120L)
                .build();

        try (MockedStatic<PasswordManagementMapper> mapperMock = mockStatic(PasswordManagementMapper.class)) {
            mapperMock.when(() -> PasswordManagementMapper.toDtoWithTokens(
                    anyString(),
                    any(UserTokens.class),
                    eq(120L) // 120000 / 1000
            )).thenReturn(response);

            when(authenticationService.getEmailFromValidatedTempToken(anyString())).thenReturn(email);
            doNothing().when(passwordService).setInitialPassword(anyString(), anyString());
            when(authenticationService.getMemberByEmail(email)).thenReturn(member);
            when(jwt.generateUserTokens(member)).thenReturn(userTokens);

            ResponseEntity<PasswordManagementResponse> result = passwordController.setPassword(authHeader, setPasswordRequest);

            assertNotNull(result.getBody());
            assertEquals(120L, result.getBody().getExpiresIn());

            mapperMock.verify(() -> PasswordManagementMapper.toDtoWithTokens(
                    anyString(),
                    any(UserTokens.class),
                    eq(120L)
            ));
        }
    }

    // ====================================================================
    // Tests setPassword - Cas d'erreur (extractTokenFromHeader)
    // ====================================================================

    @Test
    @DisplayName("setPassword - Devrait lancer RuntimeException si header Authorization est null")
    void setPassword_ShouldThrowRuntimeException_WhenAuthHeaderIsNull() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> passwordController.setPassword(null, setPasswordRequest));

        assertEquals("Header Authorization manquant ou format invalide. Format attendu: Bearer {token}", ex.getMessage());

        verifyNoInteractions(authenticationService, passwordService, jwt);
    }

    @Test
    @DisplayName("setPassword - Devrait lancer RuntimeException si header ne commence pas par 'Bearer '")
    void setPassword_ShouldThrowRuntimeException_WhenHeaderDoesNotStartWithBearer() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> passwordController.setPassword("Basic token123", setPasswordRequest));

        assertEquals("Header Authorization manquant ou format invalide. Format attendu: Bearer {token}", ex.getMessage());

        verifyNoInteractions(authenticationService, passwordService, jwt);
    }

    @Test
    @DisplayName("setPassword - Devrait lancer RuntimeException si header est vide")
    void setPassword_ShouldThrowRuntimeException_WhenHeaderIsEmpty() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> passwordController.setPassword("", setPasswordRequest));

        assertEquals("Header Authorization manquant ou format invalide. Format attendu: Bearer {token}", ex.getMessage());
    }

    @Test
    @DisplayName("setPassword - Devrait lancer RuntimeException si header est 'Bearer' sans token")
    void setPassword_ShouldThrowRuntimeException_WhenHeaderIsBearerWithoutToken() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> passwordController.setPassword("Bearer", setPasswordRequest));

        assertEquals("Header Authorization manquant ou format invalide. Format attendu: Bearer {token}", ex.getMessage());
    }

    @Test
    @DisplayName("setPassword - Devrait accepter header 'Bearer ' avec token")
    void setPassword_ShouldAcceptBearerWithSpace() {
        String authHeader = "Bearer valid-token";
        String email = "user@example.com";

        PasswordManagementResponse response = PasswordManagementResponse.builder()
                .message("Success")
                .accessToken("token")
                .refreshToken("refresh")
                .expiresIn(60L)
                .build();

        try (MockedStatic<PasswordManagementMapper> mapperMock = mockStatic(PasswordManagementMapper.class)) {
            mapperMock.when(() -> PasswordManagementMapper.toDtoWithTokens(anyString(), any(), anyLong()))
                    .thenReturn(response);

            when(authenticationService.getEmailFromValidatedTempToken("valid-token")).thenReturn(email);
            doNothing().when(passwordService).setInitialPassword(anyString(), anyString());
            when(authenticationService.getMemberByEmail(email)).thenReturn(member);
            when(jwt.generateUserTokens(member)).thenReturn(userTokens);

            ResponseEntity<PasswordManagementResponse> result = passwordController.setPassword(authHeader, setPasswordRequest);

            assertNotNull(result);
            assertEquals(HttpStatus.OK, result.getStatusCode());
        }
    }

    // ====================================================================
    // Tests setPassword - Cas d'erreur métier
    // ====================================================================

    @Test
    @DisplayName("setPassword - Devrait propager exception si validation du token échoue")
    void setPassword_ShouldThrowException_WhenTokenValidationFails() {
        String authHeader = "Bearer invalid-token";

        when(authenticationService.getEmailFromValidatedTempToken("invalid-token"))
                .thenThrow(new RuntimeException("Token invalide"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> passwordController.setPassword(authHeader, setPasswordRequest));

        assertEquals("Token invalide", ex.getMessage());
        verify(authenticationService).getEmailFromValidatedTempToken("invalid-token");
        verifyNoInteractions(passwordService);
    }

    @Test
    @DisplayName("setPassword - Devrait propager exception si setInitialPassword échoue")
    void setPassword_ShouldThrowException_WhenSetInitialPasswordFails() {
        String authHeader = "Bearer temp-token";
        String email = "user@example.com";

        when(authenticationService.getEmailFromValidatedTempToken("temp-token")).thenReturn(email);
        doThrow(new RuntimeException("Erreur lors de la définition du mot de passe"))
                .when(passwordService).setInitialPassword(email, setPasswordRequest.getPassword());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> passwordController.setPassword(authHeader, setPasswordRequest));

        assertEquals("Erreur lors de la définition du mot de passe", ex.getMessage());
        verify(passwordService).setInitialPassword(email, setPasswordRequest.getPassword());
    }

    @Test
    @DisplayName("setPassword - Devrait propager exception si getMemberByEmail échoue")
    void setPassword_ShouldThrowException_WhenGetMemberByEmailFails() {
        String authHeader = "Bearer temp-token";
        String email = "user@example.com";

        when(authenticationService.getEmailFromValidatedTempToken("temp-token")).thenReturn(email);
        doNothing().when(passwordService).setInitialPassword(email, setPasswordRequest.getPassword());
        when(authenticationService.getMemberByEmail(email))
                .thenThrow(new RuntimeException("Membre non trouvé"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> passwordController.setPassword(authHeader, setPasswordRequest));

        assertEquals("Membre non trouvé", ex.getMessage());
        verify(authenticationService).getMemberByEmail(email);
    }

    @Test
    @DisplayName("setPassword - Devrait propager exception si generateUserTokens échoue")
    void setPassword_ShouldThrowException_WhenGenerateUserTokensFails() {
        String authHeader = "Bearer temp-token";
        String email = "user@example.com";

        when(authenticationService.getEmailFromValidatedTempToken("temp-token")).thenReturn(email);
        doNothing().when(passwordService).setInitialPassword(email, setPasswordRequest.getPassword());
        when(authenticationService.getMemberByEmail(email)).thenReturn(member);
        when(jwt.generateUserTokens(member))
                .thenThrow(new RuntimeException("Erreur génération tokens"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> passwordController.setPassword(authHeader, setPasswordRequest));

        assertEquals("Erreur génération tokens", ex.getMessage());
        verify(jwt).generateUserTokens(member);
    }

    // ====================================================================
    // Tests changePassword - Cas de succès
    // ====================================================================

    @Test
    @DisplayName("changePassword - Devrait retourner 200 OK en cas de succès")
    void changePassword_ShouldReturnOk_WhenSuccess() {
        doNothing().when(passwordService).changePassword(
                changePasswordRequest.getEmail(),
                changePasswordRequest.getCurrentPassword(),
                changePasswordRequest.getNewPassword()
        );

        ResponseEntity<Void> response = passwordController.changePassword(changePasswordRequest);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody());

        verify(passwordService).changePassword(
                changePasswordRequest.getEmail(),
                changePasswordRequest.getCurrentPassword(),
                changePasswordRequest.getNewPassword()
        );
    }

    @Test
    @DisplayName("changePassword - Devrait passer les bons paramètres au service")
    void changePassword_ShouldPassCorrectParametersToService() {
        String email = "test@ecclesiaflow.com";
        String currentPwd = "Current123!";
        String newPwd = "NewPassword456!";

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setEmail(email);
        request.setCurrentPassword(currentPwd);
        request.setNewPassword(newPwd);

        doNothing().when(passwordService).changePassword(email, currentPwd, newPwd);

        passwordController.changePassword(request);

        verify(passwordService).changePassword(email, currentPwd, newPwd);
    }

    // ====================================================================
    // Tests changePassword - Cas d'erreur
    // ====================================================================

    @Test
    @DisplayName("changePassword - Devrait propager exception si mot de passe actuel incorrect")
    void changePassword_ShouldThrowException_WhenCurrentPasswordIncorrect() {
        doThrow(new RuntimeException("Mot de passe actuel incorrect"))
                .when(passwordService).changePassword(
                        changePasswordRequest.getEmail(),
                        changePasswordRequest.getCurrentPassword(),
                        changePasswordRequest.getNewPassword()
                );

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> passwordController.changePassword(changePasswordRequest));

        assertEquals("Mot de passe actuel incorrect", ex.getMessage());
        verify(passwordService).changePassword(
                changePasswordRequest.getEmail(),
                changePasswordRequest.getCurrentPassword(),
                changePasswordRequest.getNewPassword()
        );
    }

    @Test
    @DisplayName("changePassword - Devrait propager exception si utilisateur non trouvé")
    void changePassword_ShouldThrowException_WhenUserNotFound() {
        doThrow(new RuntimeException("Utilisateur non trouvé"))
                .when(passwordService).changePassword(anyString(), anyString(), anyString());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> passwordController.changePassword(changePasswordRequest));

        assertEquals("Utilisateur non trouvé", ex.getMessage());
    }

    @Test
    @DisplayName("changePassword - Devrait propager exception générique du service")
    void changePassword_ShouldThrowException_OnServiceFailure() {
        doThrow(new RuntimeException("Erreur interne du service"))
                .when(passwordService).changePassword(anyString(), anyString(), anyString());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> passwordController.changePassword(changePasswordRequest));

        assertEquals("Erreur interne du service", ex.getMessage());
    }

    // ====================================================================
    // Méthode utilitaire
    // ====================================================================

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