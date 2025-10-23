package com.ecclesiaflow.springsecurity.web.security;

import com.ecclesiaflow.springsecurity.web.exception.InvalidTokenException;
import com.ecclesiaflow.springsecurity.web.exception.JwtProcessingException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.data.Offset.offset; // Import pour la correction isCloseTo

/**
 * Tests unitaires pour JwtProcessor
 * 
 * Teste les fonctionnalités critiques de génération, validation et traitement
 * des tokens JWT pour garantir la sécurité du système d'authentification.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtProcessor - Tests de sécurité JWT")
class JwtProcessorTest {

    private JwtProcessor jwtProcessor;
    private TestUserDetails testUser;

    // Constantes de test au niveau de la classe
    private final String TEST_SECRET = "EcclesiaFlowTestSecretKeyFor512BitJwtSignature0123456789ABCDEF";
    private final long ACCESS_EXP = 3600000L; // 1 heure
    private final long REFRESH_EXP = 86400000L; // 24 heures
    private final long TEMP_EXP = 900000L; // 15 minutes

    @BeforeEach
    void setUp() {
        jwtProcessor = new JwtProcessor();

        // **Injection COHÉRENTE** des propriétés pour que les tokens générés puissent être parsés par parseClaims()
        ReflectionTestUtils.setField(jwtProcessor, "secretKey", TEST_SECRET);
        ReflectionTestUtils.setField(jwtProcessor, "accessTokenExpiration", ACCESS_EXP);
        ReflectionTestUtils.setField(jwtProcessor, "refreshTokenExpiration", REFRESH_EXP);
        ReflectionTestUtils.setField(jwtProcessor, "temporaryTokenExpiration", TEMP_EXP);

        testUser = new TestUserDetails("test@example.com", "password", List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    /**
     * Helper pour décoder et valider les claims.
     * Cette méthode utilise la constante de classe TEST_SECRET.
     */
    private Claims parseClaims(String token) {
        // La clé doit être décodée en bytes à partir de la chaîne BASE64 de TEST_SECRET
        Key key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET));
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }

    // ====================================================================
    // Tests de Génération et d'Extraction
    // ====================================================================

    @Test
    @DisplayName("Devrait générer un token d'accès valide")
    void shouldGenerateValidAccessToken() throws JwtProcessingException {
        // When
        String accessToken = jwtProcessor.generateAccessToken(testUser);

        // Then
        assertThat(accessToken).isNotNull();
        assertThat(accessToken.split("\\.")).hasSize(3);

        Claims claims = parseClaims(accessToken);
        assertThat(claims.getSubject()).isEqualTo(testUser.getUsername());
        // Correction isCloseTo
        assertThat(claims.getExpiration().getTime()).isCloseTo(System.currentTimeMillis() + ACCESS_EXP, offset(1000L));
    }

    @Test
    @DisplayName("Devrait générer un token d'accès avec scopes et memberId")
    void shouldGenerateAccessTokenWithScopesAndMemberId() throws JwtProcessingException, InvalidTokenException {
        // Given
        UUID memberId = UUID.randomUUID();
        Set<String> scopes = Set.of("ef:members:read:own", "ef:profile:read:own");

        // When
        String token = jwtProcessor.generateAccessToken(testUser, memberId, scopes);

        // Then
        Claims claims = parseClaims(token);
        assertThat(claims.get("cid", String.class)).isEqualTo(memberId.toString());
        String scopeString = claims.get("scope", String.class);
        assertThat(scopeString).isNotNull();
        assertThat(scopeString.split("\\s+")).containsExactlyInAnyOrder(scopes.toArray(new String[0]));

        assertThat(jwtProcessor.extractMemberId(token)).isEqualTo(memberId);
        assertThat(jwtProcessor.extractScopes(token)).containsExactlyInAnyOrderElementsOf(scopes);
    }

    @Test
    @DisplayName("Devrait générer un token d'accès avec memberId et scopes vides")
    void shouldGenerateAccessTokenWithEmptyScopes() throws JwtProcessingException, InvalidTokenException {
        // Given
        UUID memberId = UUID.randomUUID();

        // When
        String token = jwtProcessor.generateAccessToken(testUser, memberId, Set.of());

        // Then
        Claims claims = parseClaims(token);
        assertThat(claims.get("cid", String.class)).isEqualTo(memberId.toString());
        assertThat(claims.containsKey("scopes")).isFalse();
        assertThat(jwtProcessor.extractMemberId(token)).isEqualTo(memberId);
        assertThat(jwtProcessor.extractScopes(token)).isEmpty();
    }

    @Test
    @DisplayName("Devrait générer un token d'accès sans claims additionnels")
    void shouldGenerateAccessTokenWithoutExtraClaims() throws JwtProcessingException {
        // When
        String token = jwtProcessor.generateAccessToken(testUser, null, Set.of());

        // Then
        Claims claims = parseClaims(token);
        assertThat(claims.get("cid")).isNull();
        assertThat(claims.containsKey("scopes")).isFalse();
        assertThat(jwtProcessor.extractScopes(token)).isEmpty();
    }

    @Test
    @DisplayName("Devrait générer un token d'accès sans claims lorsque scopes est null")
    void shouldGenerateAccessTokenWithoutExtraClaimsWhenScopesNull() throws JwtProcessingException {
        // When
        String token = jwtProcessor.generateAccessToken(testUser, null, null);

        // Then
        Claims claims = parseClaims(token);
        assertThat(claims.get("cid")).isNull();
        assertThat(claims.containsKey("scopes")).isFalse();
        assertThat(jwtProcessor.extractScopes(token)).isEmpty();
    }

    @Test
    @DisplayName("Devrait générer un token d'accès avec uniquement le memberId")
    void shouldGenerateAccessTokenWithOnlyMemberId() throws JwtProcessingException, InvalidTokenException {
        // Given
        UUID memberId = UUID.randomUUID();

        // When
        String token = jwtProcessor.generateAccessToken(testUser, memberId, null);

        // Then
        Claims claims = parseClaims(token);
        assertThat(claims.get("cid", String.class)).isEqualTo(memberId.toString());
        assertThat(claims.containsKey("scopes")).isFalse();
        assertThat(jwtProcessor.extractMemberId(token)).isEqualTo(memberId);
        assertThat(jwtProcessor.extractScopes(token)).isEmpty();
    }

    @Test
    @DisplayName("Devrait générer un token d'accès avec uniquement les scopes")
    void shouldGenerateAccessTokenWithOnlyScopes() throws JwtProcessingException {
        // Given
        Set<String> scopes = Set.of("ef:members:read:own", "ef:members:write:own");

        // When
        String token = jwtProcessor.generateAccessToken(testUser, null, scopes);

        // Then
        Claims claims = parseClaims(token);
        assertThat(claims.get("cid")).isNull();
        String scopeString = claims.get("scope", String.class);
        assertThat(scopeString).isNotNull();
        assertThat(scopeString.split("\\s+")).containsExactlyInAnyOrder(scopes.toArray(new String[0]));
        assertThat(jwtProcessor.extractScopes(token)).containsExactlyInAnyOrderElementsOf(scopes);
    }

    @Test
    @DisplayName("Devrait générer un refresh token valide")
    void shouldGenerateValidRefreshToken() throws JwtProcessingException {
        // When
        String refreshToken = jwtProcessor.generateRefreshToken(testUser);

        // Then
        assertThat(refreshToken).isNotNull();
        assertThat(refreshToken.split("\\.")).hasSize(3);

        Claims claims = parseClaims(refreshToken);
        assertThat(claims.get("type", String.class)).isEqualTo("refresh");
        // Correction isCloseTo
        assertThat(claims.getExpiration().getTime()).isCloseTo(System.currentTimeMillis() + REFRESH_EXP, offset(1000L));
    }

    @Test
    @DisplayName("Devrait extraire le nom d'utilisateur du token")
    void shouldExtractUsernameFromToken() throws JwtProcessingException {
        // Given
        String token = jwtProcessor.generateAccessToken(testUser);

        // When
        String extractedUsername = jwtProcessor.extractUsername(token);

        // Then
        assertThat(extractedUsername).isEqualTo(testUser.getUsername());
    }

    // ====================================================================
    // Tests de Validation des Tokens Standards
    // ====================================================================

    @Test
    @DisplayName("Devrait valider un token d'accès valide")
    void shouldValidateValidAccessToken() throws JwtProcessingException {
        // Given
        String token = jwtProcessor.generateAccessToken(testUser);

        // When
        boolean isValid = jwtProcessor.isTokenValid(token);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Devrait valider un refresh token valide")
    void shouldValidateValidRefreshToken() throws JwtProcessingException, InvalidTokenException {
        // Given
        String refreshToken = jwtProcessor.generateRefreshToken(testUser);

        // When
        boolean isValid = jwtProcessor.isRefreshTokenValid(refreshToken);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Devrait rejeter un token d'accès comme refresh token")
    void shouldRejectAccessTokenAsRefreshToken() throws JwtProcessingException {
        // Given
        String accessToken = jwtProcessor.generateAccessToken(testUser);

        // When & Then
        assertThatThrownBy(() -> jwtProcessor.isRefreshTokenValid(accessToken))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Le token fourni n'est pas un token de rafraîchissement");
    }

    @Test
    @DisplayName("Devrait retourner liste vide si aucun scope")
    void shouldReturnEmptyScopesWhenMissing() throws JwtProcessingException {
        // Given
        String token = jwtProcessor.generateAccessToken(testUser);

        // When
        Set<String> scopes = jwtProcessor.extractScopes(token);

        // Then
        assertThat(scopes).isEmpty();
    }

    @Test
    @DisplayName("Devrait retourner un ensemble vide si le scope est blank")
    void shouldReturnEmptySetWhenScopeIsBlank() throws JwtProcessingException {
        // Given - Créer manuellement un token avec un scope blank
        Key key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET));
        String tokenWithBlankScope = Jwts.builder()
                .setSubject(testUser.getUsername())
                .claim("scope", "   ")  // Scope blank (espaces)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_EXP))
                .signWith(key)
                .compact();

        // When
        Set<String> scopes = jwtProcessor.extractScopes(tokenWithBlankScope);

        // Then
        assertThat(scopes).isEmpty();
    }

    // ====================================================================
    // Tests de Gestion des Erreurs
    // ====================================================================

    @Test
    @DisplayName("Devrait rejeter un token malformé")
    void shouldRejectMalformedToken() throws JwtProcessingException {
        // Given
        String malformedToken = "invalid.token.format";

        // When
        boolean isValid = jwtProcessor.isTokenValid(malformedToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Devrait rejeter un token avec signature invalide")
    void shouldRejectTokenWithInvalidSignature() throws JwtProcessingException {
        // Given
        String validToken = jwtProcessor.generateAccessToken(testUser);
        String tokenWithInvalidSignature = validToken.substring(0, validToken.lastIndexOf('.')) + ".invalidSignature";

        // When
        boolean isValid = jwtProcessor.isTokenValid(tokenWithInvalidSignature);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Devrait rejeter un token null")
    void shouldRejectNullToken() {
        // When & Then
        assertThatThrownBy(() -> jwtProcessor.isTokenValid(null))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Devrait rejeter un token vide")
    void shouldRejectEmptyToken() {
        // When & Then
        assertThatThrownBy(() -> jwtProcessor.isTokenValid(""))
                .isInstanceOf(RuntimeException.class);
    }

    // ====================================================================
    // Tests de Génération et Validation du Token Temporaire
    // ====================================================================

    @Test
    @DisplayName("generateTemporaryToken - Devrait créer un token valide avec les claims 'temporary' et 'password_setup'")
    void shouldGenerateValidTemporaryTokenWithCorrectClaims() throws JwtProcessingException {
        // Given
        String email = "temp@user.com";

        // When
        String token = jwtProcessor.generateTemporaryToken(email, UUID.randomUUID());
        Claims claims = parseClaims(token);

        // Then
        assertThat(token).isNotNull();
        assertThat(claims.getSubject()).isEqualTo(email);
        assertThat(claims.get("type", String.class)).isEqualTo("temporary");
        assertThat(claims.get("purpose", String.class)).isEqualTo("password_setup");

        // Correction isCloseTo avec tolérance augmentée pour éviter les échecs de timing
        assertThat(claims.getExpiration().getTime())
                .isCloseTo(System.currentTimeMillis() + TEMP_EXP, offset(2000L));
    }

    @Test
    @DisplayName("validateTemporaryToken - Devrait retourner true pour un token temporaire valide et le bon email")
    void shouldValidateValidTemporaryToken() throws JwtProcessingException, InvalidTokenException {
        // Given
        String email = "setup@email.com";
        String token = jwtProcessor.generateTemporaryToken(email, UUID.randomUUID());

        // When
        boolean isValid = jwtProcessor.validateTemporaryToken(token, email);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("validateTemporaryToken - Devrait retourner false si l'email ne correspond pas")
    void shouldRejectTemporaryTokenIfEmailMismatch() throws JwtProcessingException, InvalidTokenException {
        // Given
        String token = jwtProcessor.generateTemporaryToken("correct@email.com", UUID.randomUUID());

        // When
        boolean isValid = jwtProcessor.validateTemporaryToken(token, "wrong@email.com");

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("validateTemporaryToken - Devrait rejeter un token expiré")
    void shouldRejectExpiredTemporaryToken() throws JwtProcessingException {
        // Given
        String email = "expired@user.com";

        // 1. Décode la clé pour la signature
        Key key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET));

        // 2. Créer manuellement un token qui a expiré il y a 10 secondes
        long tenSecondsAgo = System.currentTimeMillis() - 10000;

        String expiredToken = Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new java.util.Date(tenSecondsAgo))
                // Définir l'expiration dans le passé (par exemple, 1 seconde dans le passé)
                .setExpiration(new java.util.Date(tenSecondsAgo + 1)) // <-- Expire il y a environ 10s
                .claim("type", "temporary")
                .claim("purpose", "password_setup")
                .signWith(key)
                .compact();

        // When & Then
        boolean isValid = jwtProcessor.validateTemporaryToken(expiredToken, email);

        // Assertion (le token doit être rejeté, donc false)
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("validateTemporaryToken - Devrait lancer InvalidTokenException si le token n'est pas de type 'temporary'")
    void shouldRejectWrongTypeTokenAsTemporaryToken() throws JwtProcessingException {
        // Given: Utiliser un access token standard
        String accessToken = jwtProcessor.generateAccessToken(testUser);

        // When & Then
        assertThatThrownBy(() -> jwtProcessor.validateTemporaryToken(accessToken, testUser.getUsername()))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Le token fourni n'est pas un token temporaire valide");
    }

    @Test
    @DisplayName("validateTemporaryToken - Devrait lancer InvalidTokenException pour token malformé")
    void shouldThrowInvalidTokenExceptionForMalformedTemporaryToken() {
        // Given
        String malformedToken = "invalid.malformed.token";

        // When & Then
        assertThatThrownBy(() -> jwtProcessor.validateTemporaryToken(malformedToken, "test@email.com"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Token invalide");
    }

    @Test
    @DisplayName("validateTemporaryToken - Devrait lancer InvalidTokenException pour token avec signature invalide")
    void shouldThrowInvalidTokenExceptionForInvalidSignature() throws JwtProcessingException {
        // Given
        String validToken = jwtProcessor.generateTemporaryToken("test@email.com", UUID.randomUUID());
        String tokenWithInvalidSignature = validToken.substring(0, validToken.lastIndexOf('.')) + ".invalidSignature";

        // When & Then
        assertThatThrownBy(() -> jwtProcessor.validateTemporaryToken(tokenWithInvalidSignature, "test@email.com"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Token invalide");
    }

    @Test
    @DisplayName("isRefreshTokenValid - Devrait retourner false pour refresh token expiré")
    void shouldReturnFalseForExpiredRefreshToken() throws JwtProcessingException, InvalidTokenException {
        // Given
        Key key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET));
        long tenSecondsAgo = System.currentTimeMillis() - 10000;

        String expiredRefreshToken = Jwts.builder()
                .setSubject("test@example.com")
                .setIssuedAt(new java.util.Date(tenSecondsAgo))
                .setExpiration(new java.util.Date(tenSecondsAgo + 1))
                .claim("type", "refresh")
                .signWith(key)
                .compact();

        // When
        boolean isValid = jwtProcessor.isRefreshTokenValid(expiredRefreshToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("isTokenValid - Devrait retourner false pour token expiré")
    void shouldReturnFalseForExpiredAccessToken() throws JwtProcessingException {
        // Given
        Key key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET));
        long tenSecondsAgo = System.currentTimeMillis() - 10000;

        String expiredToken = Jwts.builder()
                .setSubject("test@example.com")
                .setIssuedAt(new java.util.Date(tenSecondsAgo))
                .setExpiration(new java.util.Date(tenSecondsAgo + 1))
                .signWith(key)
                .compact();

        // When
        boolean isValid = jwtProcessor.isTokenValid(expiredToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("isRefreshTokenValid - Devrait gérer token null sans crash")
    void shouldHandleNullTokenGracefully() {
        // When & Then
        assertThatThrownBy(() -> jwtProcessor.isRefreshTokenValid(null))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("validateTemporaryToken - Devrait rejeter token avec claim 'type' manquant")
    void shouldRejectTemporaryTokenWithMissingTypeClaim() throws JwtProcessingException {
        // Given
        Key key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET));
        String email = "test@email.com";

        String tokenWithoutType = Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new java.util.Date())
                .setExpiration(new java.util.Date(System.currentTimeMillis() + TEMP_EXP))
                .claim("purpose", "password_setup")
                // Pas de claim "type"
                .signWith(key)
                .compact();

        // When & Then
        assertThatThrownBy(() -> jwtProcessor.validateTemporaryToken(tokenWithoutType, email))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Le token fourni n'est pas un token temporaire valide");
    }

    @Test
    @DisplayName("validateTemporaryToken - Devrait rejeter token avec claim 'purpose' manquant")
    void shouldRejectTemporaryTokenWithMissingPurposeClaim() throws JwtProcessingException {
        // Given
        Key key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET));
        String email = "test@email.com";

        String tokenWithoutPurpose = Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new java.util.Date())
                .setExpiration(new java.util.Date(System.currentTimeMillis() + TEMP_EXP))
                .claim("type", "temporary")
                // Pas de claim "purpose"
                .signWith(key)
                .compact();

        // When & Then
        assertThatThrownBy(() -> jwtProcessor.validateTemporaryToken(tokenWithoutPurpose, email))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Le token fourni n'est pas un token temporaire valide");
    }

    // ====================================================================
    // Tests de Cohérence et de Thread Safety
    // ====================================================================

    @Test
    @DisplayName("Devrait générer des tokens valides avec même contenu utilisateur")
    void shouldGenerateValidTokensWithSameUserContent() throws JwtProcessingException {
        // When
        String token1 = jwtProcessor.generateAccessToken(testUser);
        String token2 = jwtProcessor.generateRefreshToken(testUser);

        // Then
        assertThat(jwtProcessor.isTokenValid(token1)).isTrue();
        assertThat(jwtProcessor.extractUsername(token1)).isEqualTo(testUser.getUsername());

        // Note: isRefreshTokenValid couvre déjà isTokenValid et le type
        assertThat(jwtProcessor.isRefreshTokenValid(token2)).isTrue();
        assertThat(jwtProcessor.extractUsername(token2)).isEqualTo(testUser.getUsername());

        // Les tokens access et refresh sont différents
        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    @DisplayName("Devrait maintenir la cohérence entre access et refresh tokens")
    void shouldMaintainConsistencyBetweenAccessAndRefreshTokens() throws JwtProcessingException, InvalidTokenException {
        // When
        String accessToken = jwtProcessor.generateAccessToken(testUser);
        String refreshToken = jwtProcessor.generateRefreshToken(testUser);

        // Then
        assertThat(jwtProcessor.extractUsername(accessToken)).isEqualTo(jwtProcessor.extractUsername(refreshToken));
    }

    @Test
    @DisplayName("Devrait être thread-safe pour la génération de tokens")
    void shouldBeThreadSafeForTokenGeneration() throws InterruptedException {
        // Given
        int threadCount = 10;
        String[] tokens = new String[threadCount];
        Thread[] threads = new Thread[threadCount];

        // When
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    tokens[index] = jwtProcessor.generateAccessToken(testUser);
                } catch (JwtProcessingException e) {
                    fail("Exception during token generation: " + e.getMessage());
                }
            });
            threads[i].start();
        }

        // Attendre que tous les threads se terminent
        for (Thread thread : threads) {
            thread.join();
        }

        // Then
        for (String token : tokens) {
            assertThat(token).isNotNull();
            try {
                // Utiliser parseClaims pour vérifier que la signature est correcte
                parseClaims(token);
                assertThat(jwtProcessor.extractUsername(token)).isEqualTo(testUser.getUsername());
            } catch (Exception e) {
                fail("Token generated in a separate thread failed validation: " + e.getMessage());
            }
        }
    }

    // ====================================================================
    // Tests extractMemberId - Extraction du memberId depuis le token
    // ====================================================================

    @Test
    @DisplayName("extractMemberId - Devrait extraire le memberId du token temporaire")
    void extractMemberId_ShouldExtractMemberIdFromTemporaryToken() throws JwtProcessingException, InvalidTokenException {
        // Given
        String email = "member@example.com";
        UUID memberId = UUID.randomUUID();
        String token = jwtProcessor.generateTemporaryToken(email, memberId);

        // When
        UUID extractedMemberId = jwtProcessor.extractMemberId(token);

        // Then
        assertThat(extractedMemberId).isEqualTo(memberId);
    }

    @Test
    @DisplayName("extractMemberId - Devrait fonctionner avec différents UUIDs")
    void extractMemberId_ShouldWorkWithDifferentUUIDs() throws JwtProcessingException, InvalidTokenException {
        // Given
        UUID memberId1 = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID memberId2 = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        
        String token1 = jwtProcessor.generateTemporaryToken("user1@example.com", memberId1);
        String token2 = jwtProcessor.generateTemporaryToken("user2@example.com", memberId2);

        // When
        UUID extracted1 = jwtProcessor.extractMemberId(token1);
        UUID extracted2 = jwtProcessor.extractMemberId(token2);

        // Then
        assertThat(extracted1).isEqualTo(memberId1);
        assertThat(extracted2).isEqualTo(memberId2);
    }

    @Test
    @DisplayName("extractMemberId - Devrait lever InvalidTokenException si claim 'cid' absent")
    void extractMemberId_ShouldThrowInvalidTokenException_WhenCidClaimMissing() {
        // Given - Token sans claim 'cid'
        Key key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET));
        String tokenWithoutCid = Jwts.builder()
                .setSubject("test@example.com")
                .setIssuedAt(new java.util.Date())
                .setExpiration(new java.util.Date(System.currentTimeMillis() + 900000))
                .signWith(key)
                .compact();

        // When & Then
        assertThatThrownBy(() -> jwtProcessor.extractMemberId(tokenWithoutCid))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Le token ne contient pas de memberId (claim 'cid')");
    }

    @Test
    @DisplayName("extractMemberId - Devrait lever JwtException pour token invalide")
    void extractMemberId_ShouldThrowJwtException_ForInvalidToken() {
        // Given
        String invalidToken = "invalid.token.here";

        // When & Then - parseAndValidateClaims lance directement les exceptions JJWT
        assertThatThrownBy(() -> jwtProcessor.extractMemberId(invalidToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("extractMemberId - Devrait lever ExpiredJwtException pour token expiré")
    void extractMemberId_ShouldThrowExpiredJwtException_ForExpiredToken() {
        // Given - Token expiré avec claim 'cid'
        Key key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET));
        long tenSecondsAgo = System.currentTimeMillis() - 10000;
        UUID memberId = UUID.randomUUID();

        String expiredToken = Jwts.builder()
                .setSubject("test@example.com")
                .claim("cid", memberId.toString())
                .setIssuedAt(new java.util.Date(tenSecondsAgo))
                .setExpiration(new java.util.Date(tenSecondsAgo + 1))
                .signWith(key)
                .compact();

        // When & Then - parseAndValidateClaims lance ExpiredJwtException directement
        assertThatThrownBy(() -> jwtProcessor.extractMemberId(expiredToken))
                .isInstanceOf(ExpiredJwtException.class);
    }


    /**
     * Classe utilitaire pour les tests UserDetails
     */
    private static class TestUserDetails implements UserDetails {
        private final String username;
        private final String password;
        private final Collection<? extends GrantedAuthority> authorities;

        public TestUserDetails(String username, String password, Collection<? extends GrantedAuthority> authorities) {
            this.username = username;
            this.password = password;
            this.authorities = authorities;
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return authorities;
        }

        @Override
        public String getPassword() {
            return password;
        }

        @Override
        public String getUsername() {
            return username;
        }

        @Override
        public boolean isAccountNonExpired() {
            return true;
        }

        @Override
        public boolean isAccountNonLocked() {
            return true;
        }

        @Override
        public boolean isCredentialsNonExpired() {
            return true;
        }

        @Override
        public boolean isEnabled() {
            return true;
        }
    }
}