package com.ecclesiaflow.springsecurity.web.delegate;

import com.ecclesiaflow.springsecurity.business.domain.token.UserTokens;
import com.ecclesiaflow.springsecurity.business.services.PasswordService;
import com.ecclesiaflow.springsecurity.web.constants.Messages;
import com.ecclesiaflow.springsecurity.web.exception.InvalidRequestException;
import com.ecclesiaflow.springsecurity.web.model.PasswordManagementResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordManagementDelegate - Tests Unitaires")
class PasswordManagementDelegateTest {

    @Mock
    private PasswordService passwordService;

    @InjectMocks
    private PasswordManagementDelegate passwordManagementDelegate;

    private static final String SETUP_TOKEN = "setup-token-abc123";
    private static final String PASSWORD = "StrongPassword123!";
    private static final UserTokens MOCK_TOKENS = new UserTokens("access-token", "refresh-token", 300);

    @Nested
    @DisplayName("setupPassword - Success scenarios")
    class SetupPasswordSuccessTests {

        @Test
        @DisplayName("Should setup password successfully and return 200 OK")
        void shouldSetupPasswordSuccessfully() {
            when(passwordService.setupPassword(SETUP_TOKEN, PASSWORD)).thenReturn(Optional.of(MOCK_TOKENS));

            ResponseEntity<PasswordManagementResponse> response = 
                    passwordManagementDelegate.setupPassword(SETUP_TOKEN, PASSWORD);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo(Messages.PASSWORD_SETUP_SUCCESS);
            
            verify(passwordService).setupPassword(SETUP_TOKEN, PASSWORD);
        }

        @Test
        @DisplayName("Should call passwordService with correct parameters")
        void shouldCallPasswordServiceWithCorrectParameters() {
            when(passwordService.setupPassword(SETUP_TOKEN, PASSWORD)).thenReturn(Optional.of(MOCK_TOKENS));

            passwordManagementDelegate.setupPassword(SETUP_TOKEN, PASSWORD);

            verify(passwordService).setupPassword(SETUP_TOKEN, PASSWORD);
        }

        @Test
        @DisplayName("Should return response with success message")
        void shouldReturnResponseWithSuccessMessage() {
            when(passwordService.setupPassword(SETUP_TOKEN, PASSWORD)).thenReturn(Optional.of(MOCK_TOKENS));

            ResponseEntity<PasswordManagementResponse> response = 
                    passwordManagementDelegate.setupPassword(SETUP_TOKEN, PASSWORD);

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).contains("successfully");
        }
    }

    @Nested
    @DisplayName("setupPassword - Direct Grant fallback")
    class SetupPasswordDirectGrantFallbackTests {

        @Test
        @DisplayName("Should return 200 OK with success message even when Direct Grant fails")
        void shouldReturn200EvenWhenDirectGrantFails() {
            when(passwordService.setupPassword(SETUP_TOKEN, PASSWORD)).thenReturn(Optional.empty());

            ResponseEntity<PasswordManagementResponse> response =
                    passwordManagementDelegate.setupPassword(SETUP_TOKEN, PASSWORD);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo(Messages.PASSWORD_SETUP_SUCCESS);
            assertThat(response.getBody().getAccessToken()).isNull();
        }
    }

    @Nested
    @DisplayName("setupPassword - Validation failures")
    class SetupPasswordValidationTests {

        @Test
        @DisplayName("Should throw InvalidRequestException when token is null")
        void shouldThrowExceptionWhenTokenIsNull() {
            assertThatThrownBy(() -> passwordManagementDelegate.setupPassword(null, PASSWORD))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("Setup token is required");

            verify(passwordService, never()).setupPassword(anyString(), anyString());
        }

        @Test
        @DisplayName("Should throw InvalidRequestException when token is blank")
        void shouldThrowExceptionWhenTokenIsBlank() {
            assertThatThrownBy(() -> passwordManagementDelegate.setupPassword("", PASSWORD))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("Setup token is required");

            verify(passwordService, never()).setupPassword(anyString(), anyString());
        }

        @Test
        @DisplayName("Should throw InvalidRequestException when token is whitespace")
        void shouldThrowExceptionWhenTokenIsWhitespace() {
            assertThatThrownBy(() -> passwordManagementDelegate.setupPassword("   ", PASSWORD))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("Setup token is required");

            verify(passwordService, never()).setupPassword(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("setupPassword - Service failures")
    class SetupPasswordServiceFailureTests {

        @Test
        @DisplayName("Should propagate InvalidRequestException from service")
        void shouldPropagateInvalidRequestException() {
            doThrow(new InvalidRequestException("Token is invalid or expired"))
                    .when(passwordService).setupPassword(SETUP_TOKEN, PASSWORD);

            assertThatThrownBy(() -> passwordManagementDelegate.setupPassword(SETUP_TOKEN, PASSWORD))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("Token is invalid or expired");
        }

        @Test
        @DisplayName("Should propagate exception when email not confirmed")
        void shouldPropagateExceptionWhenEmailNotConfirmed() {
            doThrow(new InvalidRequestException("Member email is not confirmed"))
                    .when(passwordService).setupPassword(SETUP_TOKEN, PASSWORD);

            assertThatThrownBy(() -> passwordManagementDelegate.setupPassword(SETUP_TOKEN, PASSWORD))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("Member email is not confirmed");
        }

        @Test
        @DisplayName("Should propagate exception when Keycloak fails")
        void shouldPropagateExceptionWhenKeycloakFails() {
            doThrow(new InvalidRequestException("Failed to create account"))
                    .when(passwordService).setupPassword(SETUP_TOKEN, PASSWORD);

            assertThatThrownBy(() -> passwordManagementDelegate.setupPassword(SETUP_TOKEN, PASSWORD))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("Failed to create account");
        }
    }

    @Nested
    @DisplayName("setupPassword - Edge cases")
    class SetupPasswordEdgeCasesTests {

        @Test
        @DisplayName("Should handle long token")
        void shouldHandleLongToken() {
            String longToken = "a".repeat(500);
            when(passwordService.setupPassword(longToken, PASSWORD)).thenReturn(Optional.of(MOCK_TOKENS));

            ResponseEntity<PasswordManagementResponse> response = 
                    passwordManagementDelegate.setupPassword(longToken, PASSWORD);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(passwordService).setupPassword(longToken, PASSWORD);
        }

        @Test
        @DisplayName("Should handle special characters in token")
        void shouldHandleSpecialCharactersInToken() {
            String tokenWithSpecialChars = "token-with_special.chars+123";
            when(passwordService.setupPassword(tokenWithSpecialChars, PASSWORD)).thenReturn(Optional.of(MOCK_TOKENS));

            ResponseEntity<PasswordManagementResponse> response = 
                    passwordManagementDelegate.setupPassword(tokenWithSpecialChars, PASSWORD);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(passwordService).setupPassword(tokenWithSpecialChars, PASSWORD);
        }

        @Test
        @DisplayName("Should handle complex password")
        void shouldHandleComplexPassword() {
            String complexPassword = "C0mpl3x!P@ssw0rd#With$Special%Chars";
            when(passwordService.setupPassword(SETUP_TOKEN, complexPassword)).thenReturn(Optional.of(MOCK_TOKENS));

            ResponseEntity<PasswordManagementResponse> response = 
                    passwordManagementDelegate.setupPassword(SETUP_TOKEN, complexPassword);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(passwordService).setupPassword(SETUP_TOKEN, complexPassword);
        }
    }

    @Nested
    @DisplayName("setupPassword - Response structure")
    class SetupPasswordResponseTests {

        @Test
        @DisplayName("Should return non-null response body")
        void shouldReturnNonNullResponseBody() {
            when(passwordService.setupPassword(SETUP_TOKEN, PASSWORD)).thenReturn(Optional.of(MOCK_TOKENS));

            ResponseEntity<PasswordManagementResponse> response = 
                    passwordManagementDelegate.setupPassword(SETUP_TOKEN, PASSWORD);

            assertThat(response.getBody()).isNotNull();
        }

        @Test
        @DisplayName("Should return response with message field")
        void shouldReturnResponseWithMessageField() {
            when(passwordService.setupPassword(SETUP_TOKEN, PASSWORD)).thenReturn(Optional.of(MOCK_TOKENS));

            ResponseEntity<PasswordManagementResponse> response = 
                    passwordManagementDelegate.setupPassword(SETUP_TOKEN, PASSWORD);

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isNotNull();
            assertThat(response.getBody().getMessage()).isNotEmpty();
        }

        @Test
        @DisplayName("Should return 200 OK status code")
        void shouldReturn200StatusCode() {
            when(passwordService.setupPassword(SETUP_TOKEN, PASSWORD)).thenReturn(Optional.of(MOCK_TOKENS));

            ResponseEntity<PasswordManagementResponse> response = 
                    passwordManagementDelegate.setupPassword(SETUP_TOKEN, PASSWORD);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getStatusCode().value()).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("addLocalCredentials")
    class AddLocalCredentialsTests {

        private static final String KEYCLOAK_USER_ID = "keycloak-user-123";

        @BeforeEach
        void setUpSecurityContext() {
            Jwt jwt = Jwt.withTokenValue("mock-token")
                    .header("alg", "RS256")
                    .subject(KEYCLOAK_USER_ID)
                    .claim("email", "user@test.com")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(300))
                    .build();
            JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        @AfterEach
        void clearSecurityContext() {
            SecurityContextHolder.clearContext();
        }

        @Test
        @DisplayName("Should return 201 Created on success")
        void shouldReturn201OnSuccess() {
            ResponseEntity<PasswordManagementResponse> response =
                    passwordManagementDelegate.addLocalCredentials(PASSWORD);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).contains("password added");
            verify(passwordService).addLocalCredentials(KEYCLOAK_USER_ID, PASSWORD);
        }

        @Test
        @DisplayName("Should extract keycloakUserId from JWT sub claim")
        void shouldExtractKeycloakUserIdFromJwt() {
            passwordManagementDelegate.addLocalCredentials(PASSWORD);

            verify(passwordService).addLocalCredentials(KEYCLOAK_USER_ID, PASSWORD);
        }

        @Test
        @DisplayName("Should propagate service exception")
        void shouldPropagateServiceException() {
            doThrow(new RuntimeException("Keycloak error"))
                    .when(passwordService).addLocalCredentials(KEYCLOAK_USER_ID, PASSWORD);

            assertThatThrownBy(() -> passwordManagementDelegate.addLocalCredentials(PASSWORD))
                    .isInstanceOf(RuntimeException.class);
        }
    }
}
