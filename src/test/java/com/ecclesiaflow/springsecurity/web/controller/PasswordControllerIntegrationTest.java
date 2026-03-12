package com.ecclesiaflow.springsecurity.web.controller;

import com.ecclesiaflow.springsecurity.business.domain.member.MembersClient;
import com.ecclesiaflow.springsecurity.business.domain.token.SetupToken;
import com.ecclesiaflow.springsecurity.io.keycloak.KeycloakAdminClient;
import com.ecclesiaflow.springsecurity.io.keycloak.KeycloakTokenResponse;
import com.ecclesiaflow.springsecurity.io.persistence.jpa.SetupTokenEntity;
import com.ecclesiaflow.springsecurity.io.persistence.repositories.SetupTokenJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for the password setup HTTP endpoint.
 * Validates request/response serialization, HTTP status codes, and security.
 */
@SpringBootTest(properties = {
        "grpc.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:controller-test",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8180/realms/test",
        "auth.token.setup.ttl-hours=24"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PasswordControllerIntegrationTest.MockDeps.class)
@DisplayName("PasswordController - Integration Tests (HTTP)")
class PasswordControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SetupTokenJpaRepository tokenJpaRepository;

    @Autowired
    private KeycloakAdminClient keycloakAdminClient;

    @Autowired
    private MembersClient membersClient;

    private static final String SETUP_ENDPOINT = "/ecclesiaflow/auth/password/setup";
    private static final String RAW_TOKEN = "http-test-token-xyz";
    private static final String EMAIL = "httptest@ecclesiaflow.com";
    private static final UUID MEMBER_ID = UUID.randomUUID();
    private static final String KEYCLOAK_USER_ID = "kc-" + UUID.randomUUID();
    private static final KeycloakTokenResponse MOCK_TOKEN_RESPONSE = new KeycloakTokenResponse(
            "access-token", "refresh-token", 300, 1800, "Bearer", "openid");

    @TestConfiguration
    static class MockDeps {

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

    private void createIssuedToken() {
        SetupTokenEntity entity = SetupTokenEntity.builder()
                .tokenHash(hashToken(RAW_TOKEN))
                .email(EMAIL)
                .memberId(MEMBER_ID)
                .purpose(SetupToken.TokenPurpose.PASSWORD_SETUP)
                .status(SetupToken.TokenStatus.ISSUED)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
        tokenJpaRepository.saveAndFlush(entity);
    }

    @Nested
    @DisplayName("POST /ecclesiaflow/auth/password/setup")
    class SetupPasswordEndpointTests {

        @Test
        @DisplayName("Should return 200 OK on successful password setup")
        void shouldReturn200OnSuccess() throws Exception {
            createIssuedToken();
            when(membersClient.isEmailNotConfirmed(EMAIL)).thenReturn(false);
            when(keycloakAdminClient.createUser(anyString(), anyString(), anyBoolean()))
                    .thenReturn(KEYCLOAK_USER_ID);
            when(keycloakAdminClient.authenticateUser(anyString(), anyString()))
                    .thenReturn(MOCK_TOKEN_RESPONSE);

            mockMvc.perform(post(SETUP_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Setup-Token", RAW_TOKEN)
                            .content("{\"password\": \"SecurePassword123!\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("Should return 400 when token is invalid")
        void shouldReturn400WhenTokenInvalid() throws Exception {
            mockMvc.perform(post(SETUP_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Setup-Token", "invalid-token")
                            .content("{\"password\": \"SecurePassword123!\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when X-Setup-Token header is missing")
        void shouldReturn400WhenTokenHeaderMissing() throws Exception {
            mockMvc.perform(post(SETUP_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"password\": \"SecurePassword123!\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when password is missing from body")
        void shouldReturn400WhenPasswordMissing() throws Exception {
            mockMvc.perform(post(SETUP_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Setup-Token", RAW_TOKEN)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should not require JWT authentication")
        void shouldNotRequireJwtAuthentication() throws Exception {
            mockMvc.perform(post(SETUP_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Setup-Token", "any-token")
                            .content("{\"password\": \"Test123!\"}"))
                    .andExpect(result -> {
                        int httpStatus = result.getResponse().getStatus();
                        if (httpStatus == 401 || httpStatus == 403) {
                            throw new AssertionError(
                                    "Password setup should be public, got: " + httpStatus);
                        }
                    });
        }
    }

    @Nested
    @DisplayName("Response format")
    class ResponseFormatTests {

        @Test
        @DisplayName("Should return JSON content type")
        void shouldReturnJsonContentType() throws Exception {
            createIssuedToken();
            when(membersClient.isEmailNotConfirmed(EMAIL)).thenReturn(false);
            when(keycloakAdminClient.createUser(anyString(), anyString(), anyBoolean()))
                    .thenReturn(KEYCLOAK_USER_ID);
            when(keycloakAdminClient.authenticateUser(anyString(), anyString()))
                    .thenReturn(MOCK_TOKEN_RESPONSE);

            mockMvc.perform(post(SETUP_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Setup-Token", RAW_TOKEN)
                            .content("{\"password\": \"SecurePassword123!\"}"))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("Error response should contain standard fields")
        void errorResponseShouldContainStandardFields() throws Exception {
            mockMvc.perform(post(SETUP_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Setup-Token", "bad-token")
                            .content("{\"password\": \"SecurePassword123!\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").exists());
        }
    }
}
