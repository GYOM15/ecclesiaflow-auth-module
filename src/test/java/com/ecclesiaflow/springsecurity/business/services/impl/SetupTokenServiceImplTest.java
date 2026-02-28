package com.ecclesiaflow.springsecurity.business.services.impl;

import com.ecclesiaflow.springsecurity.business.domain.token.SetupToken;
import com.ecclesiaflow.springsecurity.business.domain.token.SetupTokenRepository;
import com.ecclesiaflow.springsecurity.business.exceptions.InvalidTokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SetupTokenServiceImpl - Tests Unitaires")
class SetupTokenServiceImplTest {

    @Mock
    private SetupTokenRepository setupTokenRepository;

    @InjectMocks
    private SetupTokenServiceImpl setupTokenService;

    private static final String EMAIL = "user@test.com";
    private static final UUID MEMBER_ID = UUID.randomUUID();
    private static final int SETUP_TOKEN_TTL_HOURS = 24;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(setupTokenService, "setupTokenTtlHours", SETUP_TOKEN_TTL_HOURS);
    }

    @Nested
    @DisplayName("generateSetupToken - Success scenarios")
    class GenerateSetupTokenSuccessTests {

        @Test
        @DisplayName("Should generate setup token successfully")
        void shouldGenerateSetupTokenSuccessfully() {
            String token = setupTokenService.generateSetupToken(EMAIL, MEMBER_ID);

            assertThat(token).isNotNull();
            assertThat(token).isNotEmpty();
            verify(setupTokenRepository).revokeTokensForMember(MEMBER_ID);
            verify(setupTokenRepository).save(any(SetupToken.class));
        }

        @Test
        @DisplayName("Should revoke existing tokens before generating new one")
        void shouldRevokeExistingTokensBeforeGenerating() {
            setupTokenService.generateSetupToken(EMAIL, MEMBER_ID);

            var inOrder = inOrder(setupTokenRepository);
            inOrder.verify(setupTokenRepository).revokeTokensForMember(MEMBER_ID);
            inOrder.verify(setupTokenRepository).save(any(SetupToken.class));
        }

        @Test
        @DisplayName("Should save token with correct properties")
        void shouldSaveTokenWithCorrectProperties() {
            setupTokenService.generateSetupToken(EMAIL, MEMBER_ID);

            ArgumentCaptor<SetupToken> tokenCaptor = ArgumentCaptor.forClass(SetupToken.class);
            verify(setupTokenRepository).save(tokenCaptor.capture());
            
            SetupToken savedToken = tokenCaptor.getValue();
            assertThat(savedToken.getEmail()).isEqualTo(EMAIL);
            assertThat(savedToken.getMemberId()).isEqualTo(MEMBER_ID);
            assertThat(savedToken.getPurpose()).isEqualTo(SetupToken.TokenPurpose.PASSWORD_SETUP);
            assertThat(savedToken.getStatus()).isEqualTo(SetupToken.TokenStatus.ISSUED);
            assertThat(savedToken.getTokenHash()).isNotNull();
            assertThat(savedToken.getCreatedAt()).isNotNull();
            assertThat(savedToken.getExpiresAt()).isNotNull();
        }

        @Test
        @DisplayName("Should set expiration to configured TTL hours")
        void shouldSetExpirationToConfiguredTtl() {
            LocalDateTime beforeGeneration = LocalDateTime.now();
            
            setupTokenService.generateSetupToken(EMAIL, MEMBER_ID);

            ArgumentCaptor<SetupToken> tokenCaptor = ArgumentCaptor.forClass(SetupToken.class);
            verify(setupTokenRepository).save(tokenCaptor.capture());
            
            SetupToken savedToken = tokenCaptor.getValue();
            LocalDateTime expectedExpiration = beforeGeneration.plusHours(SETUP_TOKEN_TTL_HOURS);
            
            assertThat(savedToken.getExpiresAt()).isAfter(beforeGeneration);
            assertThat(savedToken.getExpiresAt()).isBeforeOrEqualTo(expectedExpiration.plusMinutes(1));
        }

        @Test
        @DisplayName("Should generate different tokens for same user")
        void shouldGenerateDifferentTokensForSameUser() {
            String token1 = setupTokenService.generateSetupToken(EMAIL, MEMBER_ID);
            String token2 = setupTokenService.generateSetupToken(EMAIL, MEMBER_ID);

            assertThat(token1).isNotEqualTo(token2);
        }

        @Test
        @DisplayName("Should generate URL-safe token")
        void shouldGenerateUrlSafeToken() {
            String token = setupTokenService.generateSetupToken(EMAIL, MEMBER_ID);

            assertThat(token).matches("^[A-Za-z0-9_-]+$");
        }

        @Test
        @DisplayName("Should hash token before storing")
        void shouldHashTokenBeforeStoring() {
            String rawToken = setupTokenService.generateSetupToken(EMAIL, MEMBER_ID);

            ArgumentCaptor<SetupToken> tokenCaptor = ArgumentCaptor.forClass(SetupToken.class);
            verify(setupTokenRepository).save(tokenCaptor.capture());
            
            SetupToken savedToken = tokenCaptor.getValue();
            assertThat(savedToken.getTokenHash()).isNotEqualTo(rawToken);
        }
    }

    @Nested
    @DisplayName("validate - Success scenarios")
    class ValidateSuccessTests {

        @Test
        @DisplayName("Should validate token successfully")
        void shouldValidateTokenSuccessfully() {
            String rawToken = "test-token";
            SetupToken expectedToken = createValidToken();
            
            when(setupTokenRepository.findValidToken(anyString(), any(LocalDateTime.class)))
                    .thenReturn(Optional.of(expectedToken));

            SetupToken result = setupTokenService.validate(rawToken, SetupToken.TokenPurpose.PASSWORD_SETUP);

            assertThat(result).isEqualTo(expectedToken);
            verify(setupTokenRepository).findValidToken(anyString(), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Should hash token before validation")
        void shouldHashTokenBeforeValidation() {
            String rawToken = "test-token";
            SetupToken expectedToken = createValidToken();
            
            when(setupTokenRepository.findValidToken(anyString(), any(LocalDateTime.class)))
                    .thenReturn(Optional.of(expectedToken));

            setupTokenService.validate(rawToken, SetupToken.TokenPurpose.PASSWORD_SETUP);

            ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
            verify(setupTokenRepository).findValidToken(hashCaptor.capture(), any(LocalDateTime.class));
            
            assertThat(hashCaptor.getValue()).isNotEqualTo(rawToken);
        }

        @Test
        @DisplayName("Should pass current time to repository")
        void shouldPassCurrentTimeToRepository() {
            String rawToken = "test-token";
            SetupToken expectedToken = createValidToken();
            LocalDateTime beforeValidation = LocalDateTime.now();
            
            when(setupTokenRepository.findValidToken(anyString(), any(LocalDateTime.class)))
                    .thenReturn(Optional.of(expectedToken));

            setupTokenService.validate(rawToken, SetupToken.TokenPurpose.PASSWORD_SETUP);

            ArgumentCaptor<LocalDateTime> timeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(setupTokenRepository).findValidToken(anyString(), timeCaptor.capture());
            
            assertThat(timeCaptor.getValue()).isAfterOrEqualTo(beforeValidation);
            assertThat(timeCaptor.getValue()).isBeforeOrEqualTo(LocalDateTime.now().plusSeconds(1));
        }
    }

    @Nested
    @DisplayName("validate - Failure scenarios")
    class ValidateFailureTests {

        @Test
        @DisplayName("Should throw InvalidTokenException when token not found")
        void shouldThrowExceptionWhenTokenNotFound() {
            String rawToken = "invalid-token";
            
            when(setupTokenRepository.findValidToken(anyString(), any(LocalDateTime.class)))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> setupTokenService.validate(rawToken, SetupToken.TokenPurpose.PASSWORD_SETUP))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("Token is invalid or expired");
        }

        @Test
        @DisplayName("Should throw InvalidTokenException when purpose mismatch")
        void shouldThrowExceptionWhenPurposeMismatch() {
            String rawToken = "test-token";
            
            SetupToken mockToken = mock(SetupToken.class);
            when(mockToken.getPurpose()).thenReturn(null);
            
            when(setupTokenRepository.findValidToken(anyString(), any(LocalDateTime.class)))
                    .thenReturn(Optional.of(mockToken));

            assertThatThrownBy(() -> setupTokenService.validate(rawToken, SetupToken.TokenPurpose.PASSWORD_SETUP))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("Token purpose mismatch");
        }

        @Test
        @DisplayName("Should not call repository delete when validation fails")
        void shouldNotDeleteWhenValidationFails() {
            String rawToken = "invalid-token";
            
            when(setupTokenRepository.findValidToken(anyString(), any(LocalDateTime.class)))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> setupTokenService.validate(rawToken, SetupToken.TokenPurpose.PASSWORD_SETUP))
                    .isInstanceOf(InvalidTokenException.class);

            verify(setupTokenRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("deleteToken - Success scenarios")
    class DeleteTokenSuccessTests {

        @Test
        @DisplayName("Should delete token successfully")
        void shouldDeleteTokenSuccessfully() {
            SetupToken token = createValidToken();

            setupTokenService.deleteToken(token);

            verify(setupTokenRepository).delete(token);
        }

        @Test
        @DisplayName("Should call repository delete exactly once")
        void shouldCallRepositoryDeleteExactlyOnce() {
            SetupToken token = createValidToken();

            setupTokenService.deleteToken(token);

            verify(setupTokenRepository, times(1)).delete(token);
        }

        @Test
        @DisplayName("Should pass correct token to repository")
        void shouldPassCorrectTokenToRepository() {
            SetupToken token = createValidToken();

            setupTokenService.deleteToken(token);

            ArgumentCaptor<SetupToken> tokenCaptor = ArgumentCaptor.forClass(SetupToken.class);
            verify(setupTokenRepository).delete(tokenCaptor.capture());
            
            assertThat(tokenCaptor.getValue()).isEqualTo(token);
        }
    }

    @Nested
    @DisplayName("Token hashing")
    class TokenHashingTests {

        @Test
        @DisplayName("Should generate consistent hash for same token")
        void shouldGenerateConsistentHashForSameToken() {
            String token1 = setupTokenService.generateSetupToken(EMAIL, MEMBER_ID);
            String token2 = setupTokenService.generateSetupToken(EMAIL, MEMBER_ID);

            assertThat(token1).isNotEqualTo(token2);
        }

        @Test
        @DisplayName("Should use SHA-256 for hashing")
        void shouldUseSha256ForHashing() {
            setupTokenService.generateSetupToken(EMAIL, MEMBER_ID);

            ArgumentCaptor<SetupToken> tokenCaptor = ArgumentCaptor.forClass(SetupToken.class);
            verify(setupTokenRepository).save(tokenCaptor.capture());
            
            String hash = tokenCaptor.getValue().getTokenHash();
            assertThat(hash).isNotNull();
            assertThat(hash).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle email with special characters")
        void shouldHandleEmailWithSpecialCharacters() {
            String specialEmail = "user+test@example.com";

            String token = setupTokenService.generateSetupToken(specialEmail, MEMBER_ID);

            assertThat(token).isNotNull();
            
            ArgumentCaptor<SetupToken> tokenCaptor = ArgumentCaptor.forClass(SetupToken.class);
            verify(setupTokenRepository).save(tokenCaptor.capture());
            assertThat(tokenCaptor.getValue().getEmail()).isEqualTo(specialEmail);
        }

        @Test
        @DisplayName("Should handle token validation with expired token")
        void shouldHandleExpiredTokenValidation() {
            String rawToken = "expired-token";
            SetupToken expiredToken = SetupToken.builder()
                    .id(UUID.randomUUID())
                    .tokenHash("hashed-token")
                    .email(EMAIL)
                    .memberId(MEMBER_ID)
                    .purpose(SetupToken.TokenPurpose.PASSWORD_SETUP)
                    .status(SetupToken.TokenStatus.EXPIRED)
                    .expiresAt(LocalDateTime.now().minusHours(1))
                    .createdAt(LocalDateTime.now().minusHours(25))
                    .build();
            
            when(setupTokenRepository.findValidToken(anyString(), any(LocalDateTime.class)))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> setupTokenService.validate(rawToken, SetupToken.TokenPurpose.PASSWORD_SETUP))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("Token is invalid or expired");
        }

        @Test
        @DisplayName("Should handle different member IDs")
        void shouldHandleDifferentMemberIds() {
            UUID memberId1 = UUID.randomUUID();
            UUID memberId2 = UUID.randomUUID();

            String token1 = setupTokenService.generateSetupToken(EMAIL, memberId1);
            String token2 = setupTokenService.generateSetupToken(EMAIL, memberId2);

            assertThat(token1).isNotNull();
            assertThat(token2).isNotNull();
            verify(setupTokenRepository).revokeTokensForMember(memberId1);
            verify(setupTokenRepository).revokeTokensForMember(memberId2);
        }

        @Test
        @DisplayName("Should handle custom TTL configuration")
        void shouldHandleCustomTtlConfiguration() {
            ReflectionTestUtils.setField(setupTokenService, "setupTokenTtlHours", 48);
            LocalDateTime beforeGeneration = LocalDateTime.now();

            setupTokenService.generateSetupToken(EMAIL, MEMBER_ID);

            ArgumentCaptor<SetupToken> tokenCaptor = ArgumentCaptor.forClass(SetupToken.class);
            verify(setupTokenRepository).save(tokenCaptor.capture());
            
            SetupToken savedToken = tokenCaptor.getValue();
            LocalDateTime expectedExpiration = beforeGeneration.plusHours(48);
            
            assertThat(savedToken.getExpiresAt()).isAfter(beforeGeneration.plusHours(47));
            assertThat(savedToken.getExpiresAt()).isBeforeOrEqualTo(expectedExpiration.plusMinutes(1));
        }
    }

    private SetupToken createValidToken() {
        return SetupToken.builder()
                .id(UUID.randomUUID())
                .tokenHash("hashed-token")
                .email(EMAIL)
                .memberId(MEMBER_ID)
                .purpose(SetupToken.TokenPurpose.PASSWORD_SETUP)
                .status(SetupToken.TokenStatus.ISSUED)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .createdAt(LocalDateTime.now())
                .build();
    }
}
