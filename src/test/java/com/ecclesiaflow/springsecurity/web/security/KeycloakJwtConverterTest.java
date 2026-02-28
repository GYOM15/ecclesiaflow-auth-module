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

@DisplayName("KeycloakJwtConverter - Unit tests")
class KeycloakJwtConverterTest {

    private KeycloakJwtConverter keycloakJwtConverter;

    @BeforeEach
    void setUp() {
        keycloakJwtConverter = new KeycloakJwtConverter();
    }

    @Nested
    @DisplayName("convert - JWT to Authentication conversion")
    class ConvertTests {

        @Test
        @DisplayName("Should convert a JWT to AbstractAuthenticationToken")
        void shouldConvertJwtToAuthenticationToken() {
            Jwt jwt = createJwtWithRoles(List.of("USER"));

            AbstractAuthenticationToken token = keycloakJwtConverter.convert(jwt);

            assertThat(token).isNotNull();
            assertThat(token.getPrincipal()).isEqualTo(jwt);
        }

        @Test
        @DisplayName("Should extract roles directly from 'roles' claim")
        void shouldExtractDirectRolesFromRolesClaim() {
            Jwt jwt = createJwtWithDirectRoles(List.of("ADMIN", "USER"));

            AbstractAuthenticationToken token = keycloakJwtConverter.convert(jwt);

            Collection<? extends GrantedAuthority> authorities = token.getAuthorities();
            assertThat(authorities)
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
        }

        @Test
        @DisplayName("Should extract roles from 'realm_access.roles' claim")
        void shouldExtractRealmAccessRoles() {
            Jwt jwt = createJwtWithRealmAccessRoles(List.of("SUPER_ADMIN", "SUPPORT"));

            AbstractAuthenticationToken token = keycloakJwtConverter.convert(jwt);

            Collection<? extends GrantedAuthority> authorities = token.getAuthorities();
            assertThat(authorities)
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactlyInAnyOrder("ROLE_SUPER_ADMIN", "ROLE_SUPPORT");
        }

        @Test
        @DisplayName("Should combine roles direct and realm_access")
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
        @DisplayName("Should deduplicate roles identical")
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
        @DisplayName("Should return an empty list if no role is present")
        void shouldReturnEmptyListWhenNoRolesPresent() {
            Jwt jwt = createJwtWithoutRoles();

            AbstractAuthenticationToken token = keycloakJwtConverter.convert(jwt);

            Collection<? extends GrantedAuthority> authorities = token.getAuthorities();
            assertThat(authorities).isEmpty();
        }

        @Test
        @DisplayName("Should prefix roles with ROLE_ if they don't already have it")
        void shouldPrefixRolesWithRolePrefix() {
            Jwt jwt = createJwtWithDirectRoles(List.of("USER", "ROLE_ADMIN"));

            AbstractAuthenticationToken token = keycloakJwtConverter.convert(jwt);

            Collection<? extends GrantedAuthority> authorities = token.getAuthorities();
            assertThat(authorities)
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
        }

        @Test
        @DisplayName("Should use email as principal if available")
        void shouldUseEmailAsPrincipalWhenAvailable() {
            Map<String, Object> claims = new HashMap<>();
            claims.put("sub", "user-123");
            claims.put("email", "user@example.com");

            Jwt jwt = createJwt(claims);

            AbstractAuthenticationToken token = keycloakJwtConverter.convert(jwt);

            assertThat(token.getName()).isEqualTo("user@example.com");
        }

        @Test
        @DisplayName("Should use subject as principal if email absent")
        void shouldUseSubjectAsPrincipalWhenEmailAbsent() {
            Map<String, Object> claims = new HashMap<>();
            claims.put("sub", "user-123");

            Jwt jwt = createJwt(claims);

            AbstractAuthenticationToken token = keycloakJwtConverter.convert(jwt);

            assertThat(token.getName()).isEqualTo("user-123");
        }

        @Test
        @DisplayName("Should use subject if email is empty")
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
    @DisplayName("extractUserId - User ID extraction")
    class ExtractUserIdTests {

        @Test
        @DisplayName("Should extract user ID from 'sub' claim")
        void shouldExtractUserIdFromSubClaim() {
            Jwt jwt = createJwtWithSubject("user-456");

            String userId = keycloakJwtConverter.extractUserId(jwt);

            assertThat(userId).isEqualTo("user-456");
        }

        @Test
        @DisplayName("Should return null if subject is absent")
        void shouldReturnNullWhenSubjectAbsent() {
            Map<String, Object> claims = new HashMap<>();
            Jwt jwt = createJwt(claims);

            String userId = keycloakJwtConverter.extractUserId(jwt);

            assertThat(userId).isNull();
        }
    }

    @Nested
    @DisplayName("extractEmail - Email extraction")
    class ExtractEmailTests {

        @Test
        @DisplayName("Should extract email from 'email' claim")
        void shouldExtractEmailFromEmailClaim() {
            Map<String, Object> claims = new HashMap<>();
            claims.put("sub", "user-123");
            claims.put("email", "test@example.com");

            Jwt jwt = createJwt(claims);

            String email = keycloakJwtConverter.extractEmail(jwt);

            assertThat(email).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("Should return null if email is absent")
        void shouldReturnNullWhenEmailAbsent() {
            Jwt jwt = createJwtWithSubject("user-123");

            String email = keycloakJwtConverter.extractEmail(jwt);

            assertThat(email).isNull();
        }
    }

    @Nested
    @DisplayName("hasRole - Role verification")
    class HasRoleTests {

        @Test
        @DisplayName("Should return true if role is present")
        void shouldReturnTrueWhenRoleIsPresent() {
            Jwt jwt = createJwtWithDirectRoles(List.of("ADMIN", "USER"));

            boolean hasRole = keycloakJwtConverter.hasRole(jwt, "ADMIN");

            assertThat(hasRole).isTrue();
        }

        @Test
        @DisplayName("Should return false if role is absent")
        void shouldReturnFalseWhenRoleIsAbsent() {
            Jwt jwt = createJwtWithDirectRoles(List.of("USER"));

            boolean hasRole = keycloakJwtConverter.hasRole(jwt, "ADMIN");

            assertThat(hasRole).isFalse();
        }

        @Test
        @DisplayName("Should work with or without ROLE_ prefix")
        void shouldWorkWithOrWithoutRolePrefix() {
            Jwt jwt = createJwtWithDirectRoles(List.of("ADMIN"));

            assertThat(keycloakJwtConverter.hasRole(jwt, "ADMIN")).isTrue();
            assertThat(keycloakJwtConverter.hasRole(jwt, "ROLE_ADMIN")).isTrue();
        }

        @Test
        @DisplayName("Should verify roles in realm_access")
        void shouldCheckRolesInRealmAccess() {
            Jwt jwt = createJwtWithRealmAccessRoles(List.of("SUPER_ADMIN"));

            boolean hasRole = keycloakJwtConverter.hasRole(jwt, "SUPER_ADMIN");

            assertThat(hasRole).isTrue();
        }

        @Test
        @DisplayName("Should return false if no role is present")
        void shouldReturnFalseWhenNoRolesPresent() {
            Jwt jwt = createJwtWithoutRoles();

            boolean hasRole = keycloakJwtConverter.hasRole(jwt, "ADMIN");

            assertThat(hasRole).isFalse();
        }
    }

    @Nested
    @DisplayName("Edge case handling")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle a 'roles' claim that is not a list")
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
        @DisplayName("Should handle a 'realm_access' claim without 'roles'")
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
        @DisplayName("Should handle a 'realm_access.roles' claim that is not a list")
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
        @DisplayName("Should handle empty role lists")
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
