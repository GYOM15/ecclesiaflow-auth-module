package com.ecclesiaflow.springsecurity.web.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Converts Keycloak JWT tokens to Spring Security authentication tokens.
 * <p>
 * Supports both direct "roles" claim (custom Keycloak mapper) and nested
 * "realm_access.roles" claim (default Keycloak structure).
 * </p>
 *
 * @see org.springframework.core.convert.converter.Converter
 */
@Component
public class KeycloakJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String ROLE_PREFIX = "ROLE_";
    private static final String ROLES_CLAIM = "roles";
    private static final String REALM_ACCESS_CLAIM = "realm_access";

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
        return new JwtAuthenticationToken(jwt, authorities, extractPrincipalName(jwt));
    }

    /**
     * Extracts authorities from both direct and nested role claims.
     *
     * @param jwt the JWT token
     * @return collection of granted authorities
     */
    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        return Stream.concat(
                extractDirectRoles(jwt).stream(),
                extractRealmAccessRoles(jwt).stream()
        )
        .distinct()
        .collect(Collectors.toList());
    }

    /**
     * Extracts roles from direct "roles" claim (custom Keycloak mapper).
     */
    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractDirectRoles(Jwt jwt) {
        Object rolesObj = jwt.getClaim(ROLES_CLAIM);
        if (rolesObj instanceof List) {
            return ((List<String>) rolesObj).stream()
                    .map(role -> new SimpleGrantedAuthority(prefixRole(role)))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * Extracts roles from nested "realm_access.roles" claim (default Keycloak structure).
     */
    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractRealmAccessRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim(REALM_ACCESS_CLAIM);
        if (realmAccess == null) {
            return Collections.emptyList();
        }

        Object rolesObj = realmAccess.get("roles");
        if (rolesObj instanceof List) {
            return ((List<String>) rolesObj).stream()
                    .map(role -> new SimpleGrantedAuthority(prefixRole(role)))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * Adds ROLE_ prefix if not already present.
     */
    private String prefixRole(String role) {
        return role.startsWith(ROLE_PREFIX) ? role : ROLE_PREFIX + role;
    }

    /**
     * Extracts principal name, preferring email over subject.
     */
    private String extractPrincipalName(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        return (email != null && !email.isBlank()) ? email : jwt.getSubject();
    }

    /**
     * Extracts user ID from JWT.
     *
     * @param jwt the JWT token
     * @return the user ID (subject claim)
     */
    public String extractUserId(Jwt jwt) {
        return jwt.getSubject();
    }

    /**
     * Extracts email from JWT.
     *
     * @param jwt the JWT token
     * @return the email claim value
     */
    public String extractEmail(Jwt jwt) {
        return jwt.getClaimAsString("email");
    }

    /**
     * Checks if the JWT contains a specific role.
     *
     * @param jwt the JWT token
     * @param role the role name to check
     * @return true if the role is present
     */
    public boolean hasRole(Jwt jwt, String role) {
        return extractAuthorities(jwt).stream()
                .anyMatch(auth -> auth.getAuthority().equals(prefixRole(role)));
    }
}
