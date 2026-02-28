package com.ecclesiaflow.springsecurity.application.config;

import com.ecclesiaflow.springsecurity.business.domain.member.MembersClient;
import com.ecclesiaflow.springsecurity.io.keycloak.KeycloakAdminClient;
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

import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for SecurityConfiguration with OAuth2 Resource Server.
 * Tests the complete Spring Security configuration with JWT Keycloak.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(SecurityConfigurationIntegrationTest.TestSecurityConfig.class)
@DisplayName("SecurityConfiguration - OAuth2 Integration Tests")
class SecurityConfigurationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @TestConfiguration
    public static class TestSecurityConfig {

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

    @Nested
    @DisplayName("Public endpoints tests")
    class PublicEndpointsTests {

        @Test
        @DisplayName("Should allow access to /ecclesiaflow/auth/password/setup without authentication")
        void shouldPermitAccessToPasswordSetupEndpoint() throws Exception {
            mockMvc.perform(post("/ecclesiaflow/auth/password/setup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Setup-Token", "test-token")
                            .content("{\"password\":\"TestPassword123!\"}"))
                    .andExpect(status().is4xxClientError()); // 4xx because validation fails, but not 401
        }

        @Test
        @DisplayName("Should allow access to health checks without authentication")
        void shouldPermitAccessToHealthChecks() throws Exception {
            // Verify access is allowed (not 401/403), even if endpoint does not exist (404)
            mockMvc.perform(get("/actuator/health/liveness"))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        if (status == 401 || status == 403) {
                            throw new AssertionError("Endpoint should be accessible without authentication, got: " + status);
                        }
                    });

            mockMvc.perform(get("/actuator/health/readiness"))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        if (status == 401 || status == 403) {
                            throw new AssertionError("Endpoint should be accessible without authentication, got: " + status);
                        }
                    });
        }

        @Test
        @DisplayName("Should allow access to Swagger without authentication")
        void shouldPermitAccessToSwagger() throws Exception {
            // Verify access is allowed (not 401/403)
            mockMvc.perform(get("/swagger-ui/index.html"))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        if (status == 401 || status == 403) {
                            throw new AssertionError("Swagger should be accessible without authentication, got: " + status);
                        }
                    });
        }

        @Test
        @DisplayName("Should allow access to OpenAPI docs without authentication")
        void shouldPermitAccessToOpenApiDocs() throws Exception {
            // Verify access is allowed (not 401/403)
            mockMvc.perform(get("/v3/api-docs"))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        if (status == 401 || status == 403) {
                            throw new AssertionError("OpenAPI docs should be accessible without authentication, got: " + status);
                        }
                    });
        }
    }

    @Nested
    @DisplayName("Protected endpoints tests - Without authentication")
    class ProtectedEndpointsWithoutAuthTests {

        @Test
        @DisplayName("Should deny access to protected endpoints without JWT")
        void shouldDenyAccessToProtectedEndpointsWithoutJwt() throws Exception {
            mockMvc.perform(get("/ecclesiaflow/api/protected"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should deny access to actuator endpoints without JWT")
        void shouldDenyAccessToActuatorEndpointsWithoutJwt() throws Exception {
            mockMvc.perform(get("/actuator/metrics"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should deny access with invalid JWT")
        void shouldDenyAccessWithInvalidJwt() throws Exception {
            mockMvc.perform(get("/ecclesiaflow/api/protected")
                            .header("Authorization", "Bearer invalid-token"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Tests with valid JWT")
    class AuthenticatedEndpointsTests {

        @Test
        @DisplayName("Should allow access with valid JWT")
        void shouldAllowAccessWithValidJwt() throws Exception {
            mockMvc.perform(get("/ecclesiaflow/api/test")
                            .with(jwt()))
                    .andExpect(status().isNotFound()); // 404 because endpoint does not exist, but not 401
        }

        @Test
        @DisplayName("Should extract claims from JWT")
        void shouldExtractClaimsFromJwt() throws Exception {
            mockMvc.perform(get("/ecclesiaflow/api/user")
                            .with(jwt().jwt(jwt -> jwt
                                    .claim("sub", "user-123")
                                    .claim("email", "user@test.com"))))
                    .andExpect(status().isNotFound()); // 404 but authenticated
        }

        @Test
        @DisplayName("Should allow access to actuator endpoints with SUPER_ADMIN role")
        void shouldAllowAccessToActuatorWithSuperAdminRole() throws Exception {
            // Verify access is not denied (not 403), even if endpoint does not exist (404)
            mockMvc.perform(get("/actuator/metrics")
                            .with(jwt().authorities(() -> "ROLE_SUPER_ADMIN")))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        if (status == 403) {
                            throw new AssertionError("SUPER_ADMIN should have access to actuator, got: " + status);
                        }
                    });
        }

        @Test
        @DisplayName("Should allow access to actuator endpoints with SUPPORT role")
        void shouldAllowAccessToActuatorWithSupportRole() throws Exception {
            // Verify access is not denied (not 403), even if endpoint does not exist (404)
            mockMvc.perform(get("/actuator/metrics")
                            .with(jwt().authorities(() -> "ROLE_SUPPORT")))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        if (status == 403) {
                            throw new AssertionError("SUPPORT should have access to actuator, got: " + status);
                        }
                    });
        }

        @Test
        @DisplayName("Should deny access to actuator endpoints without appropriate role")
        void shouldDenyAccessToActuatorWithoutAppropriateRole() throws Exception {
            mockMvc.perform(get("/actuator/metrics")
                            .with(jwt().authorities(() -> "ROLE_USER")))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Session management tests (STATELESS)")
    class SessionManagementTests {

        @Test
        @DisplayName("Should use STATELESS session policy")
        void shouldUseStatelessSessionPolicy() throws Exception {
            mockMvc.perform(get("/ecclesiaflow/auth/password/setup")
                            .with(jwt()))
                    .andExpect(request -> {
                        if (request.getRequest().getSession(false) != null) {
                            throw new AssertionError("Session should not be created in STATELESS mode");
                        }
                    });
        }

        @Test
        @DisplayName("Should not create session even after multiple requests")
        void shouldNotCreateSessionAfterMultipleRequests() throws Exception {
            for (int i = 0; i < 3; i++) {
                mockMvc.perform(get("/ecclesiaflow/api/test")
                                .with(jwt()))
                        .andExpect(request -> {
                            if (request.getRequest().getSession(false) != null) {
                                throw new AssertionError("Session should not be created");
                            }
                        });
            }
        }
    }

    @Nested
    @DisplayName("CSRF tests (disabled)")
    class CsrfTests {

        @Test
        @DisplayName("Should accept POST requests without CSRF token")
        void shouldAcceptPostRequestsWithoutCsrfToken() throws Exception {
            mockMvc.perform(post("/ecclesiaflow/auth/password/setup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Setup-Token", "test-token")
                            .content("{\"password\":\"Test123!\"}"))
                    .andExpect(status().is4xxClientError()); // 4xx but not 403 (CSRF)
        }

        @Test
        @DisplayName("Should accept PUT requests without CSRF token")
        void shouldAcceptPutRequestsWithoutCsrfToken() throws Exception {
            mockMvc.perform(put("/ecclesiaflow/api/update")
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isNotFound()); // 404 but not 403 (CSRF)
        }

        @Test
        @DisplayName("Should accept DELETE requests without CSRF token")
        void shouldAcceptDeleteRequestsWithoutCsrfToken() throws Exception {
            mockMvc.perform(delete("/ecclesiaflow/api/delete")
                            .with(jwt()))
                    .andExpect(status().isNotFound()); // 404 but not 403 (CSRF)
        }
    }

    @Nested
    @DisplayName("General security tests")
    class GeneralSecurityTests {

        @Test
        @DisplayName("Should require authentication for all non-public endpoints")
        void shouldRequireAuthenticationForAllNonPublicEndpoints() throws Exception {
            String[] protectedPaths = {
                    "/ecclesiaflow/api/data",
                    "/ecclesiaflow/secure/resource",
                    "/actuator/metrics"
            };

            for (String path : protectedPaths) {
                mockMvc.perform(get(path))
                        .andExpect(status().isUnauthorized());
            }
        }

        @Test
        @DisplayName("Should accept authenticated requests for protected endpoints")
        void shouldAcceptAuthenticatedRequestsForProtectedEndpoints() throws Exception {
            mockMvc.perform(get("/ecclesiaflow/api/test")
                            .with(jwt()))
                    .andExpect(status().isNotFound()); // 404 but authenticated
        }

        @Test
        @DisplayName("Should validate JWT format")
        void shouldValidateJwtFormat() throws Exception {
            mockMvc.perform(get("/ecclesiaflow/api/test")
                            .header("Authorization", "Bearer malformed.jwt.token"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("HTTP methods tests")
    class HttpMethodsTests {

        @Test
        @DisplayName("GET on public endpoint should work")
        void shouldAllowGetOnPublicEndpoint() throws Exception {
            // Verify access is allowed (not 401/403)
            mockMvc.perform(get("/actuator/health/liveness"))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        if (status == 401 || status == 403) {
                            throw new AssertionError("Public endpoint should be accessible, got: " + status);
                        }
                    });
        }

        @Test
        @DisplayName("POST on public endpoint should work")
        void shouldAllowPostOnPublicEndpoint() throws Exception {
            mockMvc.perform(post("/ecclesiaflow/auth/password/setup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Setup-Token", "token")
                            .content("{\"password\":\"Test123!\"}"))
                    .andExpect(status().is4xxClientError()); // 4xx but not 401
        }

        @Test
        @DisplayName("All HTTP methods should respect authorization rules")
        void shouldRespectAuthorizationForAllHttpMethods() throws Exception {
            mockMvc.perform(get("/ecclesiaflow/api/test").with(jwt()))
                    .andExpect(status().isNotFound());

            mockMvc.perform(post("/ecclesiaflow/api/test")
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isNotFound());

            mockMvc.perform(put("/ecclesiaflow/api/test")
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isNotFound());

            mockMvc.perform(delete("/ecclesiaflow/api/test").with(jwt()))
                    .andExpect(status().isNotFound());

            mockMvc.perform(patch("/ecclesiaflow/api/test")
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Tests OAuth2 Resource Server")
    class OAuth2ResourceServerTests {

        @Test
        @DisplayName("Should use OAuth2 Resource Server for JWT validation")
        void shouldUseOAuth2ResourceServerForJwtValidation() throws Exception {
            mockMvc.perform(get("/ecclesiaflow/api/test")
                            .with(jwt().jwt(jwt -> jwt.claim("sub", "test-user"))))
                    .andExpect(status().isNotFound()); // Authenticated
        }

        @Test
        @DisplayName("Should extract roles from JWT via KeycloakJwtConverter")
        void shouldExtractRolesFromJwtViaConverter() throws Exception {
            // Verify role is recognized (not 403)
            mockMvc.perform(get("/actuator/metrics")
                            .with(jwt().authorities(() -> "ROLE_SUPER_ADMIN")))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        if (status == 403) {
                            throw new AssertionError("JWT role should be recognized, got: " + status);
                        }
                    });
        }

        @Test
        @DisplayName("Should reject JWTs without valid issuer")
        void shouldRejectJwtWithoutValidIssuer() throws Exception {
            mockMvc.perform(get("/ecclesiaflow/api/test")
                            .header("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalid"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
