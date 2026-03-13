package com.ecclesiaflow.springsecurity.business.services;

import com.ecclesiaflow.springsecurity.business.domain.token.UserTokens;

import java.util.Optional;

public interface PasswordService {

    /**
     * Sets up initial password for a new user after email confirmation.
     * After creating the Keycloak user, attempts Direct Grant authentication
     * for immediate auto-login.
     *
     * @param setupToken Opaque setup token from email
     * @param password New password
     * @return Optional containing tokens if Direct Grant succeeds, empty otherwise
     * @throws com.ecclesiaflow.springsecurity.web.exception.InvalidRequestException if token is invalid or email not confirmed
     */
    Optional<UserTokens> setupPassword(String setupToken, String password);

    /**
     * Adds a local password to an SSO user's Keycloak account
     * and notifies the Members module.
     *
     * @param keycloakUserId Keycloak user ID (JWT sub claim)
     * @param password       new local password
     */
    void addLocalCredentials(String keycloakUserId, String password);
}
