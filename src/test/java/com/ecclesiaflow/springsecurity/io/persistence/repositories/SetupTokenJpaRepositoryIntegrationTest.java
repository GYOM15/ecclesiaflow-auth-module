package com.ecclesiaflow.springsecurity.io.persistence.repositories;

import com.ecclesiaflow.springsecurity.business.domain.token.SetupToken;
import com.ecclesiaflow.springsecurity.io.persistence.jpa.SetupTokenEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for SetupTokenJpaRepository.
 * Uses H2 in-memory database to validate JPA queries and entity mapping.
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("SetupTokenJpaRepository - Integration Tests")
class SetupTokenJpaRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SetupTokenJpaRepository repository;

    private static final String EMAIL = "user@ecclesiaflow.com";
    private static final UUID MEMBER_ID = UUID.randomUUID();

    private SetupTokenEntity createAndPersistToken(String tokenHash,
                                                    SetupToken.TokenStatus status,
                                                    LocalDateTime expiresAt) {
        SetupTokenEntity entity = SetupTokenEntity.builder()
                .tokenHash(tokenHash)
                .email(EMAIL)
                .memberId(MEMBER_ID)
                .purpose(SetupToken.TokenPurpose.PASSWORD_SETUP)
                .status(status)
                .expiresAt(expiresAt)
                .build();
        return entityManager.persistAndFlush(entity);
    }

    @Nested
    @DisplayName("findValidToken")
    class FindValidTokenTests {

        @Test
        @DisplayName("Should find a valid issued token that has not expired")
        void shouldFindValidIssuedToken() {
            String hash = "valid-hash-123";
            LocalDateTime future = LocalDateTime.now().plusHours(24);
            createAndPersistToken(hash, SetupToken.TokenStatus.ISSUED, future);

            Optional<SetupTokenEntity> result = repository.findValidToken(
                    hash, SetupToken.TokenStatus.ISSUED, LocalDateTime.now());

            assertThat(result).isPresent();
            assertThat(result.get().getTokenHash()).isEqualTo(hash);
            assertThat(result.get().getEmail()).isEqualTo(EMAIL);
        }

        @Test
        @DisplayName("Should return empty for expired token")
        void shouldReturnEmptyForExpiredToken() {
            String hash = "expired-hash-456";
            LocalDateTime past = LocalDateTime.now().minusHours(1);
            createAndPersistToken(hash, SetupToken.TokenStatus.ISSUED, past);

            Optional<SetupTokenEntity> result = repository.findValidToken(
                    hash, SetupToken.TokenStatus.ISSUED, LocalDateTime.now());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return empty for revoked token")
        void shouldReturnEmptyForRevokedToken() {
            String hash = "revoked-hash-789";
            LocalDateTime future = LocalDateTime.now().plusHours(24);
            createAndPersistToken(hash, SetupToken.TokenStatus.REVOKED, future);

            Optional<SetupTokenEntity> result = repository.findValidToken(
                    hash, SetupToken.TokenStatus.ISSUED, LocalDateTime.now());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return empty for non-existent hash")
        void shouldReturnEmptyForNonExistentHash() {
            Optional<SetupTokenEntity> result = repository.findValidToken(
                    "non-existent", SetupToken.TokenStatus.ISSUED, LocalDateTime.now());

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("revokeTokensForMember")
    class RevokeTokensForMemberTests {

        @Test
        @DisplayName("Should revoke all issued tokens for a member")
        void shouldRevokeAllIssuedTokensForMember() {
            LocalDateTime future = LocalDateTime.now().plusHours(24);
            createAndPersistToken("hash-a", SetupToken.TokenStatus.ISSUED, future);
            createAndPersistToken("hash-b", SetupToken.TokenStatus.ISSUED, future);

            int revoked = repository.revokeTokensForMember(
                    MEMBER_ID, SetupToken.TokenStatus.REVOKED, SetupToken.TokenStatus.ISSUED);

            assertThat(revoked).isEqualTo(2);
        }

        @Test
        @DisplayName("Should not revoke tokens of other members")
        void shouldNotRevokeTokensOfOtherMembers() {
            UUID otherMember = UUID.randomUUID();
            LocalDateTime future = LocalDateTime.now().plusHours(24);
            createAndPersistToken("hash-c", SetupToken.TokenStatus.ISSUED, future);

            int revoked = repository.revokeTokensForMember(
                    otherMember, SetupToken.TokenStatus.REVOKED, SetupToken.TokenStatus.ISSUED);

            assertThat(revoked).isZero();
        }

        @Test
        @DisplayName("Should not revoke already revoked tokens")
        void shouldNotRevokeAlreadyRevokedTokens() {
            LocalDateTime future = LocalDateTime.now().plusHours(24);
            createAndPersistToken("hash-d", SetupToken.TokenStatus.REVOKED, future);

            int revoked = repository.revokeTokensForMember(
                    MEMBER_ID, SetupToken.TokenStatus.REVOKED, SetupToken.TokenStatus.ISSUED);

            assertThat(revoked).isZero();
        }
    }

    @Nested
    @DisplayName("existsByMemberIdAndStatusAndExpiresAtAfter")
    class ExistsValidTokenTests {

        @Test
        @DisplayName("Should return true when valid token exists for member")
        void shouldReturnTrueWhenValidTokenExists() {
            LocalDateTime future = LocalDateTime.now().plusHours(24);
            createAndPersistToken("hash-e", SetupToken.TokenStatus.ISSUED, future);

            boolean exists = repository.existsByMemberIdAndStatusAndExpiresAtAfter(
                    MEMBER_ID, SetupToken.TokenStatus.ISSUED, LocalDateTime.now());

            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("Should return false when no valid token exists")
        void shouldReturnFalseWhenNoValidTokenExists() {
            boolean exists = repository.existsByMemberIdAndStatusAndExpiresAtAfter(
                    UUID.randomUUID(), SetupToken.TokenStatus.ISSUED, LocalDateTime.now());

            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("Should return false when token is expired")
        void shouldReturnFalseWhenTokenIsExpired() {
            LocalDateTime past = LocalDateTime.now().minusHours(1);
            createAndPersistToken("hash-f", SetupToken.TokenStatus.ISSUED, past);

            boolean exists = repository.existsByMemberIdAndStatusAndExpiresAtAfter(
                    MEMBER_ID, SetupToken.TokenStatus.ISSUED, LocalDateTime.now());

            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("Entity persistence")
    class EntityPersistenceTests {

        @Test
        @DisplayName("Should auto-generate UUID on persist")
        void shouldAutoGenerateUuidOnPersist() {
            SetupTokenEntity entity = SetupTokenEntity.builder()
                    .tokenHash("gen-uuid-hash")
                    .email(EMAIL)
                    .memberId(MEMBER_ID)
                    .purpose(SetupToken.TokenPurpose.PASSWORD_SETUP)
                    .status(SetupToken.TokenStatus.ISSUED)
                    .expiresAt(LocalDateTime.now().plusHours(24))
                    .build();

            SetupTokenEntity saved = entityManager.persistAndFlush(entity);

            assertThat(saved.getId()).isNotNull();
        }

        @Test
        @DisplayName("Should set createdAt timestamp automatically")
        void shouldSetCreatedAtAutomatically() {
            SetupTokenEntity entity = SetupTokenEntity.builder()
                    .tokenHash("timestamp-hash")
                    .email(EMAIL)
                    .memberId(MEMBER_ID)
                    .purpose(SetupToken.TokenPurpose.PASSWORD_SETUP)
                    .status(SetupToken.TokenStatus.ISSUED)
                    .expiresAt(LocalDateTime.now().plusHours(24))
                    .build();

            SetupTokenEntity saved = entityManager.persistAndFlush(entity);

            assertThat(saved.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should enforce unique constraint on tokenHash")
        void shouldEnforceUniqueTokenHash() {
            createAndPersistToken("unique-hash", SetupToken.TokenStatus.ISSUED,
                    LocalDateTime.now().plusHours(24));

            SetupTokenEntity duplicate = SetupTokenEntity.builder()
                    .tokenHash("unique-hash")
                    .email("other@test.com")
                    .memberId(UUID.randomUUID())
                    .purpose(SetupToken.TokenPurpose.PASSWORD_SETUP)
                    .status(SetupToken.TokenStatus.ISSUED)
                    .expiresAt(LocalDateTime.now().plusHours(24))
                    .build();

            org.assertj.core.api.Assertions.assertThatThrownBy(
                    () -> entityManager.persistAndFlush(duplicate)
            ).isInstanceOf(Exception.class);
        }
    }
}
