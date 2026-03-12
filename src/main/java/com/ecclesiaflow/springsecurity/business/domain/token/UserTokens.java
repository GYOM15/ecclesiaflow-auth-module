package com.ecclesiaflow.springsecurity.business.domain.token;

/**
 * Domain object representing authentication tokens for auto-login after password setup.
 */
public record UserTokens(
        String accessToken,
        String refreshToken,
        int expiresIn
) {}
