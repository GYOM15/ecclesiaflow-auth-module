package com.ecclesiaflow.springsecurity.web.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

@DisplayName("KeycloakJwtConverter - Tests unitaires")
class KeycloakJwtConverterTest {

    private KeycloakJwtConverter keycloakJwtConverter;

    @BeforeEach
    void setUp() {
        keycloakJwtConverter = new KeycloakJwtConverter();
    }

    @Nested
    @DisplayName("convert - Conversion JWT vers Authentication")
    class ConvertTests {

        @Test
        @DisplayName("Devrait convertir un JWT en AbstractAuthenticationToken")
        void shouldConvertJwtToAuthenticationToken() {
            Jwt jwt = createJwtWithRoles(List.of("USER"));

            AbstractAuthenticationToken token = keycloakJwtConverter.convert(jwt);

            assertThat(token).isNotNull();
            assertThat(token.getPrincipal()).isEqualTo(jwt);
        }

        @Test
        @DisplayName("Devrait extraire les rôles directs du claim 'roles'")
        void shouldExtractDirectRolesFromRolesClaim() {
            Jwt jwt = createJwtWithDirectRoles(List.of("ADMIN", "USER"));

            AbstractAuthenticationToken token = keycloakJwtConverter.convert(jwt);

            Collection<? extends GrantedAuthority> authorities = token.getAuthorities();
            assertThat(authorities)
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
        }

        @Test
        @DisplayName("Devrait extraire les rôles du claim 'realm_access.roles'")
        void shouldExtractRealmAccessRoles() {
            Jwt jwt = createJwtWithRealmAccessRoles(List.of("SUPER_ADMIN", "SUPPORT"));

            AbstractAuthenticationToken token = keycloakJwtConverter.convert(jwt);

            Collection<? extends GrantedAuthority> authorities = token.getAuthorities();
            assertThat(authorities)
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactlyInAnyOrder("ROLE_SUPER_ADMIN", "ROLE_SUPPORT");
        }

        @Test
        @DisplayName("Devrait combiner les rôles directs et realm_access")
        void shouldCombineDirectAndRealmAccessRoles() {
            Map<String, Object> claims = new HashMap<>();
            claims.put("roles", List.of("USER"));
            claims.put("realm_access", Map.of("roles", List.of("ADMIN")));
            claims.put("sub", "user-123");

            Jwt jwt = createJwt(claims);

            AbstractAuthenticationToken token = keycloakJwtConverter.convert(jwt);

            Collection<? extends GrantedAuthority> authorities = token.getAuthorities();
            assertThat(authorities)
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
        }

        @Test
        @DisplayName("Devrait dédupliquer les rôles identiques")
        void shouldDeduplicateIdenticalRoles() {
            Map<String, Object> claims = new HashMap<>();
            claims.put("roles", List.of("ADMIN"));
            claims.put("realm_access", Map.of("roles", List.of("ADMIN")));
            claims.put("sub", "user-123");

            Jwt jwt = createJwt(claims);

            AbstractAuthenticationToken token = keycloakJwtConverter.convert(jwt);

            Collection<? extends GrantedAuthority> authorities = token.getAuthorities();
            assertThat(authorities)
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_ADMIN");
        }

        @Test
        @DisplayName("Devrait retourner une liste vide si aucun rôle n'est présent")
        void shouldReturnEmptyListWhenNoRolesPresent() {
            Jwt jwt = createJwtWithoutRoles();

            AbstractAuthenticationToken token = keycloakJwtConverter.convert(jwt);

            Collection<? extends GrantedAuthority> authorities = token.getAuthorities();
            assertThat(authorities).isEmpty();
        }

        @Test
        @DisplayName("Devrait préfixer les rôles avec ROLE_ s'ils ne le sont pas déjà")
        void shouldPrefixRolesWithRolePrefix() {
            Jwt jwt = createJwtWithDirectRoles(List.of("USER", "ROLE_ADMIN"));

            AbstractAuthenticationToken token = keycloakJwtConverter.convert(jwt);

            Collection<? extends GrantedAuthority> authorities = token.getAuthorities();
            assertThat(authorities)
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
        }

        @Test
        @DisplayName("Devrait utiliser l'email comme principal si disponible")
        void shouldUseEmailAsPrincipalWhenAvailable() {
            Map<String, Object> claims = new HashMap<>();
            claims.put("sub", "user-123");
            claims.put("email", "user@example.com");

            Jwt jwt = createJwt(claims);

            AbstractAuthenticationToken token = keycloakJwtConverter.convert(jwt);

            assertThat(token.getName()).isEqualTo("user@example.com");
        }

        @Test
        @DisplayName("Devrait utiliser le subject comme principal si email absent")
        void shouldUseSubjectAsPrincipalWhenEmailAbsent() {
            Map<String, Object> claims = new HashMap<>();
            claims.put("sub", "user-123");

            Jwt jwt = createJwt(claims);

            AbstractAuthenticationToken token = keycloakJwtConverter.convert(jwt);

            assertThat(token.getName()).isEqualTo("user-123");
        }

        @Test
        @DisplayName("Devrait utiliser le subject si email est vide")
        void shouldUseSubjectWhenEmailIsBlank() {
            Map<String, Object> claims = new HashMap<>();
            claims.put("sub", "user-123");
            claims.put("email", "");

            Jwt jwt = createJwt(claims);

            AbstractAuthenticationToken token = keycloakJwtConverter.convert(jwt);

            assertThat(token.getName()).isEqualTo("user-123");
        }
    }

    @Nested
    @DisplayName("extractUserId - Extraction de l'ID utilisateur")
    class ExtractUserIdTests {

        @Test
        @DisplayName("Devrait extraire l'ID utilisateur du claim 'sub'")
        void shouldExtractUserIdFromSubClaim() {
            Jwt jwt = createJwtWithSubject("user-456");

            String userId = keycloakJwtConverter.extractUserId(jwt);

            assertThat(userId).isEqualTo("user-456");
        }

        @Test
        @DisplayName("Devrait retourner null si le subject est absent")
        void shouldReturnNullWhenSubjectAbsent() {
            Map<String, Object> claims = new HashMap<>();
            Jwt jwt = createJwt(claims);

            String userId = keycloakJwtConverter.extractUserId(jwt);

            assertThat(userId).isNull();
        }
    }

    @Nested
    @DisplayName("extractEmail - Extraction de l'email")
    class ExtractEmailTests {

        @Test
        @DisplayName("Devrait extraire l'email du claim 'email'")
        void shouldExtractEmailFromEmailClaim() {
            Map<String, Object> claims = new HashMap<>();
            claims.put("sub", "user-123");
            claims.put("email", "test@example.com");

            Jwt jwt = createJwt(claims);

            String email = keycloakJwtConverter.extractEmail(jwt);

            assertThat(email).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("Devrait retourner null si l'email est absent")
        void shouldReturnNullWhenEmailAbsent() {
            Jwt jwt = createJwtWithSubject("user-123");

            String email = keycloakJwtConverter.extractEmail(jwt);

            assertThat(email).isNull();
        }
    }

    @Nested
    @DisplayName("hasRole - Vérification de rôle")
    class HasRoleTests {

        @Test
        @DisplayName("Devrait retourner true si le rôle est présent")
        void shouldReturnTrueWhenRoleIsPresent() {
            Jwt jwt = createJwtWithDirectRoles(List.of("ADMIN", "USER"));

            boolean hasRole = keycloakJwtConverter.hasRole(jwt, "ADMIN");

            assertThat(hasRole).isTrue();
        }

        @Test
        @DisplayName("Devrait retourner false si le rôle est absent")
        void shouldReturnFalseWhenRoleIsAbsent() {
            Jwt jwt = createJwtWithDirectRoles(List.of("USER"));

            boolean hasRole = keycloakJwtConverter.hasRole(jwt, "ADMIN");

            assertThat(hasRole).isFalse();
        }

        @Test
        @DisplayName("Devrait fonctionner avec ou sans préfixe ROLE_")
        void shouldWorkWithOrWithoutRolePrefix() {
            Jwt jwt = createJwtWithDirectRoles(List.of("ADMIN"));

            assertThat(keycloakJwtConverter.hasRole(jwt, "ADMIN")).isTrue();
            assertThat(keycloakJwtConverter.hasRole(jwt, "ROLE_ADMIN")).isTrue();
        }

        @Test
        @DisplayName("Devrait vérifier les rôles dans realm_access")
        void shouldCheckRolesInRealmAccess() {
            Jwt jwt = createJwtWithRealmAccessRoles(List.of("SUPER_ADMIN"));

            boolean hasRole = keycloakJwtConverter.hasRole(jwt, "SUPER_ADMIN");

            assertThat(hasRole).isTrue();
        }

        @Test
        @DisplayName("Devrait retourner false si aucun rôle n'est présent")
        void shouldReturnFalseWhenNoRolesPresent() {
            Jwt jwt = createJwtWithoutRoles();

            boolean hasRole = keycloakJwtConverter.hasRole(jwt, "ADMIN");

            assertThat(hasRole).isFalse();
        }
    }

    @Nested
    @DisplayName("Gestion des cas limites")
    class EdgeCaseTests {

        @Test
        @DisplayName("Devrait gérer un claim 'roles' qui n'est pas une liste")
        void shouldHandleRolesClaimThatIsNotAList() {
            Map<String, Object> claims = new HashMap<>();
            claims.put("roles", "ADMIN");
            claims.put("sub", "user-123");

            Jwt jwt = createJwt(claims);

            AbstractAuthenticationToken token = keycloakJwtConverter.convert(jwt);

            Collection<? extends GrantedAuthority> authorities = token.getAuthorities();
            assertThat(authorities).isEmpty();
        }

        @Test
        @DisplayName("Devrait gérer un claim 'realm_access' sans 'roles'")
        void shouldHandleRealmAccessWithoutRoles() {
            Map<String, Object> claims = new HashMap<>();
            claims.put("realm_access", Map.of("other", "value"));
            claims.put("sub", "user-123");

            Jwt jwt = createJwt(claims);

            AbstractAuthenticationToken token = keycloakJwtConverter.convert(jwt);

            Collection<? extends GrantedAuthority> authorities = token.getAuthorities();
            assertThat(authorities).isEmpty();
        }

        @Test
        @DisplayName("Devrait gérer un claim 'realm_access.roles' qui n'est pas une liste")
        void shouldHandleRealmAccessRolesThatIsNotAList() {
            Map<String, Object> claims = new HashMap<>();
            claims.put("realm_access", Map.of("roles", "ADMIN"));
            claims.put("sub", "user-123");

            Jwt jwt = createJwt(claims);

            AbstractAuthenticationToken token = keycloakJwtConverter.convert(jwt);

            Collection<? extends GrantedAuthority> authorities = token.getAuthorities();
            assertThat(authorities).isEmpty();
        }

        @Test
        @DisplayName("Devrait gérer des listes de rôles vides")
        void shouldHandleEmptyRoleLists() {
            Map<String, Object> claims = new HashMap<>();
            claims.put("roles", List.of());
            claims.put("realm_access", Map.of("roles", List.of()));
            claims.put("sub", "user-123");

            Jwt jwt = createJwt(claims);

            AbstractAuthenticationToken token = keycloakJwtConverter.convert(jwt);

            Collection<? extends GrantedAuthority> authorities = token.getAuthorities();
            assertThat(authorities).isEmpty();
        }
    }

    // Helper methods
    private Jwt createJwt(Map<String, Object> claims) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claims(c -> c.putAll(claims))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    private Jwt createJwtWithDirectRoles(List<String> roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roles);
        claims.put("sub", "user-123");
        return createJwt(claims);
    }

    private Jwt createJwtWithRealmAccessRoles(List<String> roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("realm_access", Map.of("roles", roles));
        claims.put("sub", "user-123");
        return createJwt(claims);
    }

    private Jwt createJwtWithRoles(List<String> roles) {
        return createJwtWithDirectRoles(roles);
    }

    private Jwt createJwtWithoutRoles() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "user-123");
        return createJwt(claims);
    }

    private Jwt createJwtWithSubject(String subject) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", subject);
        return createJwt(claims);
    }
}
