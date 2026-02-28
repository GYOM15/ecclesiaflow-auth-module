package com.ecclesiaflow.springsecurity.business.services;

import com.ecclesiaflow.springsecurity.business.domain.token.SetupToken;
import com.ecclesiaflow.springsecurity.business.exceptions.InvalidTokenException;

import java.util.UUID;

/**
 * Service for managing opaque setup/reset tokens.
 */
public interface SetupTokenService {

    /**
     * Generates an opaque token for password setup.
     *
     * @param email    member email
     * @param memberId member UUID
     * @return the generated opaque token (raw, not hashed)
     */
    String generateSetupToken(String email, UUID memberId);

    /**
     * Validates a token
     *
     * @param rawToken the raw token received from client
     * @param purpose  expected purpose (PASSWORD_SETUP)
     * @return the validated SetupToken entity
     * @throws InvalidTokenException if token is invalid, expired, or already consumed
     */
    SetupToken validate(String rawToken, SetupToken.TokenPurpose purpose);

    /**
     * Deletes a token after successful operation.
     * Should only be called after all operations have succeeded.
     *
     * @param token the token to delete
     */
    void deleteToken(SetupToken token);
}
