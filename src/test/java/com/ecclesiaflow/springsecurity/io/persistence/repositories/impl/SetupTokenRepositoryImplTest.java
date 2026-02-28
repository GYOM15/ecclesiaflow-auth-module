package com.ecclesiaflow.springsecurity.io.persistence.repositories.impl;

import com.ecclesiaflow.springsecurity.business.domain.token.SetupToken;
import com.ecclesiaflow.springsecurity.io.persistence.jpa.SetupTokenEntity;
import com.ecclesiaflow.springsecurity.io.persistence.mappers.SetupTokenPersistenceMapper;
import com.ecclesiaflow.springsecurity.io.persistence.repositories.SetupTokenJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SetupTokenRepositoryImpl - Tests Unitaires")
class SetupTokenRepositoryImplTest {

    @Mock
    private SetupTokenJpaRepository jpaRepository;

    @Mock
    private SetupTokenPersistenceMapper mapper;

    @InjectMocks
    private SetupTokenRepositoryImpl setupTokenRepository;

    private static final String TOKEN_HASH = "hashed-token";
    private static final String EMAIL = "user@test.com";
    private static final UUID MEMBER_ID = UUID.randomUUID();
    private static final UUID TOKEN_ID = UUID.randomUUID();

    @Nested
    @DisplayName("findValidToken - Success scenarios")
    class FindValidTokenSuccessTests {

        @Test
        @DisplayName("Should find valid token successfully")
        void shouldFindValidTokenSuccessfully() {
            LocalDateTime now = LocalDateTime.now();
            SetupTokenEntity entity = createEntity();
            SetupToken domain = createDomain();

            when(jpaRepository.findValidToken(TOKEN_HASH, SetupToken.TokenStatus.ISSUED, now)).thenReturn(Optional.of(entity));
            when(mapper.toDomainOrThrow(entity)).thenReturn(domain);

            Optional<SetupToken> result = setupTokenRepository.findValidToken(TOKEN_HASH, now);

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(domain);
            verify(jpaRepository).findValidToken(TOKEN_HASH, SetupToken.TokenStatus.ISSUED, now);
            verify(mapper).toDomainOrThrow(entity);
        }

        @Test
        @DisplayName("Should return empty when token not found")
        void shouldReturnEmptyWhenTokenNotFound() {
            LocalDateTime now = LocalDateTime.now();

            when(jpaRepository.findValidToken(TOKEN_HASH, SetupToken.TokenStatus.ISSUED, now)).thenReturn(Optional.empty());

            Optional<SetupToken> result = setupTokenRepository.findValidToken(TOKEN_HASH, now);

            assertThat(result).isEmpty();
            verify(jpaRepository).findValidToken(TOKEN_HASH, SetupToken.TokenStatus.ISSUED, now);
            verify(mapper, never()).toDomainOrThrow(any());
        }

        @Test
        @DisplayName("Should pass correct parameters to JPA repository")
        void shouldPassCorrectParametersToJpaRepository() {
            LocalDateTime now = LocalDateTime.now();

            when(jpaRepository.findValidToken(TOKEN_HASH, SetupToken.TokenStatus.ISSUED, now)).thenReturn(Optional.empty());

            setupTokenRepository.findValidToken(TOKEN_HASH, now);

            verify(jpaRepository).findValidToken(eq(TOKEN_HASH), eq(SetupToken.TokenStatus.ISSUED), eq(now));
        }
    }

    @Nested
    @DisplayName("save - Success scenarios")
    class SaveSuccessTests {

        @Test
        @DisplayName("Should save token successfully")
        void shouldSaveTokenSuccessfully() {
            SetupToken domain = createDomain();
            SetupTokenEntity entity = createEntity();
            SetupTokenEntity savedEntity = createEntity();
            SetupToken savedDomain = createDomain();

            when(mapper.toEntity(domain)).thenReturn(entity);
            when(jpaRepository.save(entity)).thenReturn(savedEntity);
            when(mapper.toDomainOrThrow(savedEntity)).thenReturn(savedDomain);

            SetupToken result = setupTokenRepository.save(domain);

            assertThat(result).isEqualTo(savedDomain);
            verify(mapper).toEntity(domain);
            verify(jpaRepository).save(entity);
            verify(mapper).toDomainOrThrow(savedEntity);
        }

        @Test
        @DisplayName("Should convert domain to entity before saving")
        void shouldConvertDomainToEntityBeforeSaving() {
            SetupToken domain = createDomain();
            SetupTokenEntity entity = createEntity();
            SetupTokenEntity savedEntity = createEntity();

            when(mapper.toEntity(domain)).thenReturn(entity);
            when(jpaRepository.save(entity)).thenReturn(savedEntity);
            when(mapper.toDomainOrThrow(savedEntity)).thenReturn(domain);

            setupTokenRepository.save(domain);

            verify(mapper).toEntity(domain);
        }

        @Test
        @DisplayName("Should convert saved entity back to domain")
        void shouldConvertSavedEntityBackToDomain() {
            SetupToken domain = createDomain();
            SetupTokenEntity entity = createEntity();
            SetupTokenEntity savedEntity = createEntity();

            when(mapper.toEntity(domain)).thenReturn(entity);
            when(jpaRepository.save(entity)).thenReturn(savedEntity);
            when(mapper.toDomainOrThrow(savedEntity)).thenReturn(domain);

            setupTokenRepository.save(domain);

            verify(mapper).toDomainOrThrow(savedEntity);
        }
    }

    @Nested
    @DisplayName("delete - Success scenarios")
    class DeleteSuccessTests {

        @Test
        @DisplayName("Should delete token successfully")
        void shouldDeleteTokenSuccessfully() {
            SetupToken domain = createDomain();
            SetupTokenEntity entity = createEntity();

            when(mapper.toEntityOrThrow(domain)).thenReturn(entity);

            setupTokenRepository.delete(domain);

            verify(mapper).toEntityOrThrow(domain);
            verify(jpaRepository).delete(entity);
        }

        @Test
        @DisplayName("Should convert domain to entity before deleting")
        void shouldConvertDomainToEntityBeforeDeleting() {
            SetupToken domain = createDomain();
            SetupTokenEntity entity = createEntity();

            when(mapper.toEntityOrThrow(domain)).thenReturn(entity);

            setupTokenRepository.delete(domain);

            verify(mapper).toEntityOrThrow(domain);
        }

        @Test
        @DisplayName("Should call JPA repository delete")
        void shouldCallJpaRepositoryDelete() {
            SetupToken domain = createDomain();
            SetupTokenEntity entity = createEntity();

            when(mapper.toEntityOrThrow(domain)).thenReturn(entity);

            setupTokenRepository.delete(domain);

            verify(jpaRepository).delete(entity);
        }
    }

    @Nested
    @DisplayName("revokeTokensForMember - Success scenarios")
    class RevokeTokensForMemberSuccessTests {

        @Test
        @DisplayName("Should revoke tokens for member successfully")
        void shouldRevokeTokensForMemberSuccessfully() {
            int revokedCount = 3;

            when(jpaRepository.revokeTokensForMember(MEMBER_ID, SetupToken.TokenStatus.REVOKED, SetupToken.TokenStatus.ISSUED)).thenReturn(revokedCount);

            int result = setupTokenRepository.revokeTokensForMember(MEMBER_ID);

            assertThat(result).isEqualTo(revokedCount);
            verify(jpaRepository).revokeTokensForMember(MEMBER_ID, SetupToken.TokenStatus.REVOKED, SetupToken.TokenStatus.ISSUED);
        }

        @Test
        @DisplayName("Should return zero when no tokens revoked")
        void shouldReturnZeroWhenNoTokensRevoked() {
            when(jpaRepository.revokeTokensForMember(MEMBER_ID, SetupToken.TokenStatus.REVOKED, SetupToken.TokenStatus.ISSUED)).thenReturn(0);

            int result = setupTokenRepository.revokeTokensForMember(MEMBER_ID);

            assertThat(result).isZero();
        }

        @Test
        @DisplayName("Should pass correct member ID to JPA repository")
        void shouldPassCorrectMemberIdToJpaRepository() {
            when(jpaRepository.revokeTokensForMember(MEMBER_ID, SetupToken.TokenStatus.REVOKED, SetupToken.TokenStatus.ISSUED)).thenReturn(1);

            setupTokenRepository.revokeTokensForMember(MEMBER_ID);

            verify(jpaRepository).revokeTokensForMember(eq(MEMBER_ID), eq(SetupToken.TokenStatus.REVOKED), eq(SetupToken.TokenStatus.ISSUED));
        }
    }

    @Nested
    @DisplayName("existsValidTokenForMember - Success scenarios")
    class ExistsValidTokenForMemberSuccessTests {

        @Test
        @DisplayName("Should return true when valid token exists")
        void shouldReturnTrueWhenValidTokenExists() {
            LocalDateTime now = LocalDateTime.now();

            when(jpaRepository.existsByMemberIdAndStatusAndExpiresAtAfter(
                    MEMBER_ID, SetupToken.TokenStatus.ISSUED, now))
                    .thenReturn(true);

            boolean result = setupTokenRepository.existsValidTokenForMember(MEMBER_ID, now);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when no valid token exists")
        void shouldReturnFalseWhenNoValidTokenExists() {
            LocalDateTime now = LocalDateTime.now();

            when(jpaRepository.existsByMemberIdAndStatusAndExpiresAtAfter(
                    MEMBER_ID, SetupToken.TokenStatus.ISSUED, now))
                    .thenReturn(false);

            boolean result = setupTokenRepository.existsValidTokenForMember(MEMBER_ID, now);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should pass correct parameters to JPA repository")
        void shouldPassCorrectParametersToJpaRepository() {
            LocalDateTime now = LocalDateTime.now();

            when(jpaRepository.existsByMemberIdAndStatusAndExpiresAtAfter(
                    MEMBER_ID, SetupToken.TokenStatus.ISSUED, now))
                    .thenReturn(false);

            setupTokenRepository.existsValidTokenForMember(MEMBER_ID, now);

            verify(jpaRepository).existsByMemberIdAndStatusAndExpiresAtAfter(
                    eq(MEMBER_ID), 
                    eq(SetupToken.TokenStatus.ISSUED), 
                    eq(now)
            );
        }
    }

    @Nested
    @DisplayName("Integration with mapper")
    class MapperIntegrationTests {

        @Test
        @DisplayName("Should use mapper for all conversions")
        void shouldUseMapperForAllConversions() {
            SetupToken domain = createDomain();
            SetupTokenEntity entity = createEntity();
            SetupTokenEntity savedEntity = createEntity();

            when(mapper.toEntity(domain)).thenReturn(entity);
            when(jpaRepository.save(entity)).thenReturn(savedEntity);
            when(mapper.toDomainOrThrow(savedEntity)).thenReturn(domain);

            setupTokenRepository.save(domain);

            verify(mapper).toEntity(domain);
            verify(mapper).toDomainOrThrow(savedEntity);
        }

        @Test
        @DisplayName("Should not call mapper when JPA returns empty")
        void shouldNotCallMapperWhenJpaReturnsEmpty() {
            LocalDateTime now = LocalDateTime.now();

            when(jpaRepository.findValidToken(TOKEN_HASH, SetupToken.TokenStatus.ISSUED, now)).thenReturn(Optional.empty());

            setupTokenRepository.findValidToken(TOKEN_HASH, now);

            verify(mapper, never()).toDomainOrThrow(any());
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle different member IDs")
        void shouldHandleDifferentMemberIds() {
            UUID memberId1 = UUID.randomUUID();
            UUID memberId2 = UUID.randomUUID();

            when(jpaRepository.revokeTokensForMember(memberId1, SetupToken.TokenStatus.REVOKED, SetupToken.TokenStatus.ISSUED)).thenReturn(1);
            when(jpaRepository.revokeTokensForMember(memberId2, SetupToken.TokenStatus.REVOKED, SetupToken.TokenStatus.ISSUED)).thenReturn(2);

            int result1 = setupTokenRepository.revokeTokensForMember(memberId1);
            int result2 = setupTokenRepository.revokeTokensForMember(memberId2);

            assertThat(result1).isEqualTo(1);
            assertThat(result2).isEqualTo(2);
        }

        @Test
        @DisplayName("Should handle multiple save operations")
        void shouldHandleMultipleSaveOperations() {
            SetupToken domain1 = createDomain();
            SetupToken domain2 = createDomain();
            SetupTokenEntity entity = createEntity();

            when(mapper.toEntity(any())).thenReturn(entity);
            when(jpaRepository.save(entity)).thenReturn(entity);
            when(mapper.toDomainOrThrow(entity)).thenReturn(domain1, domain2);

            setupTokenRepository.save(domain1);
            setupTokenRepository.save(domain2);

            verify(jpaRepository, times(2)).save(entity);
        }
    }

    private SetupToken createDomain() {
        return SetupToken.builder()
                .id(TOKEN_ID)
                .tokenHash(TOKEN_HASH)
                .email(EMAIL)
                .memberId(MEMBER_ID)
                .purpose(SetupToken.TokenPurpose.PASSWORD_SETUP)
                .status(SetupToken.TokenStatus.ISSUED)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .createdAt(LocalDateTime.now())
                .build();
    }

    private SetupTokenEntity createEntity() {
        SetupTokenEntity entity = new SetupTokenEntity();
        entity.setId(TOKEN_ID);
        entity.setTokenHash(TOKEN_HASH);
        entity.setEmail(EMAIL);
        entity.setMemberId(MEMBER_ID);
        entity.setPurpose(SetupToken.TokenPurpose.PASSWORD_SETUP);
        entity.setStatus(SetupToken.TokenStatus.ISSUED);
        entity.setExpiresAt(LocalDateTime.now().plusHours(24));
        entity.setCreatedAt(LocalDateTime.now());
        return entity;
    }
}
