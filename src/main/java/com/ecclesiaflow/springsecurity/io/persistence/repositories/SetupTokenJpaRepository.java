package com.ecclesiaflow.springsecurity.io.persistence.repositories;

import com.ecclesiaflow.springsecurity.business.domain.token.SetupToken;
import com.ecclesiaflow.springsecurity.io.persistence.jpa.SetupTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for SetupTokenEntity.
 * Infrastructure layer - handles database operations.
 */
public interface SetupTokenJpaRepository extends JpaRepository<SetupTokenEntity, UUID> {

    @Query("SELECT t FROM SetupTokenEntity t WHERE t.tokenHash = :hash AND t.status = :status AND t.expiresAt > :now")
    Optional<SetupTokenEntity> findValidToken(@Param("hash") String tokenHash, @Param("status") SetupToken.TokenStatus status, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE SetupTokenEntity t SET t.status = :revokedStatus WHERE t.memberId = :memberId AND t.status = :issuedStatus")
    int revokeTokensForMember(@Param("memberId") UUID memberId, @Param("revokedStatus") SetupToken.TokenStatus revokedStatus, @Param("issuedStatus") SetupToken.TokenStatus issuedStatus);

    boolean existsByMemberIdAndStatusAndExpiresAtAfter(UUID memberId, SetupToken.TokenStatus status, LocalDateTime now);
}
