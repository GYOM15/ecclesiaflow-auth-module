package com.ecclesiaflow.springsecurity.io.persistence.repositories.impl;

import com.ecclesiaflow.springsecurity.business.domain.token.SetupToken;
import com.ecclesiaflow.springsecurity.business.domain.token.SetupTokenRepository;
import com.ecclesiaflow.springsecurity.io.persistence.jpa.SetupTokenEntity;
import com.ecclesiaflow.springsecurity.io.persistence.mappers.SetupTokenPersistenceMapper;
import com.ecclesiaflow.springsecurity.io.persistence.repositories.SetupTokenJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of SetupTokenRepository using JPA.
 * Infrastructure layer - adapts JPA to domain interface.
 */
@Repository
@RequiredArgsConstructor
public class SetupTokenRepositoryImpl implements SetupTokenRepository {

    private final SetupTokenJpaRepository jpaRepository;
    private final SetupTokenPersistenceMapper mapper;

    @Override
    public Optional<SetupToken> findValidToken(String tokenHash, LocalDateTime now) {
        return jpaRepository.findValidToken(tokenHash, SetupToken.TokenStatus.ISSUED, now)
                .map(mapper::toDomainOrThrow);
    }

    @Override
    @Transactional
    public SetupToken save(SetupToken token) {
        SetupTokenEntity entity = mapper.toEntity(token);
        SetupTokenEntity saved = jpaRepository.save(entity);
        return mapper.toDomainOrThrow(saved);
    }

    @Override
    @Transactional
    public void delete(SetupToken token) {
        SetupTokenEntity entity = mapper.toEntityOrThrow(token);
        jpaRepository.delete(entity);
    }

    @Override
    @Transactional
    public int revokeTokensForMember(UUID memberId) {
        return jpaRepository.revokeTokensForMember(
                memberId, SetupToken.TokenStatus.REVOKED, SetupToken.TokenStatus.ISSUED);
    }

    @Override
    public boolean existsValidTokenForMember(UUID memberId, LocalDateTime now) {
        return jpaRepository.existsByMemberIdAndStatusAndExpiresAtAfter(
                memberId, SetupToken.TokenStatus.ISSUED, now);
    }
}
