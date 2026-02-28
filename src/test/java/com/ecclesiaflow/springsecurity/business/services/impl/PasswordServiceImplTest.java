package com.ecclesiaflow.springsecurity.business.services.impl;

import com.ecclesiaflow.springsecurity.business.domain.token.SetupToken;
import com.ecclesiaflow.springsecurity.business.services.SetupTokenService;
import com.ecclesiaflow.springsecurity.io.keycloak.KeycloakAdminClient;
import com.ecclesiaflow.springsecurity.business.domain.member.MembersClient;
import com.ecclesiaflow.springsecurity.business.exceptions.CompensationFailedException;
import com.ecclesiaflow.springsecurity.business.exceptions.InvalidTokenException;
import com.ecclesiaflow.springsecurity.web.constants.Messages;
import com.ecclesiaflow.springsecurity.web.exception.InvalidRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordServiceImpl - Unit Tests")
class PasswordServiceImplTest {

    @Mock
    private SetupTokenService setupTokenService;
    
    @Mock
    private KeycloakAdminClient keycloakAdminClient;
    
    @Mock
    private MembersClient membersClient;

    @InjectMocks
    private PasswordServiceImpl passwordService;

    private static final String EMAIL = "user@test.com";
    private static final String PASSWORD = "StrongPassword123!";
    private static final String SETUP_TOKEN = "setup-token-abc123";
    private static final UUID MEMBER_ID = UUID.randomUUID();
    private static final String KEYCLOAK_USER_ID = "keycloak-user-123";

    private SetupToken validToken;

    @BeforeEach
    void setUp() {
        validToken = SetupToken.builder()
                .id(UUID.randomUUID())
                .tokenHash("hashed-token")
                .email(EMAIL)
                .memberId(MEMBER_ID)
                .purpose(SetupToken.TokenPurpose.PASSWORD_SETUP)
                .status(SetupToken.TokenStatus.ISSUED)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("setupPassword - Success scenarios")
    class SetupPasswordSuccessTests {

        @Test
        @DisplayName("Should setup password successfully with valid token")
        void shouldSetupPasswordSuccessfully() {
            when(setupTokenService.validate(SETUP_TOKEN, SetupToken.TokenPurpose.PASSWORD_SETUP))
                    .thenReturn(validToken);
            when(membersClient.isEmailNotConfirmed(EMAIL)).thenReturn(false);
            when(keycloakAdminClient.createUser(EMAIL, PASSWORD, true)).thenReturn(KEYCLOAK_USER_ID);

            passwordService.setupPassword(SETUP_TOKEN, PASSWORD);

            verify(setupTokenService).validate(SETUP_TOKEN, SetupToken.TokenPurpose.PASSWORD_SETUP);
            verify(membersClient).isEmailNotConfirmed(EMAIL);
            verify(keycloakAdminClient).createUser(EMAIL, PASSWORD, true);
            verify(membersClient).notifyAccountActivated(MEMBER_ID, KEYCLOAK_USER_ID);
            verify(setupTokenService).deleteToken(validToken);
        }

        @Test
        @DisplayName("Should delete token only after all operations succeed")
        void shouldDeleteTokenOnlyAfterSuccess() {
            when(setupTokenService.validate(SETUP_TOKEN, SetupToken.TokenPurpose.PASSWORD_SETUP))
                    .thenReturn(validToken);
            when(membersClient.isEmailNotConfirmed(EMAIL)).thenReturn(false);
            when(keycloakAdminClient.createUser(EMAIL, PASSWORD, true)).thenReturn(KEYCLOAK_USER_ID);

            passwordService.setupPassword(SETUP_TOKEN, PASSWORD);

            verify(setupTokenService).deleteToken(validToken);
        }

    }

    @Nested
    @DisplayName("setupPassword - Token validation failures")
    class SetupPasswordTokenValidationTests {

        @Test
        @DisplayName("Should throw InvalidRequestException when token is invalid")
        void shouldThrowExceptionWhenTokenInvalid() {
            when(setupTokenService.validate(SETUP_TOKEN, SetupToken.TokenPurpose.PASSWORD_SETUP))
                    .thenThrow(new InvalidTokenException("Token is invalid or expired"));

            assertThatThrownBy(() -> passwordService.setupPassword(SETUP_TOKEN, PASSWORD))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessage(Messages.PASSWORD_SETUP_ERROR);

            verify(keycloakAdminClient, never()).createUser(anyString(), anyString(), anyBoolean());
            verify(membersClient, never()).notifyAccountActivated(any(), anyString());
            verify(setupTokenService, never()).deleteToken(any());
        }

        @Test
        @DisplayName("Should throw InvalidRequestException when token is expired")
        void shouldThrowExceptionWhenTokenExpired() {
            when(setupTokenService.validate(SETUP_TOKEN, SetupToken.TokenPurpose.PASSWORD_SETUP))
                    .thenThrow(new InvalidTokenException("Token is invalid or expired"));

            assertThatThrownBy(() -> passwordService.setupPassword(SETUP_TOKEN, PASSWORD))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessage(Messages.PASSWORD_SETUP_ERROR);

            verify(setupTokenService, never()).deleteToken(any());
        }
    }

    @Nested
    @DisplayName("setupPassword - Email confirmation failures")
    class SetupPasswordEmailConfirmationTests {

        @Test
        @DisplayName("Should throw InvalidRequestException when email is not confirmed")
        void shouldThrowExceptionWhenEmailNotConfirmed() {
            when(setupTokenService.validate(SETUP_TOKEN, SetupToken.TokenPurpose.PASSWORD_SETUP))
                    .thenReturn(validToken);
            when(membersClient.isEmailNotConfirmed(EMAIL)).thenReturn(true);

            assertThatThrownBy(() -> passwordService.setupPassword(SETUP_TOKEN, PASSWORD))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessage(Messages.PASSWORD_SETUP_ERROR);

            verify(keycloakAdminClient, never()).createUser(anyString(), anyString(), anyBoolean());
            verify(setupTokenService, never()).deleteToken(any());
        }

        @Test
        @DisplayName("Should not delete token when email confirmation fails")
        void shouldNotDeleteTokenWhenEmailNotConfirmed() {
            when(setupTokenService.validate(SETUP_TOKEN, SetupToken.TokenPurpose.PASSWORD_SETUP))
                    .thenReturn(validToken);
            when(membersClient.isEmailNotConfirmed(EMAIL)).thenReturn(true);

            assertThatThrownBy(() -> passwordService.setupPassword(SETUP_TOKEN, PASSWORD))
                    .isInstanceOf(InvalidRequestException.class);

            verify(setupTokenService, never()).deleteToken(validToken);
        }
    }

    @Nested
    @DisplayName("setupPassword - Keycloak user creation failures")
    class SetupPasswordKeycloakFailureTests {

        @Test
        @DisplayName("Should throw InvalidRequestException when Keycloak user creation fails")
        void shouldThrowExceptionWhenKeycloakFails() {
            when(setupTokenService.validate(SETUP_TOKEN, SetupToken.TokenPurpose.PASSWORD_SETUP))
                    .thenReturn(validToken);
            when(membersClient.isEmailNotConfirmed(EMAIL)).thenReturn(false);
            when(keycloakAdminClient.createUser(EMAIL, PASSWORD, true))
                    .thenThrow(new KeycloakAdminClient.KeycloakException("Keycloak service unavailable"));

            assertThatThrownBy(() -> passwordService.setupPassword(SETUP_TOKEN, PASSWORD))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessage(Messages.PASSWORD_SETUP_ERROR);

            verify(membersClient, never()).notifyAccountActivated(any(), anyString());
            verify(setupTokenService, never()).deleteToken(any());
        }

        @Test
        @DisplayName("Should not delete token when Keycloak creation fails")
        void shouldNotDeleteTokenWhenKeycloakFails() {
            when(setupTokenService.validate(SETUP_TOKEN, SetupToken.TokenPurpose.PASSWORD_SETUP))
                    .thenReturn(validToken);
            when(membersClient.isEmailNotConfirmed(EMAIL)).thenReturn(false);
            when(keycloakAdminClient.createUser(EMAIL, PASSWORD, true))
                    .thenThrow(new KeycloakAdminClient.KeycloakException("User already exists"));

            assertThatThrownBy(() -> passwordService.setupPassword(SETUP_TOKEN, PASSWORD))
                    .isInstanceOf(InvalidRequestException.class);

            verify(setupTokenService, never()).deleteToken(validToken);
        }

    }

    @Nested
    @DisplayName("setupPassword - Operation ordering")
    class SetupPasswordOperationOrderingTests {

        @Test
        @DisplayName("Should execute operations in correct order")
        void shouldExecuteOperationsInOrder() {
            when(setupTokenService.validate(SETUP_TOKEN, SetupToken.TokenPurpose.PASSWORD_SETUP))
                    .thenReturn(validToken);
            when(membersClient.isEmailNotConfirmed(EMAIL)).thenReturn(false);
            when(keycloakAdminClient.createUser(EMAIL, PASSWORD, true)).thenReturn(KEYCLOAK_USER_ID);

            passwordService.setupPassword(SETUP_TOKEN, PASSWORD);

            var inOrder = inOrder(setupTokenService, membersClient, keycloakAdminClient, setupTokenService);
            inOrder.verify(setupTokenService).validate(SETUP_TOKEN, SetupToken.TokenPurpose.PASSWORD_SETUP);
            inOrder.verify(membersClient).isEmailNotConfirmed(EMAIL);
            inOrder.verify(keycloakAdminClient).createUser(EMAIL, PASSWORD, true);
            inOrder.verify(membersClient).notifyAccountActivated(MEMBER_ID, KEYCLOAK_USER_ID);
            inOrder.verify(setupTokenService).deleteToken(validToken);
        }

    }

    @Nested
    @DisplayName("setupPassword - Edge cases")
    class SetupPasswordEdgeCasesTests {

        @Test
        @DisplayName("Should handle email with different casing")
        void shouldHandleEmailCasing() {
            SetupToken tokenWithUpperCaseEmail = validToken.toBuilder()
                    .email("USER@TEST.COM")
                    .build();
            
            when(setupTokenService.validate(SETUP_TOKEN, SetupToken.TokenPurpose.PASSWORD_SETUP))
                    .thenReturn(tokenWithUpperCaseEmail);
            when(membersClient.isEmailNotConfirmed("USER@TEST.COM")).thenReturn(false);
            when(keycloakAdminClient.createUser("USER@TEST.COM", PASSWORD, true)).thenReturn(KEYCLOAK_USER_ID);

            passwordService.setupPassword(SETUP_TOKEN, PASSWORD);

            verify(keycloakAdminClient).createUser("USER@TEST.COM", PASSWORD, true);
        }

        @Test
        @DisplayName("Should use email and memberId from validated token")
        void shouldUseDataFromValidatedToken() {
            when(setupTokenService.validate(SETUP_TOKEN, SetupToken.TokenPurpose.PASSWORD_SETUP))
                    .thenReturn(validToken);
            when(membersClient.isEmailNotConfirmed(EMAIL)).thenReturn(false);
            when(keycloakAdminClient.createUser(EMAIL, PASSWORD, true)).thenReturn(KEYCLOAK_USER_ID);

            passwordService.setupPassword(SETUP_TOKEN, PASSWORD);

            verify(keycloakAdminClient).createUser(EMAIL, PASSWORD, true);
            verify(membersClient).notifyAccountActivated(MEMBER_ID, KEYCLOAK_USER_ID);
        }
    }

    @Nested
    @DisplayName("setupPassword - Compensation scenarios")
    class SetupPasswordCompensationTests {

        @Test
        @DisplayName("Should delete Keycloak user when notifyAccountActivated fails")
        void shouldDeleteKeycloakUserWhenNotifyFails() {
            when(setupTokenService.validate(SETUP_TOKEN, SetupToken.TokenPurpose.PASSWORD_SETUP))
                    .thenReturn(validToken);
            when(membersClient.isEmailNotConfirmed(EMAIL)).thenReturn(false);
            when(keycloakAdminClient.createUser(EMAIL, PASSWORD, true)).thenReturn(KEYCLOAK_USER_ID);
            doThrow(new RuntimeException("gRPC unavailable"))
                    .when(membersClient).notifyAccountActivated(MEMBER_ID, KEYCLOAK_USER_ID);

            assertThatThrownBy(() -> passwordService.setupPassword(SETUP_TOKEN, PASSWORD))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessage(Messages.PASSWORD_SETUP_ERROR);

            verify(keycloakAdminClient).deleteUser(KEYCLOAK_USER_ID);
        }

        @Test
        @DisplayName("Should throw CompensationFailedException when deleteUser also fails")
        void shouldThrowCompensationFailedWhenDeleteUserFails() {
            when(setupTokenService.validate(SETUP_TOKEN, SetupToken.TokenPurpose.PASSWORD_SETUP))
                    .thenReturn(validToken);
            when(membersClient.isEmailNotConfirmed(EMAIL)).thenReturn(false);
            when(keycloakAdminClient.createUser(EMAIL, PASSWORD, true)).thenReturn(KEYCLOAK_USER_ID);
            doThrow(new RuntimeException("gRPC unavailable"))
                    .when(membersClient).notifyAccountActivated(MEMBER_ID, KEYCLOAK_USER_ID);
            doThrow(new RuntimeException("Keycloak delete failed"))
                    .when(keycloakAdminClient).deleteUser(KEYCLOAK_USER_ID);

            assertThatThrownBy(() -> passwordService.setupPassword(SETUP_TOKEN, PASSWORD))
                    .isInstanceOf(CompensationFailedException.class)
                    .hasMessageContaining("Orphaned Keycloak user: " + KEYCLOAK_USER_ID);
        }

        @Test
        @DisplayName("Should NOT call deleteUser when createUser itself fails")
        void shouldNotDeleteUserWhenCreateUserFails() {
            when(setupTokenService.validate(SETUP_TOKEN, SetupToken.TokenPurpose.PASSWORD_SETUP))
                    .thenReturn(validToken);
            when(membersClient.isEmailNotConfirmed(EMAIL)).thenReturn(false);
            when(keycloakAdminClient.createUser(EMAIL, PASSWORD, true))
                    .thenThrow(new KeycloakAdminClient.KeycloakException("User already exists"));

            assertThatThrownBy(() -> passwordService.setupPassword(SETUP_TOKEN, PASSWORD))
                    .isInstanceOf(InvalidRequestException.class);

            verify(keycloakAdminClient, never()).deleteUser(anyString());
        }

        @Test
        @DisplayName("Should NOT call deleteUser when token validation fails")
        void shouldNotDeleteUserWhenTokenValidationFails() {
            when(setupTokenService.validate(SETUP_TOKEN, SetupToken.TokenPurpose.PASSWORD_SETUP))
                    .thenThrow(new InvalidTokenException("Token expired"));

            assertThatThrownBy(() -> passwordService.setupPassword(SETUP_TOKEN, PASSWORD))
                    .isInstanceOf(InvalidRequestException.class);

            verify(keycloakAdminClient, never()).deleteUser(anyString());
        }

        @Test
        @DisplayName("Should use generic error message, not Keycloak internal message")
        void shouldUseGenericErrorMessageNotKeycloakInternal() {
            when(setupTokenService.validate(SETUP_TOKEN, SetupToken.TokenPurpose.PASSWORD_SETUP))
                    .thenReturn(validToken);
            when(membersClient.isEmailNotConfirmed(EMAIL)).thenReturn(false);
            when(keycloakAdminClient.createUser(EMAIL, PASSWORD, true))
                    .thenThrow(new KeycloakAdminClient.KeycloakException("User exists with same username"));

            assertThatThrownBy(() -> passwordService.setupPassword(SETUP_TOKEN, PASSWORD))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessage(Messages.PASSWORD_SETUP_ERROR)
                    .extracting(Throwable::getMessage)
                    .asString()
                    .doesNotContain("User exists");
        }
    }
}
