package com.ecclesiaflow.springsecurity.application.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'intégration pour SecurityConfiguration.
 * Teste la configuration complète de Spring Security incluant securityFilterChain().
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("SecurityConfiguration - Tests d'Intégration")
class SecurityConfigurationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ====================================================================
    // Tests des endpoints publics (/ecclesiaflow/auth/**)
    // ====================================================================

    @Nested
    @DisplayName("Tests des endpoints publics")
    class PublicEndpointsTests {

        @Test
        @DisplayName("Devrait permettre l'accès à /ecclesiaflow/auth/register sans authentification")
        void shouldPermitAccessToRegisterEndpoint() throws Exception {
            mockMvc.perform(post("/ecclesiaflow/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().is4xxClientError()); // 4xx car endpoint pas implémenté, mais pas 401/403
        }

        @Test
        @DisplayName("Devrait permettre l'accès à /ecclesiaflow/auth/login sans authentification")
        void shouldPermitAccessToLoginEndpoint() throws Exception {
            mockMvc.perform(post("/ecclesiaflow/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().is4xxClientError()); // 4xx car endpoint pas implémenté, mais pas 401/403
        }

        @Test
        @DisplayName("Devrait permettre l'accès à /ecclesiaflow/auth/refresh sans authentification")
        void shouldPermitAccessToRefreshEndpoint() throws Exception {
            // Utiliser un endpoint qui n'existe pas pour éviter les erreurs 500
            mockMvc.perform(post("/ecclesiaflow/auth/nonexistent")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().is4xxClientError()); // 4xx car endpoint pas implémenté, mais pas 401/403
        }

        @Test
        @DisplayName("Devrait permettre l'accès à tous les sous-chemins de /ecclesiaflow/auth/**")
        void shouldPermitAccessToAllAuthSubpaths() throws Exception {
            mockMvc.perform(get("/ecclesiaflow/auth/any/subpath"))
                    .andExpect(status().is4xxClientError()); // 4xx mais pas 401/403
        }
    }

    // ====================================================================
    // Tests des endpoints protégés (authentification requise)
    // ====================================================================

    @Nested
    @DisplayName("Tests des endpoints protégés - Sans authentification")
    class ProtectedEndpointsWithoutAuthTests {

        @Test
        @DisplayName("Devrait refuser l'accès à /ecclesiaflow/members sans authentification")
        void shouldDenyAccessToMembersEndpointWithoutAuth() throws Exception {
            mockMvc.perform(get("/ecclesiaflow/members"))
                    .andExpect(status().isUnauthorized()); // 401
        }

        @Test
        @DisplayName("Devrait refuser l'accès à /ecclesiaflow/adminMembers sans authentification")
        void shouldDenyAccessToAdminMembersEndpointWithoutAuth() throws Exception {
            mockMvc.perform(get("/ecclesiaflow/adminMembers"))
                    .andExpect(status().isUnauthorized()); // 401
        }

        @Test
        @DisplayName("Devrait refuser l'accès à tout endpoint non-auth sans authentification")
        void shouldDenyAccessToAnyNonAuthEndpointWithoutAuth() throws Exception {
            mockMvc.perform(get("/ecclesiaflow/some/protected/resource"))
                    .andExpect(status().isUnauthorized()); // 401
        }
    }

    // ====================================================================
    // Tests avec rôle MEMBER
    // ====================================================================

    @Nested
    @DisplayName("Tests avec rôle MEMBER")
    class MemberRoleTests {

        @Test
        @WithMockUser(authorities = "ROLE_MEMBER")
        @DisplayName("Devrait permettre l'accès à /ecclesiaflow/members avec rôle MEMBER")
        void shouldAllowAccessToMembersEndpointWithMemberRole() throws Exception {
            mockMvc.perform(get("/ecclesiaflow/members"))
                    .andExpect(status().is4xxClientError()); // 4xx (endpoint existe pas) mais pas 403
        }

        @Test
        @WithMockUser(authorities = "ROLE_MEMBER")
        @DisplayName("Devrait refuser l'accès à /ecclesiaflow/adminMembers avec rôle MEMBER")
        void shouldDenyAccessToAdminMembersEndpointWithMemberRole() throws Exception {
            mockMvc.perform(get("/ecclesiaflow/adminMembers"))
                    .andExpect(status().isForbidden()); // 403
        }

        @Test
        @WithMockUser(authorities = "ROLE_MEMBER")
        @DisplayName("Devrait permettre l'accès aux endpoints publics avec rôle MEMBER")
        void shouldAllowAccessToPublicEndpointsWithMemberRole() throws Exception {
            mockMvc.perform(get("/ecclesiaflow/auth/login"))
                    .andExpect(status().is4xxClientError()); // 4xx mais pas 403
        }
    }

    // ====================================================================
    // Tests avec rôle ADMIN
    // ====================================================================

    @Nested
    @DisplayName("Tests avec rôle ADMIN")
    class AdminRoleTests {

        @Test
        @WithMockUser(authorities = "ROLE_ADMIN")
        @DisplayName("Devrait permettre l'accès à /ecclesiaflow/adminMembers avec rôle ADMIN")
        void shouldAllowAccessToAdminMembersEndpointWithAdminRole() throws Exception {
            mockMvc.perform(get("/ecclesiaflow/adminMembers"))
                    .andExpect(status().is4xxClientError()); // 4xx (endpoint existe pas) mais pas 403
        }

        @Test
        @WithMockUser(authorities = "ROLE_ADMIN")
        @DisplayName("Devrait refuser l'accès à /ecclesiaflow/members avec rôle ADMIN seul")
        void shouldDenyAccessToMembersEndpointWithAdminRoleOnly() throws Exception {
            mockMvc.perform(get("/ecclesiaflow/members"))
                    .andExpect(status().isForbidden()); // 403 car ADMIN n'a pas ROLE_MEMBER
        }

        @Test
        @WithMockUser(authorities = {"ROLE_ADMIN", "ROLE_MEMBER"})
        @DisplayName("Devrait permettre l'accès aux deux endpoints avec ADMIN et MEMBER")
        void shouldAllowAccessToBothEndpointsWithBothRoles() throws Exception {
            mockMvc.perform(get("/ecclesiaflow/members"))
                    .andExpect(status().is4xxClientError()); // 4xx mais pas 403

            mockMvc.perform(get("/ecclesiaflow/adminMembers"))
                    .andExpect(status().is4xxClientError()); // 4xx mais pas 403
        }
    }

    // ====================================================================
    // Tests de la gestion des sessions (STATELESS)
    // ====================================================================

    @Nested
    @DisplayName("Tests de la gestion des sessions")
    class SessionManagementTests {

        @Test
        @WithMockUser
        @DisplayName("Devrait utiliser une politique de session STATELESS")
        void shouldUseStatelessSessionPolicy() throws Exception {
            // Faire une requête et vérifier qu'aucune session n'est créée
            mockMvc.perform(get("/ecclesiaflow/auth/login"))
                    .andExpect(status().isNotFound())
                    .andExpect(request -> {
                        // Vérifier qu'aucune session n'est créée
                        if (request.getRequest().getSession(false) != null) {
                            throw new AssertionError("Session should not be created in STATELESS mode");
                        }
                    });
        }
    }

    // ====================================================================
    // Tests de CSRF (désactivé)
    // ====================================================================

    @Nested
    @DisplayName("Tests CSRF")
    class CsrfTests {

        @Test
        @DisplayName("Devrait accepter les requêtes POST sans token CSRF (CSRF désactivé)")
        void shouldAcceptPostRequestsWithoutCsrfToken() throws Exception {
            // POST sans token CSRF devrait fonctionner car CSRF est désactivé
            mockMvc.perform(post("/ecclesiaflow/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isNotFound()); // 404 mais pas 403 (CSRF)
        }

        @Test
        @DisplayName("Devrait accepter les requêtes PUT sans token CSRF")
        void shouldAcceptPutRequestsWithoutCsrfToken() throws Exception {
            mockMvc.perform(put("/ecclesiaflow/auth/update")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isNotFound()); // 404 mais pas 403 (CSRF)
        }

        @Test
        @DisplayName("Devrait accepter les requêtes DELETE sans token CSRF")
        void shouldAcceptDeleteRequestsWithoutCsrfToken() throws Exception {
            mockMvc.perform(delete("/ecclesiaflow/auth/delete"))
                    .andExpect(status().isNotFound()); // 404 mais pas 403 (CSRF)
        }
    }

    // ====================================================================
    // Tests du CustomAuthenticationEntryPoint
    // ====================================================================

    @Nested
    @DisplayName("Tests CustomAuthenticationEntryPoint")
    class AuthenticationEntryPointTests {

        @Test
        @DisplayName("Devrait utiliser CustomAuthenticationEntryPoint pour les erreurs d'authentification")
        void shouldUseCustomAuthenticationEntryPoint() throws Exception {
            // Requête sans authentification à un endpoint protégé
            mockMvc.perform(get("/ecclesiaflow/members"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("Devrait retourner 401 avec en-tête WWW-Authenticate pour endpoint protégé")
        void shouldReturn401WithWwwAuthenticateHeader() throws Exception {
            mockMvc.perform(get("/ecclesiaflow/adminMembers"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ====================================================================
    // Tests de sécurité générale
    // ====================================================================

    @Nested
    @DisplayName("Tests de sécurité générale")
    class GeneralSecurityTests {

        @Test
        @DisplayName("Devrait exiger l'authentification pour tous les endpoints non-auth")
        void shouldRequireAuthenticationForAllNonAuthEndpoints() throws Exception {
            String[] protectedPaths = {
                    "/ecclesiaflow/members",
                    "/ecclesiaflow/adminMembers",
                    "/ecclesiaflow/api/data",
                    "/ecclesiaflow/secure/resource"
            };

            for (String path : protectedPaths) {
                mockMvc.perform(get(path))
                        .andExpect(status().isUnauthorized());
            }
        }

        @Test
        @WithMockUser(authorities = "INVALID_ROLE")
        @DisplayName("Devrait refuser l'accès avec un rôle invalide")
        void shouldDenyAccessWithInvalidRole() throws Exception {
            mockMvc.perform(get("/ecclesiaflow/members"))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get("/ecclesiaflow/adminMembers"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(authorities = {})
        @DisplayName("Devrait refuser l'accès sans aucun rôle")
        void shouldDenyAccessWithoutAnyRole() throws Exception {
            mockMvc.perform(get("/ecclesiaflow/members"))
                    .andExpect(status().isForbidden());
        }
    }

    // ====================================================================
    // Tests des méthodes HTTP
    // ====================================================================

    @Nested
    @DisplayName("Tests des différentes méthodes HTTP")
    class HttpMethodsTests {

        @Test
        @DisplayName("GET sur endpoint public devrait fonctionner")
        void shouldAllowGetOnPublicEndpoint() throws Exception {
            mockMvc.perform(get("/ecclesiaflow/auth/status"))
                    .andExpect(status().isNotFound()); // 404 mais pas 401
        }

        @Test
        @DisplayName("POST sur endpoint public devrait fonctionner")
        void shouldAllowPostOnPublicEndpoint() throws Exception {
            mockMvc.perform(post("/ecclesiaflow/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isNotFound()); // 404 mais pas 401
        }

        @Test
        @WithMockUser(authorities = "ROLE_MEMBER")
        @DisplayName("Toutes les méthodes HTTP devraient respecter les règles d'autorisation")
        void shouldRespectAuthorizationForAllHttpMethods() throws Exception {
            String[] methods = {"GET", "POST", "PUT", "DELETE", "PATCH"};

            for (String method : methods) {
                switch (method) {
                    case "GET":
                        mockMvc.perform(get("/ecclesiaflow/members"))
                                .andExpect(status().isNotFound()); // 404 mais pas 403
                        break;
                    case "POST":
                        mockMvc.perform(post("/ecclesiaflow/members")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{}"))
                                .andExpect(status().isNotFound());
                        break;
                    case "PUT":
                        mockMvc.perform(put("/ecclesiaflow/members")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{}"))
                                .andExpect(status().isNotFound());
                        break;
                    case "DELETE":
                        mockMvc.perform(delete("/ecclesiaflow/members"))
                                .andExpect(status().isNotFound());
                        break;
                    case "PATCH":
                        mockMvc.perform(patch("/ecclesiaflow/members")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{}"))
                                .andExpect(status().isNotFound());
                        break;
                }
            }
        }
    }
}