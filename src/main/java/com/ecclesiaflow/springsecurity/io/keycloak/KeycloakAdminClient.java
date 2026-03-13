package com.ecclesiaflow.springsecurity.io.keycloak;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Map;

/**
 * Client for Keycloak Admin REST API using Feign.
 * Creates and manages users in Keycloak realm.
 */
@Service
@RequiredArgsConstructor
public class KeycloakAdminClient {

    private final KeycloakTokenFeignClient tokenClient;
    private final KeycloakAdminFeignClient adminClient;

    @Value("${keycloak.admin.realm}")
    private String realm;

    @Value("${keycloak.admin.service.client-id}")
    private String adminClientId;

    @Value("${keycloak.admin.service.client-secret}")
    private String adminClientSecret;

    @Value("${keycloak.direct-grant.client-id}")
    private String directGrantClientId;

    @Value("${keycloak.direct-grant.client-secret}")
    private String directGrantClientSecret;

    private volatile String cachedAccessToken;
    private volatile long tokenExpiresAt = 0;

    /**
     * Creates a user in Keycloak with the specified email and password.
     *
     * @param email    user email (also used as username)
     * @param password user password
     * @return Keycloak user ID
     * @throws KeycloakException if user creation fails
     * @deprecated Use {@link #createUser(String, String, boolean)} instead
     */
    @Deprecated
    public String createUser(String email, String password) {
        return createUser(email, password, true);
    }

    /**
     * Creates a user in Keycloak with the specified email, password and email verification status.
     *
     * @param email         user email (also used as username)
     * @param password      user password
     * @param emailVerified whether email is verified
     * @return Keycloak user ID
     * @throws KeycloakException if user creation fails
     */
    public String createUser(String email, String password, boolean emailVerified) {
        String accessToken = getAdminAccessToken();

        KeycloakUserRepresentation user = KeycloakUserRepresentation.builder()
                .username(email)
                .email(email)
                .emailVerified(emailVerified)
                .enabled(true)
                .credentials(List.of(KeycloakCredential.builder()
                        .type("password")
                        .value(password)
                        .temporary(false)
                        .build()))
                .build();

        ResponseEntity<Void> response = adminClient.createUser(
                "Bearer " + accessToken, realm, user);

        if (response.getStatusCode().is2xxSuccessful()) {
            String locationHeader = response.getHeaders().getFirst("Location");
            if (locationHeader != null) {
                return locationHeader.substring(locationHeader.lastIndexOf('/') + 1);
            }
        }

        throw new KeycloakException("Failed to create user in Keycloak: " + response.getStatusCode());
    }

    /**
     * Updates user password in Keycloak.
     *
     * @param keycloakUserId Keycloak user ID
     * @param newPassword    new password
     * @throws KeycloakException if password update fails
     */
    public void updatePassword(String keycloakUserId, String newPassword) {
        String accessToken = getAdminAccessToken();

        KeycloakCredential credential = KeycloakCredential.builder()
                .type("password")
                .value(newPassword)
                .temporary(false)
                .build();

        adminClient.resetPassword("Bearer " + accessToken, realm, keycloakUserId, credential);
    }

    /**
     * Finds a user by email in Keycloak.
     *
     * @param email user email
     * @return Keycloak user ID or null if not found
     */
    public String findUserByEmail(String email) {
        String accessToken = getAdminAccessToken();

        List<Map<String, Object>> users = adminClient.findUsersByEmail(
                "Bearer " + accessToken, realm, email, true);

        if (users != null && !users.isEmpty()) {
            return (String) users.getFirst().get("id");
        }

        return null;
    }

    /**
     * Finds a user by username in Keycloak (preferred over email search).
     * Since username=email in our setup, this is more precise.
     *
     * @param username user username (email)
     * @return Keycloak user ID or null if not found
     */
    public String findUserByUsername(String username) {
        String accessToken = getAdminAccessToken();

        List<Map<String, Object>> users = adminClient.findUsersByUsername(
                "Bearer " + accessToken, realm, username, true);

        if (users != null && !users.isEmpty()) {
            return (String) users.getFirst().get("id");
        }

        return null;
    }

    /**
     * Authenticates a user via Direct Grant (Resource Owner Password Credentials).
     * Used server-to-server only, for initial password setup auto-login.
     * NOT for regular login — use OIDC/PKCE via frontend for that.
     *
     * @param email    user email (username)
     * @param password user password
     * @return Keycloak token response with access_token, refresh_token, expires_in
     * @throws KeycloakException if authentication fails
     */
    public KeycloakTokenResponse authenticateUser(String email, String password) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", directGrantClientId);
        form.add("client_secret", directGrantClientSecret);
        form.add("username", email);
        form.add("password", password);

        KeycloakTokenResponse response = tokenClient.getToken(realm, form);

        if (response != null && response.accessToken() != null) {
            return response;
        }

        throw new KeycloakException("Failed to authenticate user via Direct Grant");
    }

    /**
     * Gets admin access token with caching.
     * Reuses cached token if not expired, otherwise fetches new one.
     *
     * @return valid admin access token
     * @throws KeycloakException if token acquisition fails
     */
    private String getAdminAccessToken() {
        long now = System.currentTimeMillis();

        if (cachedAccessToken != null && now < (tokenExpiresAt - 30000)) {
            return cachedAccessToken;
        }

        synchronized (this) {
            if (cachedAccessToken != null && now < (tokenExpiresAt - 30000)) {
                return cachedAccessToken;
            }

            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "client_credentials");
            form.add("client_id", adminClientId);
            form.add("client_secret", adminClientSecret);

            KeycloakTokenResponse response = tokenClient.getToken(realm, form);

            if (response != null && response.accessToken() != null) {
                cachedAccessToken = response.accessToken();
                tokenExpiresAt = now + (response.expiresIn() * 1000L);
                return cachedAccessToken;
            }

            throw new KeycloakException("Failed to obtain admin access token");
        }
    }

    /**
     * Deletes a user from Keycloak. Used for compensation when downstream
     * operations fail after user creation.
     *
     * @param keycloakUserId Keycloak user ID to delete
     * @throws KeycloakException if deletion fails
     */
    /** Disables a Keycloak user (sets enabled=false). */
    public void disableUser(String keycloakUserId) {
        String accessToken = getAdminAccessToken();
        adminClient.updateUser("Bearer " + accessToken, realm, keycloakUserId,
                Map.of("enabled", false));
    }

    /** Updates a Keycloak user's email and username. */
    public void updateUserEmail(String keycloakUserId, String newEmail) {
        String accessToken = getAdminAccessToken();
        adminClient.updateUser("Bearer " + accessToken, realm, keycloakUserId,
                Map.of("email", newEmail, "username", newEmail));
    }

    public void deleteUser(String keycloakUserId) {
        String accessToken = getAdminAccessToken();
        adminClient.deleteUser("Bearer " + accessToken, realm, keycloakUserId);
    }

    public static class KeycloakException extends RuntimeException {
        public KeycloakException(String message) {
            super(message);
        }

        public KeycloakException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
