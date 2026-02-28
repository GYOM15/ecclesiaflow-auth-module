package com.ecclesiaflow.springsecurity.web.delegate;

import com.ecclesiaflow.springsecurity.business.services.PasswordService;
import com.ecclesiaflow.springsecurity.web.constants.Messages;
import com.ecclesiaflow.springsecurity.web.exception.InvalidRequestException;
import com.ecclesiaflow.springsecurity.web.model.PasswordManagementResponse;
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

    @Nested
    @DisplayName("setupPassword - Success scenarios")
    class SetupPasswordSuccessTests {

        @Test
        @DisplayName("Should setup password successfully and return 200 OK")
        void shouldSetupPasswordSuccessfully() {
            doNothing().when(passwordService).setupPassword(SETUP_TOKEN, PASSWORD);

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
            doNothing().when(passwordService).setupPassword(SETUP_TOKEN, PASSWORD);

            passwordManagementDelegate.setupPassword(SETUP_TOKEN, PASSWORD);

            verify(passwordService).setupPassword(SETUP_TOKEN, PASSWORD);
        }

        @Test
        @DisplayName("Should return response with success message")
        void shouldReturnResponseWithSuccessMessage() {
            doNothing().when(passwordService).setupPassword(SETUP_TOKEN, PASSWORD);

            ResponseEntity<PasswordManagementResponse> response = 
                    passwordManagementDelegate.setupPassword(SETUP_TOKEN, PASSWORD);

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).contains("succès");
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
            doNothing().when(passwordService).setupPassword(longToken, PASSWORD);

            ResponseEntity<PasswordManagementResponse> response = 
                    passwordManagementDelegate.setupPassword(longToken, PASSWORD);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(passwordService).setupPassword(longToken, PASSWORD);
        }

        @Test
        @DisplayName("Should handle special characters in token")
        void shouldHandleSpecialCharactersInToken() {
            String tokenWithSpecialChars = "token-with_special.chars+123";
            doNothing().when(passwordService).setupPassword(tokenWithSpecialChars, PASSWORD);

            ResponseEntity<PasswordManagementResponse> response = 
                    passwordManagementDelegate.setupPassword(tokenWithSpecialChars, PASSWORD);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(passwordService).setupPassword(tokenWithSpecialChars, PASSWORD);
        }

        @Test
        @DisplayName("Should handle complex password")
        void shouldHandleComplexPassword() {
            String complexPassword = "C0mpl3x!P@ssw0rd#With$Special%Chars";
            doNothing().when(passwordService).setupPassword(SETUP_TOKEN, complexPassword);

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
            doNothing().when(passwordService).setupPassword(SETUP_TOKEN, PASSWORD);

            ResponseEntity<PasswordManagementResponse> response = 
                    passwordManagementDelegate.setupPassword(SETUP_TOKEN, PASSWORD);

            assertThat(response.getBody()).isNotNull();
        }

        @Test
        @DisplayName("Should return response with message field")
        void shouldReturnResponseWithMessageField() {
            doNothing().when(passwordService).setupPassword(SETUP_TOKEN, PASSWORD);

            ResponseEntity<PasswordManagementResponse> response = 
                    passwordManagementDelegate.setupPassword(SETUP_TOKEN, PASSWORD);

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isNotNull();
            assertThat(response.getBody().getMessage()).isNotEmpty();
        }

        @Test
        @DisplayName("Should return 200 OK status code")
        void shouldReturn200StatusCode() {
            doNothing().when(passwordService).setupPassword(SETUP_TOKEN, PASSWORD);

            ResponseEntity<PasswordManagementResponse> response = 
                    passwordManagementDelegate.setupPassword(SETUP_TOKEN, PASSWORD);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getStatusCode().value()).isEqualTo(200);
        }
    }
}
