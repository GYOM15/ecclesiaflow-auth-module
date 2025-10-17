package com.ecclesiaflow.springsecurity.web.security;

import com.ecclesiaflow.springsecurity.business.domain.member.Member;
import com.ecclesiaflow.springsecurity.business.domain.member.Role;
import com.ecclesiaflow.springsecurity.business.domain.token.UserTokens;
import com.ecclesiaflow.springsecurity.business.services.mappers.ScopeMapper;
import com.ecclesiaflow.springsecurity.web.exception.InvalidTokenException;
import com.ecclesiaflow.springsecurity.web.exception.JwtProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour l'utilitaire Jwt.
 * Se concentre sur la vérification des interactions avec JwtProcessor (via Mockito)
 * et de la gestion des exceptions.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Jwt - Tests d'Orchestration et d'Exceptions")
class JwtTest {

    @Mock
    private JwtProcessor jwtProcessor;

    @Mock
    private ScopeMapper scopeMapper;

    @InjectMocks
    private Jwt jwt;

    // Constantes de test
    private static final String TEST_EMAIL = "test@ecclesiaflow.com";
    private static final UUID TEST_MEMBER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final String ACCESS_TOKEN = "mock.access.token";
    private static final String REFRESH_TOKEN = "mock.refresh.token";
    private static final String TEMP_TOKEN = "mock.temp.token";
    private Member mockMember;
    private Set<String> memberScopes;

    @BeforeEach
    void setUp() {
        // Construction d'un mockMember avec le builder de l'objet de domaine pur
        mockMember = Member.builder()
                .id(UUID.randomUUID())
                .memberId(TEST_MEMBER_ID)
                .email(TEST_EMAIL)
                .password("hashedPassword") // Doit être haché dans le domaine
                .createdAt(LocalDateTime.now())
                .role(Role.MEMBER)
                .enabled(true)
                .build();

        memberScopes = Set.of("ef:members:read:own", "ef:profile:read:own");
        when(scopeMapper.mapRoleToScopes(Role.MEMBER)).thenReturn(memberScopes);
    }

    // ====================================================================
    // Tests de Génération de Tokens (generateUserTokens)
    // ====================================================================

    @Test
    @DisplayName("generateUserTokens - Devrait appeler le processor pour créer les deux tokens et retourner UserTokens")
    void generateUserTokens_ShouldCallProcessorAndReturnUserTokens() throws JwtProcessingException {
        // Arrange
        when(jwtProcessor.generateAccessToken(any(UserDetails.class), eq(TEST_MEMBER_ID), eq(memberScopes)))
                .thenReturn(ACCESS_TOKEN);
        when(jwtProcessor.generateRefreshToken(any(UserDetails.class))).thenReturn(REFRESH_TOKEN);

        // Act
        UserTokens result = jwt.generateUserTokens(mockMember);

        // Assert
        verify(scopeMapper).mapRoleToScopes(Role.MEMBER);
        verify(jwtProcessor, times(1)).generateAccessToken(any(UserDetails.class), eq(TEST_MEMBER_ID), eq(memberScopes));
        verify(jwtProcessor, times(1)).generateRefreshToken(any(UserDetails.class));
        assertThat(result.accessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(result.refreshToken()).isEqualTo(REFRESH_TOKEN);
    }

    @Test
    @DisplayName("generateUserTokens - Devrait propager JwtProcessingException en cas d'échec de génération")
    void generateUserTokens_ShouldPropagateException() throws JwtProcessingException {
        // Arrange
        doThrow(new JwtProcessingException("Generation failed"))
                .when(jwtProcessor)
                .generateAccessToken(any(UserDetails.class), eq(TEST_MEMBER_ID), eq(memberScopes));

        // Act & Assert
        assertThatThrownBy(() -> jwt.generateUserTokens(mockMember))
                .isInstanceOf(JwtProcessingException.class)
                .hasMessage("Generation failed");
    }

    // ====================================================================
    // Tests de Validation et d'Extraction (validateAndExtractEmail)
    // ====================================================================

    @Test
    @DisplayName("validateAndExtractEmail - Devrait valider le token et extraire l'email")
    void validateAndExtractEmail_ShouldValidateAndExtract() throws InvalidTokenException, JwtProcessingException {
        // Arrange
        when(jwtProcessor.isRefreshTokenValid(REFRESH_TOKEN)).thenReturn(true);
        when(jwtProcessor.extractUsername(REFRESH_TOKEN)).thenReturn(TEST_EMAIL);

        // Act
        String resultEmail = jwt.validateAndExtractEmail(REFRESH_TOKEN);

        // Assert
        verify(jwtProcessor, times(1)).isRefreshTokenValid(REFRESH_TOKEN);
        verify(jwtProcessor, times(1)).extractUsername(REFRESH_TOKEN);
        assertThat(resultEmail).isEqualTo(TEST_EMAIL);
    }

    @Test
    @DisplayName("validateAndExtractEmail - Devrait lancer InvalidTokenException si le processor retourne false")
    void validateAndExtractEmail_ShouldThrowIfTokenIsInvalid() throws InvalidTokenException, JwtProcessingException {
        // Arrange
        when(jwtProcessor.isRefreshTokenValid(REFRESH_TOKEN)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> jwt.validateAndExtractEmail(REFRESH_TOKEN))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Le token de rafraîchissement est invalide ou expiré.");
        verify(jwtProcessor, never()).extractUsername(anyString());
    }

    @Test
    @DisplayName("validateAndExtractEmail - Devrait propager JwtProcessingException du processor")
    void validateAndExtractEmail_ShouldPropagateJwtProcessingException() throws InvalidTokenException, JwtProcessingException {
        // Arrange
        doThrow(new JwtProcessingException("JWS error")).when(jwtProcessor).isRefreshTokenValid(REFRESH_TOKEN);

        // Act & Assert
        assertThatThrownBy(() -> jwt.validateAndExtractEmail(REFRESH_TOKEN))
                .isInstanceOf(JwtProcessingException.class)
                .hasMessage("JWS error");
    }

    // ====================================================================
    // Tests de Rafraîchissement (refreshTokenForMember)
    // ====================================================================

    @Test
    @DisplayName("refreshTokenForMember - Devrait générer un nouvel access token et conserver le refresh token")
    void refreshTokenForMember_ShouldGenerateNewAccessToken() throws JwtProcessingException {
        // Arrange
        String newAccessToken = "new.mock.access.token";
        when(jwtProcessor.generateAccessToken(any(UserDetails.class), eq(TEST_MEMBER_ID), eq(memberScopes)))
                .thenReturn(newAccessToken);

        // Act
        UserTokens result = jwt.refreshTokenForMember(REFRESH_TOKEN, mockMember);

        // Assert
        verify(scopeMapper, atLeastOnce()).mapRoleToScopes(Role.MEMBER);
        verify(jwtProcessor, times(1)).generateAccessToken(any(UserDetails.class), eq(TEST_MEMBER_ID), eq(memberScopes));
        assertThat(result.accessToken()).isEqualTo(newAccessToken);
        assertThat(result.refreshToken()).isEqualTo(REFRESH_TOKEN);
    }

    // ====================================================================
    // Tests de Token Temporaire (generateTemporaryToken)
    // ====================================================================

    @Test
    @DisplayName("generateTemporaryToken - Devrait déléguer au processor et retourner le token")
    void generateTemporaryToken_ShouldDelegateAndReturnToken() throws JwtProcessingException {
        // Arrange
        when(jwtProcessor.generateTemporaryToken(TEST_EMAIL, TEST_MEMBER_ID)).thenReturn(TEMP_TOKEN);

        // Act
        String resultToken = jwt.generateTemporaryToken(TEST_EMAIL, TEST_MEMBER_ID);

        // Assert
        verify(jwtProcessor, times(1)).generateTemporaryToken(TEST_EMAIL, TEST_MEMBER_ID);
        assertThat(resultToken).isEqualTo(TEMP_TOKEN);
    }

    // ====================================================================
    // Tests de Validation de Token Temporaire (validateTemporaryToken)
    // ====================================================================

    @Test
    @DisplayName("validateTemporaryToken - Devrait déléguer au processor et retourner le résultat de validation")
    void validateTemporaryToken_ShouldDelegateAndReturnResult() {
        // Arrange
        when(jwtProcessor.validateTemporaryToken(TEMP_TOKEN, TEST_EMAIL)).thenReturn(true);

        // Act
        boolean isValid = jwt.validateTemporaryToken(TEMP_TOKEN, TEST_EMAIL);

        // Assert
        verify(jwtProcessor, times(1)).validateTemporaryToken(TEMP_TOKEN, TEST_EMAIL);
        assertThat(isValid).isTrue();
    }

    // ====================================================================
    // Tests d'Extraction de Token Temporaire (extractEmailFromTemporaryToken)
    // ====================================================================

    @Test
    @DisplayName("extractEmailFromTemporaryToken - Devrait déléguer l'extraction de l'email")
    void extractEmailFromTemporaryToken_ShouldDelegateExtraction() throws JwtProcessingException {
        // Arrange
        when(jwtProcessor.extractUsername(TEMP_TOKEN)).thenReturn(TEST_EMAIL);

        // Act
        String resultEmail = jwt.extractEmailFromTemporaryToken(TEMP_TOKEN);

        // Assert
        verify(jwtProcessor, times(1)).extractUsername(TEMP_TOKEN);
        assertThat(resultEmail).isEqualTo(TEST_EMAIL);
    }

    @Test
    @DisplayName("extractEmailFromTemporaryToken - Devrait propager JwtProcessingException en cas d'échec d'extraction")
    void extractEmailFromTemporaryToken_ShouldPropagateException() throws JwtProcessingException {
        // Arrange
        doThrow(new JwtProcessingException("Extraction failed")).when(jwtProcessor).extractUsername(TEMP_TOKEN);

        // Act & Assert
        assertThatThrownBy(() -> jwt.extractEmailFromTemporaryToken(TEMP_TOKEN))
                .isInstanceOf(JwtProcessingException.class)
                .hasMessage("Extraction failed");
    }
}