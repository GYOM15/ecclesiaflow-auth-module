package com.ecclesiaflow.springsecurity.business.services.impl;

import com.ecclesiaflow.springsecurity.business.domain.member.MembersClient;
import com.ecclesiaflow.springsecurity.business.domain.token.SetupToken;
import com.ecclesiaflow.springsecurity.io.keycloak.KeycloakAdminClient;
import com.ecclesiaflow.springsecurity.io.persistence.jpa.SetupTokenEntity;
import com.ecclesiaflow.springsecurity.io.persistence.repositories.SetupTokenJpaRepository;
import com.ecclesiaflow.springsecurity.web.exception.InvalidRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Integration test for the full password setup flow.
 * Uses real Spring context with H2 database but mocks external dependencies
 * (Keycloak, Members gRPC client).
 */
@SpringBootTest(properties = {
        "grpc.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:password-setup-test",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8180/realms/test",
        "auth.token.setup.ttl-hours=24"
})
@ActiveProfiles("test")
@Import(PasswordSetupIntegrationTest.MockExternalDependencies.class)
@DisplayName("Password Setup - Integration Tests (Full Flow)")
class PasswordSetupIntegrationTest {

    @Autowired
    private PasswordServiceImpl passwordService;

    @Autowired
    private SetupTokenJpaRepository tokenJpaRepository;

    @Autowired
    private KeycloakAdminClient keycloakAdminClient;

    @Autowired
    private MembersClient membersClient;

    private static final String RAW_TOKEN = "test-setup-token-abc123";
    private static final String EMAIL = "member@ecclesiaflow.com";
    private static final UUID MEMBER_ID = UUID.randomUUID();
    private static final String KEYCLOAK_USER_ID = "kc-user-" + UUID.randomUUID();
    private static final String PASSWORD = "SecurePassword123!";

    @TestConfiguration
    static class MockExternalDependencies {

        @Bean
        @Primary
        public KeycloakAdminClient keycloakAdminClient() {
            return mock(KeycloakAdminClient.class);
        }

        @Bean
        @Primary
        public MembersClient membersClient() {
            return mock(MembersClient.class);
        }
    }

    @BeforeEach
    void setUp() {
        tokenJpaRepository.deleteAll();
        reset(keycloakAdminClient, membersClient);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private SetupTokenEntity createIssuedToken(String rawToken, LocalDateTime expiresAt) {
        SetupTokenEntity entity = SetupTokenEntity.builder()
                .tokenHash(hashToken(rawToken))
                .email(EMAIL)
                .memberId(MEMBER_ID)
                .purpose(SetupToken.TokenPurpose.PASSWORD_SETUP)
                .status(SetupToken.TokenStatus.ISSUED)
                .expiresAt(expiresAt)
                .build();
        return tokenJpaRepository.saveAndFlush(entity);
    }

    @Nested
    @DisplayName("Successful password setup")
    class SuccessfulSetupTests {

        @Test
        @DisplayName("Should complete full setup flow: validate token, check email, create Keycloak user, notify members, delete token")
        void shouldCompleteFullSetupFlow() {
            createIssuedToken(RAW_TOKEN, LocalDateTime.now().plusHours(24));
            when(membersClient.isEmailNotConfirmed(EMAIL)).thenReturn(false);
            when(keycloakAdminClient.createUser(EMAIL, PASSWORD, true)).thenReturn(KEYCLOAK_USER_ID);

            passwordService.setupPassword(RAW_TOKEN, PASSWORD);

            verify(membersClient).isEmailNotConfirmed(EMAIL);
            verify(keycloakAdminClient).createUser(EMAIL, PASSWORD, true);
            verify(membersClient).notifyAccountActivated(MEMBER_ID, KEYCLOAK_USER_ID);
            assertThat(tokenJpaRepository.findAll()).isEmpty();
        }

        @Test
        @DisplayName("Should delete token after successful setup")
        void shouldDeleteTokenAfterSetup() {
            createIssuedToken(RAW_TOKEN, LocalDateTime.now().plusHours(24));
            when(membersClient.isEmailNotConfirmed(EMAIL)).thenReturn(false);
            when(keycloakAdminClient.createUser(anyString(), anyString(), anyBoolean()))
                    .thenReturn(KEYCLOAK_USER_ID);

            passwordService.setupPassword(RAW_TOKEN, PASSWORD);

            assertThat(tokenJpaRepository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("Failure scenarios")
    class FailureTests {

        @Test
        @DisplayName("Should fail when token does not exist")
        void shouldFailWhenTokenDoesNotExist() {
            assertThatThrownBy(() -> passwordService.setupPassword("non-existent-token", PASSWORD))
                    .isInstanceOf(InvalidRequestException.class);

            verify(keycloakAdminClient, never()).createUser(anyString(), anyString(), anyBoolean());
        }

        @Test
        @DisplayName("Should fail when token is expired")
        void shouldFailWhenTokenIsExpired() {
            createIssuedToken(RAW_TOKEN, LocalDateTime.now().minusHours(1));

            assertThatThrownBy(() -> passwordService.setupPassword(RAW_TOKEN, PASSWORD))
                    .isInstanceOf(InvalidRequestException.class);

            verify(keycloakAdminClient, never()).createUser(anyString(), anyString(), anyBoolean());
        }

        @Test
        @DisplayName("Should fail when email is not confirmed in Members module")
        void shouldFailWhenEmailNotConfirmed() {
            createIssuedToken(RAW_TOKEN, LocalDateTime.now().plusHours(24));
            when(membersClient.isEmailNotConfirmed(EMAIL)).thenReturn(true);

            assertThatThrownBy(() -> passwordService.setupPassword(RAW_TOKEN, PASSWORD))
                    .isInstanceOf(InvalidRequestException.class);

            verify(keycloakAdminClient, never()).createUser(anyString(), anyString(), anyBoolean());
        }

        @Test
        @DisplayName("Should fail when Keycloak user creation fails")
        void shouldFailWhenKeycloakCreateUserFails() {
            createIssuedToken(RAW_TOKEN, LocalDateTime.now().plusHours(24));
            when(membersClient.isEmailNotConfirmed(EMAIL)).thenReturn(false);
            when(keycloakAdminClient.createUser(anyString(), anyString(), anyBoolean()))
                    .thenThrow(new KeycloakAdminClient.KeycloakException("Keycloak down"));

            assertThatThrownBy(() -> passwordService.setupPassword(RAW_TOKEN, PASSWORD))
                    .isInstanceOf(InvalidRequestException.class);

            verify(membersClient, never()).notifyAccountActivated(any(), anyString());
        }
    }

    @Nested
    @DisplayName("Compensation mechanism")
    class CompensationTests {

        @Test
        @DisplayName("Should delete Keycloak user when Members notification fails")
        void shouldCompensateKeycloakUserWhenMembersNotificationFails() {
            createIssuedToken(RAW_TOKEN, LocalDateTime.now().plusHours(24));
            when(membersClient.isEmailNotConfirmed(EMAIL)).thenReturn(false);
            when(keycloakAdminClient.createUser(EMAIL, PASSWORD, true)).thenReturn(KEYCLOAK_USER_ID);
            doThrow(new RuntimeException("gRPC failed"))
                    .when(membersClient).notifyAccountActivated(MEMBER_ID, KEYCLOAK_USER_ID);

            assertThatThrownBy(() -> passwordService.setupPassword(RAW_TOKEN, PASSWORD))
                    .isInstanceOf(InvalidRequestException.class);

            verify(keycloakAdminClient).deleteUser(KEYCLOAK_USER_ID);
        }
    }
}
