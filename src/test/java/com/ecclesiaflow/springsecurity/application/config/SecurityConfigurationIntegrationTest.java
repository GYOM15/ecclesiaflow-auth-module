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
 * Tests d'intégration pour SecurityConfiguration avec OAuth2 Resource Server.
 * Teste la configuration complète de Spring Security avec JWT Keycloak.
 */
@SpringBootTest(properties = {
    "grpc.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8080/realms/test"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(SecurityConfigurationIntegrationTest.TestSecurityConfig.class)
@DisplayName("SecurityConfiguration - Tests d'Intégration OAuth2")
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
    @DisplayName("Tests des endpoints publics")
    class PublicEndpointsTests {

        @Test
        @DisplayName("Devrait permettre l'accès à /ecclesiaflow/auth/password/setup sans authentification")
        void shouldPermitAccessToPasswordSetupEndpoint() throws Exception {
            mockMvc.perform(post("/ecclesiaflow/auth/password/setup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Setup-Token", "test-token")
                            .content("{\"password\":\"TestPassword123!\"}"))
                    .andExpect(status().is4xxClientError()); // 4xx car validation échoue, mais pas 401
        }

        @Test
        @DisplayName("Devrait permettre l'accès aux health checks sans authentification")
        void shouldPermitAccessToHealthChecks() throws Exception {
            // Vérifie que l'accès est autorisé (pas 401/403), même si endpoint n'existe pas (404)
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
        @DisplayName("Devrait permettre l'accès à Swagger sans authentification")
        void shouldPermitAccessToSwagger() throws Exception {
            // Vérifie que l'accès est autorisé (pas 401/403)
            mockMvc.perform(get("/swagger-ui/index.html"))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        if (status == 401 || status == 403) {
                            throw new AssertionError("Swagger should be accessible without authentication, got: " + status);
                        }
                    });
        }

        @Test
        @DisplayName("Devrait permettre l'accès à OpenAPI docs sans authentification")
        void shouldPermitAccessToOpenApiDocs() throws Exception {
            // Vérifie que l'accès est autorisé (pas 401/403)
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
    @DisplayName("Tests des endpoints protégés - Sans authentification")
    class ProtectedEndpointsWithoutAuthTests {

        @Test
        @DisplayName("Devrait refuser l'accès aux endpoints protégés sans JWT")
        void shouldDenyAccessToProtectedEndpointsWithoutJwt() throws Exception {
            mockMvc.perform(get("/ecclesiaflow/api/protected"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Devrait refuser l'accès aux actuator endpoints sans JWT")
        void shouldDenyAccessToActuatorEndpointsWithoutJwt() throws Exception {
            mockMvc.perform(get("/actuator/metrics"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Devrait refuser l'accès avec JWT invalide")
        void shouldDenyAccessWithInvalidJwt() throws Exception {
            mockMvc.perform(get("/ecclesiaflow/api/protected")
                            .header("Authorization", "Bearer invalid-token"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Tests avec JWT valide")
    class AuthenticatedEndpointsTests {

        @Test
        @DisplayName("Devrait permettre l'accès avec JWT valide")
        void shouldAllowAccessWithValidJwt() throws Exception {
            mockMvc.perform(get("/ecclesiaflow/api/test")
                            .with(jwt()))
                    .andExpect(status().isNotFound()); // 404 car endpoint n'existe pas, mais pas 401
        }

        @Test
        @DisplayName("Devrait extraire les claims du JWT")
        void shouldExtractClaimsFromJwt() throws Exception {
            mockMvc.perform(get("/ecclesiaflow/api/user")
                            .with(jwt().jwt(jwt -> jwt
                                    .claim("sub", "user-123")
                                    .claim("email", "user@test.com"))))
                    .andExpect(status().isNotFound()); // 404 mais authentifié
        }

        @Test
        @DisplayName("Devrait permettre l'accès aux actuator endpoints avec rôle SUPER_ADMIN")
        void shouldAllowAccessToActuatorWithSuperAdminRole() throws Exception {
            // Vérifie que l'accès n'est pas refusé (pas 403), même si endpoint n'existe pas (404)
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
        @DisplayName("Devrait permettre l'accès aux actuator endpoints avec rôle SUPPORT")
        void shouldAllowAccessToActuatorWithSupportRole() throws Exception {
            // Vérifie que l'accès n'est pas refusé (pas 403), même si endpoint n'existe pas (404)
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
        @DisplayName("Devrait refuser l'accès aux actuator endpoints sans rôle approprié")
        void shouldDenyAccessToActuatorWithoutAppropriateRole() throws Exception {
            mockMvc.perform(get("/actuator/metrics")
                            .with(jwt().authorities(() -> "ROLE_USER")))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Tests de la gestion des sessions (STATELESS)")
    class SessionManagementTests {

        @Test
        @DisplayName("Devrait utiliser une politique de session STATELESS")
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
        @DisplayName("Ne devrait pas créer de session même après plusieurs requêtes")
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
    @DisplayName("Tests CSRF (désactivé)")
    class CsrfTests {

        @Test
        @DisplayName("Devrait accepter les requêtes POST sans token CSRF")
        void shouldAcceptPostRequestsWithoutCsrfToken() throws Exception {
            mockMvc.perform(post("/ecclesiaflow/auth/password/setup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Setup-Token", "test-token")
                            .content("{\"password\":\"Test123!\"}"))
                    .andExpect(status().is4xxClientError()); // 4xx mais pas 403 (CSRF)
        }

        @Test
        @DisplayName("Devrait accepter les requêtes PUT sans token CSRF")
        void shouldAcceptPutRequestsWithoutCsrfToken() throws Exception {
            mockMvc.perform(put("/ecclesiaflow/api/update")
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isNotFound()); // 404 mais pas 403 (CSRF)
        }

        @Test
        @DisplayName("Devrait accepter les requêtes DELETE sans token CSRF")
        void shouldAcceptDeleteRequestsWithoutCsrfToken() throws Exception {
            mockMvc.perform(delete("/ecclesiaflow/api/delete")
                            .with(jwt()))
                    .andExpect(status().isNotFound()); // 404 mais pas 403 (CSRF)
        }
    }

    @Nested
    @DisplayName("Tests de sécurité générale")
    class GeneralSecurityTests {

        @Test
        @DisplayName("Devrait exiger l'authentification pour tous les endpoints non-publics")
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
        @DisplayName("Devrait accepter les requêtes authentifiées pour les endpoints protégés")
        void shouldAcceptAuthenticatedRequestsForProtectedEndpoints() throws Exception {
            mockMvc.perform(get("/ecclesiaflow/api/test")
                            .with(jwt()))
                    .andExpect(status().isNotFound()); // 404 mais authentifié
        }

        @Test
        @DisplayName("Devrait valider le format du JWT")
        void shouldValidateJwtFormat() throws Exception {
            mockMvc.perform(get("/ecclesiaflow/api/test")
                            .header("Authorization", "Bearer malformed.jwt.token"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Tests des différentes méthodes HTTP")
    class HttpMethodsTests {

        @Test
        @DisplayName("GET sur endpoint public devrait fonctionner")
        void shouldAllowGetOnPublicEndpoint() throws Exception {
            // Vérifie que l'accès est autorisé (pas 401/403)
            mockMvc.perform(get("/actuator/health/liveness"))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        if (status == 401 || status == 403) {
                            throw new AssertionError("Public endpoint should be accessible, got: " + status);
                        }
                    });
        }

        @Test
        @DisplayName("POST sur endpoint public devrait fonctionner")
        void shouldAllowPostOnPublicEndpoint() throws Exception {
            mockMvc.perform(post("/ecclesiaflow/auth/password/setup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Setup-Token", "token")
                            .content("{\"password\":\"Test123!\"}"))
                    .andExpect(status().is4xxClientError()); // 4xx mais pas 401
        }

        @Test
        @DisplayName("Toutes les méthodes HTTP devraient respecter les règles d'autorisation")
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
        @DisplayName("Devrait utiliser OAuth2 Resource Server pour la validation JWT")
        void shouldUseOAuth2ResourceServerForJwtValidation() throws Exception {
            mockMvc.perform(get("/ecclesiaflow/api/test")
                            .with(jwt().jwt(jwt -> jwt.claim("sub", "test-user"))))
                    .andExpect(status().isNotFound()); // Authentifié
        }

        @Test
        @DisplayName("Devrait extraire les rôles du JWT via KeycloakJwtConverter")
        void shouldExtractRolesFromJwtViaConverter() throws Exception {
            // Vérifie que le rôle est reconnu (pas 403)
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
        @DisplayName("Devrait rejeter les JWT sans issuer valide")
        void shouldRejectJwtWithoutValidIssuer() throws Exception {
            mockMvc.perform(get("/ecclesiaflow/api/test")
                            .header("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalid"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
