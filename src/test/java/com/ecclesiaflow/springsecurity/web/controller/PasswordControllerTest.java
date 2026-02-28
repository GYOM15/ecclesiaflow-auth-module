package com.ecclesiaflow.springsecurity.web.controller;

import com.ecclesiaflow.springsecurity.business.domain.password.PasswordManagement;
import com.ecclesiaflow.springsecurity.web.delegate.PasswordManagementDelegate;
import com.ecclesiaflow.springsecurity.web.model.PasswordManagementResponse;
import com.ecclesiaflow.springsecurity.web.model.SetupPasswordRequest;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordController - Tests Unitaires")
class PasswordControllerTest {

    @Mock
    private PasswordManagementDelegate passwordManagementDelegate;

    @InjectMocks
    private PasswordController passwordController;

    private static final String SETUP_TOKEN = "setup-token-abc123";
    private static final String PASSWORD = "StrongPassword123!";

    private SetupPasswordRequest setupPasswordRequest;
    private PasswordManagementResponse passwordManagementResponse;

    @BeforeEach
    void setUp() {
        setupPasswordRequest = new SetupPasswordRequest();
        setupPasswordRequest.setPassword(PASSWORD);

        passwordManagementResponse = new PasswordManagementResponse()
                .message("Mot de passe défini avec succès");
    }

    @Nested
    @DisplayName("_authSetInitialPassword - Success scenarios")
    class SetInitialPasswordSuccessTests {

        @Test
        @DisplayName("Should setup initial password successfully")
        void shouldSetupInitialPasswordSuccessfully() {
            when(passwordManagementDelegate.setupPassword(SETUP_TOKEN, PASSWORD))
                    .thenReturn(ResponseEntity.ok(passwordManagementResponse));

            ResponseEntity<PasswordManagementResponse> response = 
                    passwordController._authSetInitialPassword(SETUP_TOKEN, setupPasswordRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).contains("succès");
            
            verify(passwordManagementDelegate).setupPassword(SETUP_TOKEN, PASSWORD);
        }

        @Test
        @DisplayName("Should delegate to PasswordManagementDelegate")
        void shouldDelegateToPasswordManagementDelegate() {
            when(passwordManagementDelegate.setupPassword(SETUP_TOKEN, PASSWORD))
                    .thenReturn(ResponseEntity.ok(passwordManagementResponse));

            passwordController._authSetInitialPassword(SETUP_TOKEN, setupPasswordRequest);

            verify(passwordManagementDelegate).setupPassword(SETUP_TOKEN, PASSWORD);
        }

        @Test
        @DisplayName("Should create PasswordManagement domain object")
        void shouldCreatePasswordManagementDomainObject() {
            when(passwordManagementDelegate.setupPassword(SETUP_TOKEN, PASSWORD))
                    .thenReturn(ResponseEntity.ok(passwordManagementResponse));

            passwordController._authSetInitialPassword(SETUP_TOKEN, setupPasswordRequest);

            verify(passwordManagementDelegate).setupPassword(eq(SETUP_TOKEN), eq(PASSWORD));
        }

        @Test
        @DisplayName("Should extract password from request")
        void shouldExtractPasswordFromRequest() {
            when(passwordManagementDelegate.setupPassword(SETUP_TOKEN, PASSWORD))
                    .thenReturn(ResponseEntity.ok(passwordManagementResponse));

            passwordController._authSetInitialPassword(SETUP_TOKEN, setupPasswordRequest);

            verify(passwordManagementDelegate).setupPassword(anyString(), eq(PASSWORD));
        }
    }

    @Nested
    @DisplayName("_authSetInitialPassword - Request mapping")
    class SetInitialPasswordRequestMappingTests {

        @Test
        @DisplayName("Should map SetupPasswordRequest to domain parameters")
        void shouldMapRequestToDomainParameters() {
            when(passwordManagementDelegate.setupPassword(SETUP_TOKEN, PASSWORD))
                    .thenReturn(ResponseEntity.ok(passwordManagementResponse));

            passwordController._authSetInitialPassword(SETUP_TOKEN, setupPasswordRequest);

            verify(passwordManagementDelegate).setupPassword(SETUP_TOKEN, PASSWORD);
        }

        @Test
        @DisplayName("Should pass token from header")
        void shouldPassTokenFromHeader() {
            when(passwordManagementDelegate.setupPassword(SETUP_TOKEN, PASSWORD))
                    .thenReturn(ResponseEntity.ok(passwordManagementResponse));

            passwordController._authSetInitialPassword(SETUP_TOKEN, setupPasswordRequest);

            verify(passwordManagementDelegate).setupPassword(eq(SETUP_TOKEN), anyString());
        }

        @Test
        @DisplayName("Should pass password from request body")
        void shouldPassPasswordFromRequestBody() {
            when(passwordManagementDelegate.setupPassword(SETUP_TOKEN, PASSWORD))
                    .thenReturn(ResponseEntity.ok(passwordManagementResponse));

            passwordController._authSetInitialPassword(SETUP_TOKEN, setupPasswordRequest);

            verify(passwordManagementDelegate).setupPassword(anyString(), eq(PASSWORD));
        }
    }

    @Nested
    @DisplayName("_authSetInitialPassword - Response handling")
    class SetInitialPasswordResponseTests {

        @Test
        @DisplayName("Should return response from delegate")
        void shouldReturnResponseFromDelegate() {
            ResponseEntity<PasswordManagementResponse> expectedResponse = 
                    ResponseEntity.ok(passwordManagementResponse);
            when(passwordManagementDelegate.setupPassword(SETUP_TOKEN, PASSWORD))
                    .thenReturn(expectedResponse);

            ResponseEntity<PasswordManagementResponse> actualResponse = 
                    passwordController._authSetInitialPassword(SETUP_TOKEN, setupPasswordRequest);

            assertThat(actualResponse).isEqualTo(expectedResponse);
        }

        @Test
        @DisplayName("Should propagate status code from delegate")
        void shouldPropagateStatusCodeFromDelegate() {
            when(passwordManagementDelegate.setupPassword(SETUP_TOKEN, PASSWORD))
                    .thenReturn(ResponseEntity.ok(passwordManagementResponse));

            ResponseEntity<PasswordManagementResponse> response = 
                    passwordController._authSetInitialPassword(SETUP_TOKEN, setupPasswordRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("Should propagate response body from delegate")
        void shouldPropagateResponseBodyFromDelegate() {
            when(passwordManagementDelegate.setupPassword(SETUP_TOKEN, PASSWORD))
                    .thenReturn(ResponseEntity.ok(passwordManagementResponse));

            ResponseEntity<PasswordManagementResponse> response = 
                    passwordController._authSetInitialPassword(SETUP_TOKEN, setupPasswordRequest);

            assertThat(response.getBody()).isEqualTo(passwordManagementResponse);
        }
    }

    @Nested
    @DisplayName("_authSetInitialPassword - Edge cases")
    class SetInitialPasswordEdgeCasesTests {

        @Test
        @DisplayName("Should handle different password formats")
        void shouldHandleDifferentPasswordFormats() {
            String complexPassword = "C0mpl3x!P@ssw0rd#123";
            setupPasswordRequest.setPassword(complexPassword);
            
            when(passwordManagementDelegate.setupPassword(SETUP_TOKEN, complexPassword))
                    .thenReturn(ResponseEntity.ok(passwordManagementResponse));

            ResponseEntity<PasswordManagementResponse> response = 
                    passwordController._authSetInitialPassword(SETUP_TOKEN, setupPasswordRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(passwordManagementDelegate).setupPassword(SETUP_TOKEN, complexPassword);
        }

        @Test
        @DisplayName("Should handle long tokens")
        void shouldHandleLongTokens() {
            String longToken = "a".repeat(500);
            when(passwordManagementDelegate.setupPassword(longToken, PASSWORD))
                    .thenReturn(ResponseEntity.ok(passwordManagementResponse));

            ResponseEntity<PasswordManagementResponse> response = 
                    passwordController._authSetInitialPassword(longToken, setupPasswordRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(passwordManagementDelegate).setupPassword(longToken, PASSWORD);
        }

        @Test
        @DisplayName("Should handle tokens with special characters")
        void shouldHandleTokensWithSpecialCharacters() {
            String tokenWithSpecialChars = "token-with_special.chars+123";
            when(passwordManagementDelegate.setupPassword(tokenWithSpecialChars, PASSWORD))
                    .thenReturn(ResponseEntity.ok(passwordManagementResponse));

            ResponseEntity<PasswordManagementResponse> response = 
                    passwordController._authSetInitialPassword(tokenWithSpecialChars, setupPasswordRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(passwordManagementDelegate).setupPassword(tokenWithSpecialChars, PASSWORD);
        }
    }

    @Nested
    @DisplayName("_authSetInitialPassword - Integration with delegate")
    class SetInitialPasswordIntegrationTests {

        @Test
        @DisplayName("Should call delegate exactly once")
        void shouldCallDelegateExactlyOnce() {
            when(passwordManagementDelegate.setupPassword(SETUP_TOKEN, PASSWORD))
                    .thenReturn(ResponseEntity.ok(passwordManagementResponse));

            passwordController._authSetInitialPassword(SETUP_TOKEN, setupPasswordRequest);

            verify(passwordManagementDelegate, times(1)).setupPassword(SETUP_TOKEN, PASSWORD);
        }

        @Test
        @DisplayName("Should not modify request data before delegation")
        void shouldNotModifyRequestDataBeforeDelegation() {
            when(passwordManagementDelegate.setupPassword(SETUP_TOKEN, PASSWORD))
                    .thenReturn(ResponseEntity.ok(passwordManagementResponse));

            passwordController._authSetInitialPassword(SETUP_TOKEN, setupPasswordRequest);

            verify(passwordManagementDelegate).setupPassword(SETUP_TOKEN, PASSWORD);
            assertThat(setupPasswordRequest.getPassword()).isEqualTo(PASSWORD);
        }

        @Test
        @DisplayName("Should pass through all parameters unchanged")
        void shouldPassThroughAllParametersUnchanged() {
            when(passwordManagementDelegate.setupPassword(SETUP_TOKEN, PASSWORD))
                    .thenReturn(ResponseEntity.ok(passwordManagementResponse));

            passwordController._authSetInitialPassword(SETUP_TOKEN, setupPasswordRequest);

            verify(passwordManagementDelegate).setupPassword(SETUP_TOKEN, PASSWORD);
        }
    }
}
