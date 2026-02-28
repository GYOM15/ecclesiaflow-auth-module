package com.ecclesiaflow.springsecurity.business.domain.token;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain repository interface for SetupToken.
 * Pure domain interface without any infrastructure concerns.
 */
public interface SetupTokenRepository {

    Optional<SetupToken> findValidToken(String tokenHash, LocalDateTime now);

    SetupToken save(SetupToken token);

    void delete(SetupToken token);
    
    int revokeTokensForMember(UUID memberId);

    boolean existsValidTokenForMember(UUID memberId, LocalDateTime now);
}
