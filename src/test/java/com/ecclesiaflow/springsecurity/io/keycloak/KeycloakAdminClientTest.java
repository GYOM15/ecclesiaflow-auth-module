package com.ecclesiaflow.springsecurity.io.keycloak;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KeycloakAdminClient - Unit Tests")
class KeycloakAdminClientTest {

    @Mock
    private KeycloakTokenFeignClient tokenClient;

    @Mock
    private KeycloakAdminFeignClient adminClient;

    @InjectMocks
    private KeycloakAdminClient keycloakAdminClient;

    private static final String REALM = "test-realm";
    private static final String ADMIN_CLIENT_ID = "admin-client";
    private static final String ADMIN_CLIENT_SECRET = "admin-secret";
    private static final String DIRECT_GRANT_CLIENT_ID = "ecclesiaflow-frontend";
    private static final String DIRECT_GRANT_CLIENT_SECRET = "frontend-secret";
    private static final String EMAIL = "user@test.com";
    private static final String PASSWORD = "StrongPassword123!";
    private static final String KEYCLOAK_USER_ID = "keycloak-user-123";
    private static final String ACCESS_TOKEN = "access-token-abc";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(keycloakAdminClient, "realm", REALM);
        ReflectionTestUtils.setField(keycloakAdminClient, "adminClientId", ADMIN_CLIENT_ID);
        ReflectionTestUtils.setField(keycloakAdminClient, "adminClientSecret", ADMIN_CLIENT_SECRET);
        ReflectionTestUtils.setField(keycloakAdminClient, "directGrantClientId", DIRECT_GRANT_CLIENT_ID);
        ReflectionTestUtils.setField(keycloakAdminClient, "directGrantClientSecret", DIRECT_GRANT_CLIENT_SECRET);
        ReflectionTestUtils.setField(keycloakAdminClient, "cachedAccessToken", null);
        ReflectionTestUtils.setField(keycloakAdminClient, "tokenExpiresAt", 0L);
    }

    @Nested
    @DisplayName("createUser - Success scenarios")
    class CreateUserSuccessTests {

        @Test
        @DisplayName("Should create user successfully with email verified")
        void shouldCreateUserSuccessfullyWithEmailVerified() {
            mockTokenAcquisition();
            HttpHeaders headers = new HttpHeaders();
            headers.add("Location", "http://keycloak/users/" + KEYCLOAK_USER_ID);
            ResponseEntity<Void> response = new ResponseEntity<>(headers, HttpStatus.CREATED);
            
            when(adminClient.createUser(anyString(), eq(REALM), any(KeycloakUserRepresentation.class)))
                    .thenReturn(response);

            String userId = keycloakAdminClient.createUser(EMAIL, PASSWORD, true);

            assertThat(userId).isEqualTo(KEYCLOAK_USER_ID);
            verify(adminClient).createUser(eq("Bearer " + ACCESS_TOKEN), eq(REALM), any(KeycloakUserRepresentation.class));
        }

        @Test
        @DisplayName("Should create user with correct properties")
        void shouldCreateUserWithCorrectProperties() {
            mockTokenAcquisition();
            HttpHeaders headers = new HttpHeaders();
            headers.add("Location", "http://keycloak/users/" + KEYCLOAK_USER_ID);
            ResponseEntity<Void> response = new ResponseEntity<>(headers, HttpStatus.CREATED);
            
            when(adminClient.createUser(anyString(), eq(REALM), any(KeycloakUserRepresentation.class)))
                    .thenReturn(response);

            keycloakAdminClient.createUser(EMAIL, PASSWORD, true);

            verify(adminClient).createUser(anyString(), eq(REALM), argThat(user ->
                    user.getUsername().equals(EMAIL) &&
                    user.getEmail().equals(EMAIL) &&
                    user.isEmailVerified() &&
                    user.isEnabled() &&
                    user.getCredentials().size() == 1 &&
                    user.getCredentials().get(0).getType().equals("password") &&
                    user.getCredentials().get(0).getValue().equals(PASSWORD) &&
                    !user.getCredentials().get(0).isTemporary()
            ));
        }

        @Test
        @DisplayName("Should create user with email not verified")
        void shouldCreateUserWithEmailNotVerified() {
            mockTokenAcquisition();
            HttpHeaders headers = new HttpHeaders();
            headers.add("Location", "http://keycloak/users/" + KEYCLOAK_USER_ID);
            ResponseEntity<Void> response = new ResponseEntity<>(headers, HttpStatus.CREATED);
            
            when(adminClient.createUser(anyString(), eq(REALM), any(KeycloakUserRepresentation.class)))
                    .thenReturn(response);

            keycloakAdminClient.createUser(EMAIL, PASSWORD, false);

            verify(adminClient).createUser(anyString(), eq(REALM), argThat(user ->
                    !user.isEmailVerified()
            ));
        }

        @Test
        @DisplayName("Should extract user ID from Location header")
        void shouldExtractUserIdFromLocationHeader() {
            mockTokenAcquisition();
            HttpHeaders headers = new HttpHeaders();
            headers.add("Location", "http://keycloak.example.com/admin/realms/test/users/" + KEYCLOAK_USER_ID);
            ResponseEntity<Void> response = new ResponseEntity<>(headers, HttpStatus.CREATED);
            
            when(adminClient.createUser(anyString(), eq(REALM), any(KeycloakUserRepresentation.class)))
                    .thenReturn(response);

            String userId = keycloakAdminClient.createUser(EMAIL, PASSWORD, true);

            assertThat(userId).isEqualTo(KEYCLOAK_USER_ID);
        }

        @Test
        @DisplayName("Should use deprecated method with default email verified true")
        void shouldUseDeprecatedMethodWithDefaultEmailVerified() {
            mockTokenAcquisition();
            HttpHeaders headers = new HttpHeaders();
            headers.add("Location", "http://keycloak/users/" + KEYCLOAK_USER_ID);
            ResponseEntity<Void> response = new ResponseEntity<>(headers, HttpStatus.CREATED);
            
            when(adminClient.createUser(anyString(), eq(REALM), any(KeycloakUserRepresentation.class)))
                    .thenReturn(response);

            String userId = keycloakAdminClient.createUser(EMAIL, PASSWORD);

            assertThat(userId).isEqualTo(KEYCLOAK_USER_ID);
            verify(adminClient).createUser(anyString(), eq(REALM), argThat(user ->
                    user.isEmailVerified()
            ));
        }
    }

    @Nested
    @DisplayName("createUser - Failure scenarios")
    class CreateUserFailureTests {

        @Test
        @DisplayName("Should throw KeycloakException when creation fails")
        void shouldThrowExceptionWhenCreationFails() {
            mockTokenAcquisition();
            ResponseEntity<Void> response = new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            
            when(adminClient.createUser(anyString(), eq(REALM), any(KeycloakUserRepresentation.class)))
                    .thenReturn(response);

            assertThatThrownBy(() -> keycloakAdminClient.createUser(EMAIL, PASSWORD, true))
                    .isInstanceOf(KeycloakAdminClient.KeycloakException.class)
                    .hasMessageContaining("Failed to create user in Keycloak");
        }

        @Test
        @DisplayName("Should throw KeycloakException when Location header is missing")
        void shouldThrowExceptionWhenLocationHeaderMissing() {
            mockTokenAcquisition();
            ResponseEntity<Void> response = new ResponseEntity<>(HttpStatus.CREATED);
            
            when(adminClient.createUser(anyString(), eq(REALM), any(KeycloakUserRepresentation.class)))
                    .thenReturn(response);

            assertThatThrownBy(() -> keycloakAdminClient.createUser(EMAIL, PASSWORD, true))
                    .isInstanceOf(KeycloakAdminClient.KeycloakException.class);
        }

        @Test
        @DisplayName("Should throw KeycloakException when token acquisition fails")
        void shouldThrowExceptionWhenTokenAcquisitionFails() {
            KeycloakTokenResponse emptyResponse = new KeycloakTokenResponse(
                    null, null, 0, 0, null, null);
            when(tokenClient.getToken(eq(REALM), any(MultiValueMap.class)))
                    .thenReturn(emptyResponse);

            assertThatThrownBy(() -> keycloakAdminClient.createUser(EMAIL, PASSWORD, true))
                    .isInstanceOf(KeycloakAdminClient.KeycloakException.class)
                    .hasMessageContaining("Failed to obtain admin access token");
        }
    }

    @Nested
    @DisplayName("updatePassword - Success scenarios")
    class UpdatePasswordSuccessTests {

        @Test
        @DisplayName("Should update password successfully")
        void shouldUpdatePasswordSuccessfully() {
            mockTokenAcquisition();
            String newPassword = "NewPassword456!";

            keycloakAdminClient.updatePassword(KEYCLOAK_USER_ID, newPassword);

            verify(adminClient).resetPassword(
                    eq("Bearer " + ACCESS_TOKEN),
                    eq(REALM),
                    eq(KEYCLOAK_USER_ID),
                    argThat(credential ->
                            credential.getType().equals("password") &&
                            credential.getValue().equals(newPassword) &&
                            !credential.isTemporary()
                    )
            );
        }

        @Test
        @DisplayName("Should set password as non-temporary")
        void shouldSetPasswordAsNonTemporary() {
            mockTokenAcquisition();

            keycloakAdminClient.updatePassword(KEYCLOAK_USER_ID, PASSWORD);

            verify(adminClient).resetPassword(anyString(), eq(REALM), eq(KEYCLOAK_USER_ID),
                    argThat(credential -> !credential.isTemporary()));
        }
    }

    @Nested
    @DisplayName("findUserByEmail - Success scenarios")
    class FindUserByEmailSuccessTests {

        @Test
        @DisplayName("Should find user by email successfully")
        void shouldFindUserByEmailSuccessfully() {
            mockTokenAcquisition();
            List<Map<String, Object>> users = List.of(Map.of("id", KEYCLOAK_USER_ID, "email", EMAIL));
            
            when(adminClient.findUsersByEmail(anyString(), eq(REALM), eq(EMAIL), eq(true)))
                    .thenReturn(users);

            String userId = keycloakAdminClient.findUserByEmail(EMAIL);

            assertThat(userId).isEqualTo(KEYCLOAK_USER_ID);
        }

        @Test
        @DisplayName("Should return null when user not found")
        void shouldReturnNullWhenUserNotFound() {
            mockTokenAcquisition();
            
            when(adminClient.findUsersByEmail(anyString(), eq(REALM), eq(EMAIL), eq(true)))
                    .thenReturn(List.of());

            String userId = keycloakAdminClient.findUserByEmail(EMAIL);

            assertThat(userId).isNull();
        }

        @Test
        @DisplayName("Should return null when response is null")
        void shouldReturnNullWhenResponseIsNull() {
            mockTokenAcquisition();
            
            when(adminClient.findUsersByEmail(anyString(), eq(REALM), eq(EMAIL), eq(true)))
                    .thenReturn(null);

            String userId = keycloakAdminClient.findUserByEmail(EMAIL);

            assertThat(userId).isNull();
        }

        @Test
        @DisplayName("Should return first user when multiple users found")
        void shouldReturnFirstUserWhenMultipleUsersFound() {
            mockTokenAcquisition();
            List<Map<String, Object>> users = List.of(
                    Map.of("id", KEYCLOAK_USER_ID, "email", EMAIL),
                    Map.of("id", "another-user-id", "email", EMAIL)
            );
            
            when(adminClient.findUsersByEmail(anyString(), eq(REALM), eq(EMAIL), eq(true)))
                    .thenReturn(users);

            String userId = keycloakAdminClient.findUserByEmail(EMAIL);

            assertThat(userId).isEqualTo(KEYCLOAK_USER_ID);
        }
    }

    @Nested
    @DisplayName("findUserByUsername - Success scenarios")
    class FindUserByUsernameSuccessTests {

        @Test
        @DisplayName("Should find user by username successfully")
        void shouldFindUserByUsernameSuccessfully() {
            mockTokenAcquisition();
            List<Map<String, Object>> users = List.of(Map.of("id", KEYCLOAK_USER_ID, "username", EMAIL));
            
            when(adminClient.findUsersByUsername(anyString(), eq(REALM), eq(EMAIL), eq(true)))
                    .thenReturn(users);

            String userId = keycloakAdminClient.findUserByUsername(EMAIL);

            assertThat(userId).isEqualTo(KEYCLOAK_USER_ID);
        }

        @Test
        @DisplayName("Should return null when user not found by username")
        void shouldReturnNullWhenUserNotFoundByUsername() {
            mockTokenAcquisition();
            
            when(adminClient.findUsersByUsername(anyString(), eq(REALM), eq(EMAIL), eq(true)))
                    .thenReturn(List.of());

            String userId = keycloakAdminClient.findUserByUsername(EMAIL);

            assertThat(userId).isNull();
        }
    }

    @Nested
    @DisplayName("Token caching")
    class TokenCachingTests {

        @Test
        @DisplayName("Should cache access token")
        void shouldCacheAccessToken() {
            mockTokenAcquisition();
            HttpHeaders headers = new HttpHeaders();
            headers.add("Location", "http://keycloak/users/" + KEYCLOAK_USER_ID);
            ResponseEntity<Void> response = new ResponseEntity<>(headers, HttpStatus.CREATED);
            
            when(adminClient.createUser(anyString(), eq(REALM), any(KeycloakUserRepresentation.class)))
                    .thenReturn(response);

            keycloakAdminClient.createUser(EMAIL, PASSWORD, true);
            keycloakAdminClient.createUser(EMAIL, PASSWORD, true);

            verify(tokenClient, times(1)).getToken(eq(REALM), any(MultiValueMap.class));
        }

        @Test
        @DisplayName("Should refresh token when expired")
        void shouldRefreshTokenWhenExpired() {
            KeycloakTokenResponse tokenResponse = new KeycloakTokenResponse(
                    ACCESS_TOKEN, null, 0, 0, "Bearer", null);
            when(tokenClient.getToken(eq(REALM), any(MultiValueMap.class)))
                    .thenReturn(tokenResponse);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Location", "http://keycloak/users/" + KEYCLOAK_USER_ID);
            ResponseEntity<Void> response = new ResponseEntity<>(headers, HttpStatus.CREATED);
            
            when(adminClient.createUser(anyString(), eq(REALM), any(KeycloakUserRepresentation.class)))
                    .thenReturn(response);

            keycloakAdminClient.createUser(EMAIL, PASSWORD, true);
            
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            keycloakAdminClient.createUser(EMAIL, PASSWORD, true);

            verify(tokenClient, atLeast(2)).getToken(eq(REALM), any(MultiValueMap.class));
        }

        @Test
        @DisplayName("Should use cached token before expiration")
        void shouldUseCachedTokenBeforeExpiration() {
            KeycloakTokenResponse tokenResponse = new KeycloakTokenResponse(
                    ACCESS_TOKEN, null, 3600, 0, "Bearer", null);
            when(tokenClient.getToken(eq(REALM), any(MultiValueMap.class)))
                    .thenReturn(tokenResponse);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Location", "http://keycloak/users/" + KEYCLOAK_USER_ID);
            ResponseEntity<Void> response = new ResponseEntity<>(headers, HttpStatus.CREATED);
            
            when(adminClient.createUser(anyString(), eq(REALM), any(KeycloakUserRepresentation.class)))
                    .thenReturn(response);

            keycloakAdminClient.createUser(EMAIL, PASSWORD, true);
            keycloakAdminClient.updatePassword(KEYCLOAK_USER_ID, PASSWORD);
            keycloakAdminClient.findUserByEmail(EMAIL);

            verify(tokenClient, times(1)).getToken(eq(REALM), any(MultiValueMap.class));
        }
    }

    @Nested
    @DisplayName("KeycloakException")
    class KeycloakExceptionTests {

        @Test
        @DisplayName("Should create exception with message")
        void shouldCreateExceptionWithMessage() {
            KeycloakAdminClient.KeycloakException exception = 
                    new KeycloakAdminClient.KeycloakException("Test error");

            assertThat(exception.getMessage()).isEqualTo("Test error");
        }

        @Test
        @DisplayName("Should create exception with message and cause")
        void shouldCreateExceptionWithMessageAndCause() {
            Throwable cause = new RuntimeException("Root cause");
            KeycloakAdminClient.KeycloakException exception = 
                    new KeycloakAdminClient.KeycloakException("Test error", cause);

            assertThat(exception.getMessage()).isEqualTo("Test error");
            assertThat(exception.getCause()).isEqualTo(cause);
        }
    }

    @Nested
    @DisplayName("Token acquisition - Edge cases")
    class TokenAcquisitionEdgeCasesTests {

        @Test
        @DisplayName("Should use default expiration when expires_in is zero")
        void shouldUseDefaultExpirationWhenMissing() {
            KeycloakTokenResponse tokenResponse = new KeycloakTokenResponse(
                    ACCESS_TOKEN, null, 0, 0, "Bearer", null);
            when(tokenClient.getToken(eq(REALM), any(MultiValueMap.class)))
                    .thenReturn(tokenResponse);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Location", "http://keycloak/users/" + KEYCLOAK_USER_ID);
            ResponseEntity<Void> response = new ResponseEntity<>(headers, HttpStatus.CREATED);
            
            when(adminClient.createUser(anyString(), eq(REALM), any(KeycloakUserRepresentation.class)))
                    .thenReturn(response);

            String userId = keycloakAdminClient.createUser(EMAIL, PASSWORD, true);

            assertThat(userId).isEqualTo(KEYCLOAK_USER_ID);
        }

        @Test
        @DisplayName("Should throw exception when response is null")
        void shouldThrowExceptionWhenResponseIsNull() {
            when(tokenClient.getToken(eq(REALM), any(MultiValueMap.class)))
                    .thenReturn(null);

            assertThatThrownBy(() -> keycloakAdminClient.createUser(EMAIL, PASSWORD, true))
                    .isInstanceOf(KeycloakAdminClient.KeycloakException.class)
                    .hasMessageContaining("Failed to obtain admin access token");
        }

        @Test
        @DisplayName("Should throw exception when access_token is missing")
        void shouldThrowExceptionWhenAccessTokenMissing() {
            KeycloakTokenResponse tokenResponse = new KeycloakTokenResponse(
                    null, null, 3600, 0, null, null);
            when(tokenClient.getToken(eq(REALM), any(MultiValueMap.class)))
                    .thenReturn(tokenResponse);

            assertThatThrownBy(() -> keycloakAdminClient.createUser(EMAIL, PASSWORD, true))
                    .isInstanceOf(KeycloakAdminClient.KeycloakException.class)
                    .hasMessageContaining("Failed to obtain admin access token");
        }

        @Test
        @DisplayName("Should use cached token in synchronized block when still valid")
        void shouldUseCachedTokenInSynchronizedBlock() throws Exception {
            KeycloakTokenResponse tokenResponse = new KeycloakTokenResponse(
                    ACCESS_TOKEN, null, 3600, 0, "Bearer", null);
            when(tokenClient.getToken(eq(REALM), any(MultiValueMap.class)))
                    .thenReturn(tokenResponse);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Location", "http://keycloak/users/" + KEYCLOAK_USER_ID);
            ResponseEntity<Void> response = new ResponseEntity<>(headers, HttpStatus.CREATED);
            
            when(adminClient.createUser(anyString(), eq(REALM), any(KeycloakUserRepresentation.class)))
                    .thenReturn(response);

            // First call - caches
            keycloakAdminClient.createUser(EMAIL, PASSWORD, true);
            
            // Second call immediate - should use the cache (double-check in synchronized)
            keycloakAdminClient.createUser(EMAIL, PASSWORD, true);

            // Tests that we called getToken only once
            verify(tokenClient, times(1)).getToken(eq(REALM), any(MultiValueMap.class));
        }
    }

    private void mockTokenAcquisition() {
        KeycloakTokenResponse tokenResponse = new KeycloakTokenResponse(
                ACCESS_TOKEN, null, 3600, 0, "Bearer", null);
        when(tokenClient.getToken(eq(REALM), any(MultiValueMap.class)))
                .thenReturn(tokenResponse);
    }
}
