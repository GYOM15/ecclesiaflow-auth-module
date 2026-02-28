package com.ecclesiaflow.springsecurity.business.domain.token;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SetupToken - Tests Unitaires")
class SetupTokenTest {

    private static final UUID ID = UUID.randomUUID();
    private static final String TOKEN_HASH = "hashed-token";
    private static final String EMAIL = "user@test.com";
    private static final UUID MEMBER_ID = UUID.randomUUID();

    @Nested
    @DisplayName("Builder and Getters")
    class BuilderAndGettersTests {

        @Test
        @DisplayName("Should build token with all properties")
        void shouldBuildTokenWithAllProperties() {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiresAt = now.plusHours(24);
            LocalDateTime consumedAt = now.plusHours(1);

            SetupToken token = SetupToken.builder()
                    .id(ID)
                    .tokenHash(TOKEN_HASH)
                    .email(EMAIL)
                    .memberId(MEMBER_ID)
                    .purpose(SetupToken.TokenPurpose.PASSWORD_SETUP)
                    .status(SetupToken.TokenStatus.ISSUED)
                    .expiresAt(expiresAt)
                    .createdAt(now)
                    .build();

            assertThat(token.getId()).isEqualTo(ID);
            assertThat(token.getTokenHash()).isEqualTo(TOKEN_HASH);
            assertThat(token.getEmail()).isEqualTo(EMAIL);
            assertThat(token.getMemberId()).isEqualTo(MEMBER_ID);
            assertThat(token.getPurpose()).isEqualTo(SetupToken.TokenPurpose.PASSWORD_SETUP);
            assertThat(token.getStatus()).isEqualTo(SetupToken.TokenStatus.ISSUED);
            assertThat(token.getExpiresAt()).isEqualTo(expiresAt);
            assertThat(token.getCreatedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("Should support toBuilder for immutable updates")
        void shouldSupportToBuilderForImmutableUpdates() {
            SetupToken original = SetupToken.builder()
                    .id(ID)
                    .tokenHash(TOKEN_HASH)
                    .email(EMAIL)
                    .memberId(MEMBER_ID)
                    .purpose(SetupToken.TokenPurpose.PASSWORD_SETUP)
                    .status(SetupToken.TokenStatus.ISSUED)
                    .expiresAt(LocalDateTime.now().plusHours(24))
                    .createdAt(LocalDateTime.now())
                    .build();

            SetupToken modified = original.toBuilder()
                    .status(SetupToken.TokenStatus.REVOKED)
                    .build();

            assertThat(modified.getStatus()).isEqualTo(SetupToken.TokenStatus.REVOKED);
            assertThat(modified.getId()).isEqualTo(original.getId());
            assertThat(modified.getTokenHash()).isEqualTo(original.getTokenHash());
            assertThat(original.getStatus()).isEqualTo(SetupToken.TokenStatus.ISSUED);
        }
    }

    @Nested
    @DisplayName("isExpired - Expiration logic")
    class IsExpiredTests {

        @Test
        @DisplayName("Should return false when token is not expired")
        void shouldReturnFalseWhenTokenNotExpired() {
            SetupToken token = SetupToken.builder()
                    .id(ID)
                    .tokenHash(TOKEN_HASH)
                    .email(EMAIL)
                    .memberId(MEMBER_ID)
                    .purpose(SetupToken.TokenPurpose.PASSWORD_SETUP)
                    .status(SetupToken.TokenStatus.ISSUED)
                    .expiresAt(LocalDateTime.now().plusHours(24))
                    .createdAt(LocalDateTime.now())
                    .build();

            assertThat(token.isExpired()).isFalse();
        }

        @Test
        @DisplayName("Should return true when token is expired")
        void shouldReturnTrueWhenTokenExpired() {
            SetupToken token = SetupToken.builder()
                    .id(ID)
                    .tokenHash(TOKEN_HASH)
                    .email(EMAIL)
                    .memberId(MEMBER_ID)
                    .purpose(SetupToken.TokenPurpose.PASSWORD_SETUP)
                    .status(SetupToken.TokenStatus.ISSUED)
                    .expiresAt(LocalDateTime.now().minusHours(1))
                    .createdAt(LocalDateTime.now().minusHours(25))
                    .build();

            assertThat(token.isExpired()).isTrue();
        }

        @Test
        @DisplayName("Should return true when token expires exactly now")
        void shouldReturnTrueWhenTokenExpiresExactlyNow() {
            SetupToken token = SetupToken.builder()
                    .id(ID)
                    .tokenHash(TOKEN_HASH)
                    .email(EMAIL)
                    .memberId(MEMBER_ID)
                    .purpose(SetupToken.TokenPurpose.PASSWORD_SETUP)
                    .status(SetupToken.TokenStatus.ISSUED)
                    .expiresAt(LocalDateTime.now().minusNanos(1))
                    .createdAt(LocalDateTime.now().minusHours(24))
                    .build();

            assertThat(token.isExpired()).isTrue();
        }

        @Test
        @DisplayName("Should handle token expiring in far future")
        void shouldHandleTokenExpiringInFarFuture() {
            SetupToken token = SetupToken.builder()
                    .id(ID)
                    .tokenHash(TOKEN_HASH)
                    .email(EMAIL)
                    .memberId(MEMBER_ID)
                    .purpose(SetupToken.TokenPurpose.PASSWORD_SETUP)
                    .status(SetupToken.TokenStatus.ISSUED)
                    .expiresAt(LocalDateTime.now().plusYears(10))
                    .createdAt(LocalDateTime.now())
                    .build();

            assertThat(token.isExpired()).isFalse();
        }
    }

    @Nested
    @DisplayName("isValid - Validation logic")
    class IsValidTests {

        @Test
        @DisplayName("Should return true when token is issued and not expired")
        void shouldReturnTrueWhenTokenIsIssuedAndNotExpired() {
            SetupToken token = SetupToken.builder()
                    .id(ID)
                    .tokenHash(TOKEN_HASH)
                    .email(EMAIL)
                    .memberId(MEMBER_ID)
                    .purpose(SetupToken.TokenPurpose.PASSWORD_SETUP)
                    .status(SetupToken.TokenStatus.ISSUED)
                    .expiresAt(LocalDateTime.now().plusHours(24))
                    .createdAt(LocalDateTime.now())
                    .build();

            assertThat(token.isValid()).isTrue();
        }

        @Test
        @DisplayName("Should return false when token is expired even if issued")
        void shouldReturnFalseWhenTokenExpiredEvenIfIssued() {
            SetupToken token = SetupToken.builder()
                    .id(ID)
                    .tokenHash(TOKEN_HASH)
                    .email(EMAIL)
                    .memberId(MEMBER_ID)
                    .purpose(SetupToken.TokenPurpose.PASSWORD_SETUP)
                    .status(SetupToken.TokenStatus.ISSUED)
                    .expiresAt(LocalDateTime.now().minusHours(1))
                    .createdAt(LocalDateTime.now().minusHours(25))
                    .build();

            assertThat(token.isValid()).isFalse();
        }

        @Test
        @DisplayName("Should return false when token is revoked even if not expired")
        void shouldReturnFalseWhenTokenRevokedEvenIfNotExpired() {
            SetupToken token = SetupToken.builder()
                    .id(ID)
                    .tokenHash(TOKEN_HASH)
                    .email(EMAIL)
                    .memberId(MEMBER_ID)
                    .purpose(SetupToken.TokenPurpose.PASSWORD_SETUP)
                    .status(SetupToken.TokenStatus.REVOKED)
                    .expiresAt(LocalDateTime.now().plusHours(24))
                    .createdAt(LocalDateTime.now())
                    .build();

            assertThat(token.isValid()).isFalse();
        }

        @Test
        @DisplayName("Should return false when token status is expired")
        void shouldReturnFalseWhenTokenStatusIsExpired() {
            SetupToken token = SetupToken.builder()
                    .id(ID)
                    .tokenHash(TOKEN_HASH)
                    .email(EMAIL)
                    .memberId(MEMBER_ID)
                    .purpose(SetupToken.TokenPurpose.PASSWORD_SETUP)
                    .status(SetupToken.TokenStatus.EXPIRED)
                    .expiresAt(LocalDateTime.now().plusHours(24))
                    .createdAt(LocalDateTime.now())
                    .build();

            assertThat(token.isValid()).isFalse();
        }

        @Test
        @DisplayName("Should return false when both revoked and expired")
        void shouldReturnFalseWhenBothRevokedAndExpired() {
            SetupToken token = SetupToken.builder()
                    .id(ID)
                    .tokenHash(TOKEN_HASH)
                    .email(EMAIL)
                    .memberId(MEMBER_ID)
                    .purpose(SetupToken.TokenPurpose.PASSWORD_SETUP)
                    .status(SetupToken.TokenStatus.REVOKED)
                    .expiresAt(LocalDateTime.now().minusHours(1))
                    .createdAt(LocalDateTime.now().minusHours(25))
                    .build();

            assertThat(token.isValid()).isFalse();
        }
    }

    @Nested
    @DisplayName("TokenPurpose enum")
    class TokenPurposeTests {

        @Test
        @DisplayName("Should have PASSWORD_SETUP purpose")
        void shouldHavePasswordSetupPurpose() {
            assertThat(SetupToken.TokenPurpose.PASSWORD_SETUP).isNotNull();
        }

        @Test
        @DisplayName("Should be able to use purpose in token")
        void shouldBeAbleToUsePurposeInToken() {
            SetupToken token = SetupToken.builder()
                    .id(ID)
                    .tokenHash(TOKEN_HASH)
                    .email(EMAIL)
                    .memberId(MEMBER_ID)
                    .purpose(SetupToken.TokenPurpose.PASSWORD_SETUP)
                    .status(SetupToken.TokenStatus.ISSUED)
                    .expiresAt(LocalDateTime.now().plusHours(24))
                    .createdAt(LocalDateTime.now())
                    .build();

            assertThat(token.getPurpose()).isEqualTo(SetupToken.TokenPurpose.PASSWORD_SETUP);
        }
    }

    @Nested
    @DisplayName("TokenStatus enum")
    class TokenStatusTests {

        @Test
        @DisplayName("Should have all status values")
        void shouldHaveAllStatusValues() {
            assertThat(SetupToken.TokenStatus.ISSUED).isNotNull();
            assertThat(SetupToken.TokenStatus.EXPIRED).isNotNull();
            assertThat(SetupToken.TokenStatus.REVOKED).isNotNull();
        }

        @Test
        @DisplayName("Should be able to use all statuses in token")
        void shouldBeAbleToUseAllStatusesInToken() {
            SetupToken issuedToken = createTokenWithStatus(SetupToken.TokenStatus.ISSUED);
            SetupToken expiredToken = createTokenWithStatus(SetupToken.TokenStatus.EXPIRED);
            SetupToken revokedToken = createTokenWithStatus(SetupToken.TokenStatus.REVOKED);

            assertThat(issuedToken.getStatus()).isEqualTo(SetupToken.TokenStatus.ISSUED);
            assertThat(expiredToken.getStatus()).isEqualTo(SetupToken.TokenStatus.EXPIRED);
            assertThat(revokedToken.getStatus()).isEqualTo(SetupToken.TokenStatus.REVOKED);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle email with special characters")
        void shouldHandleEmailWithSpecialCharacters() {
            String specialEmail = "user+test@example.com";
            SetupToken token = SetupToken.builder()
                    .id(ID)
                    .tokenHash(TOKEN_HASH)
                    .email(specialEmail)
                    .memberId(MEMBER_ID)
                    .purpose(SetupToken.TokenPurpose.PASSWORD_SETUP)
                    .status(SetupToken.TokenStatus.ISSUED)
                    .expiresAt(LocalDateTime.now().plusHours(24))
                    .createdAt(LocalDateTime.now())
                    .build();

            assertThat(token.getEmail()).isEqualTo(specialEmail);
        }

        @Test
        @DisplayName("Should handle different member IDs")
        void shouldHandleDifferentMemberIds() {
            UUID memberId1 = UUID.randomUUID();
            UUID memberId2 = UUID.randomUUID();

            SetupToken token1 = createTokenWithMemberId(memberId1);
            SetupToken token2 = createTokenWithMemberId(memberId2);

            assertThat(token1.getMemberId()).isEqualTo(memberId1);
            assertThat(token2.getMemberId()).isEqualTo(memberId2);
            assertThat(token1.getMemberId()).isNotEqualTo(token2.getMemberId());
        }

        @Test
        @DisplayName("Should handle token with very long hash")
        void shouldHandleTokenWithVeryLongHash() {
            String longHash = "a".repeat(1000);
            SetupToken token = SetupToken.builder()
                    .id(ID)
                    .tokenHash(longHash)
                    .email(EMAIL)
                    .memberId(MEMBER_ID)
                    .purpose(SetupToken.TokenPurpose.PASSWORD_SETUP)
                    .status(SetupToken.TokenStatus.ISSUED)
                    .expiresAt(LocalDateTime.now().plusHours(24))
                    .createdAt(LocalDateTime.now())
                    .build();

            assertThat(token.getTokenHash()).isEqualTo(longHash);
            assertThat(token.getTokenHash()).hasSize(1000);
        }
    }

    private SetupToken createTokenWithStatus(SetupToken.TokenStatus status) {
        return SetupToken.builder()
                .id(ID)
                .tokenHash(TOKEN_HASH)
                .email(EMAIL)
                .memberId(MEMBER_ID)
                .purpose(SetupToken.TokenPurpose.PASSWORD_SETUP)
                .status(status)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .createdAt(LocalDateTime.now())
                .build();
    }

    private SetupToken createTokenWithMemberId(UUID memberId) {
        return SetupToken.builder()
                .id(ID)
                .tokenHash(TOKEN_HASH)
                .email(EMAIL)
                .memberId(memberId)
                .purpose(SetupToken.TokenPurpose.PASSWORD_SETUP)
                .status(SetupToken.TokenStatus.ISSUED)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .createdAt(LocalDateTime.now())
                .build();
    }
}
