package com.ecclesiaflow.springsecurity.business.domain.token;

import lombok.Builder;
import lombok.Getter;


import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain object representing an opaque setup/reset token.
 * Pure POJO without any infrastructure concerns.
 * Token is stored as SHA-256 hash for security.
 */
@Getter
@Builder(toBuilder = true)
public class SetupToken {

    private final UUID id;
    private final String tokenHash;
    private final String email;
    private final UUID memberId;
    private final TokenPurpose purpose;
    private final TokenStatus status;
    private final LocalDateTime expiresAt;
    private final LocalDateTime createdAt;

    public enum TokenPurpose {
        PASSWORD_SETUP
    }

    public enum TokenStatus {
        ISSUED,
        EXPIRED,
        REVOKED
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return status == TokenStatus.ISSUED && !isExpired();
    }

}
