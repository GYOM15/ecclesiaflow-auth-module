package com.ecclesiaflow.springsecurity.web.security;

import com.ecclesiaflow.springsecurity.web.exception.InvalidTokenException;
import com.ecclesiaflow.springsecurity.web.exception.JwtProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

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

    @BeforeEach
    void setUp() {
        jwtProcessor = new JwtProcessor();
        
        // Configuration des propriétés JWT pour les tests
        ReflectionTestUtils.setField(jwtProcessor, "secretKey", "dGVzdC1zZWNyZXQta2V5LWZvci1qd3QtdGVzdGluZy1wdXJwb3Nlcy1vbmx5LTEyMzQ1Njc4OTA=");
        ReflectionTestUtils.setField(jwtProcessor, "accessTokenExpiration", 86400000L); // 24 heures
        ReflectionTestUtils.setField(jwtProcessor, "refreshTokenExpiration", 604800000L); // 7 jours
        
        testUser = new TestUserDetails("test@example.com", "password", List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    @DisplayName("Devrait générer un token d'accès valide")
    void shouldGenerateValidAccessToken() throws JwtProcessingException {
        // When
        String accessToken = jwtProcessor.generateAccessToken(testUser);

        // Then
        assertThat(accessToken).isNotNull();
        assertThat(accessToken).isNotEmpty();
        assertThat(accessToken.split("\\.")).hasSize(3); // JWT format: header.payload.signature
    }

    @Test
    @DisplayName("Devrait générer un refresh token valide")
    void shouldGenerateValidRefreshToken() throws JwtProcessingException {
        // When
        String refreshToken = jwtProcessor.generateRefreshToken(testUser);

        // Then
        assertThat(refreshToken).isNotNull();
        assertThat(refreshToken).isNotEmpty();
        assertThat(refreshToken.split("\\.")).hasSize(3); // JWT format: header.payload.signature
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
    @DisplayName("Devrait rejeter un token malformé")
    void shouldRejectMalformedToken() {
        // Given
        String malformedToken = "invalid.token.format";

        // When & Then
        assertThatThrownBy(() -> jwtProcessor.isTokenValid(malformedToken))
                .isInstanceOf(RuntimeException.class); // JJWT lance des exceptions runtime
    }

    @Test
    @DisplayName("Devrait rejeter un token avec signature invalide")
    void shouldRejectTokenWithInvalidSignature() throws JwtProcessingException {
        // Given
        String validToken = jwtProcessor.generateAccessToken(testUser);
        String tokenWithInvalidSignature = validToken.substring(0, validToken.lastIndexOf('.')) + ".invalidSignature";

        // When & Then
        assertThatThrownBy(() -> jwtProcessor.isTokenValid(tokenWithInvalidSignature))
                .isInstanceOf(RuntimeException.class); // JJWT lance des exceptions runtime
    }

    @Test
    @DisplayName("Devrait rejeter un token null")
    void shouldRejectNullToken() {
        // When & Then
        assertThatThrownBy(() -> jwtProcessor.isTokenValid(null))
                .isInstanceOf(RuntimeException.class); // JJWT lance des exceptions runtime
    }

    @Test
    @DisplayName("Devrait rejeter un token vide")
    void shouldRejectEmptyToken() {
        // When & Then
        assertThatThrownBy(() -> jwtProcessor.isTokenValid(""))
                .isInstanceOf(RuntimeException.class); // JJWT lance des exceptions runtime
    }

    @Test
    @DisplayName("Devrait générer des tokens valides avec même contenu utilisateur")
    void shouldGenerateValidTokensWithSameUserContent() throws JwtProcessingException {
        // When
        String token1 = jwtProcessor.generateAccessToken(testUser);
        String token2 = jwtProcessor.generateRefreshToken(testUser);

        // Then - Les deux tokens doivent être valides et contenir le même username
        assertThat(jwtProcessor.isTokenValid(token1)).isTrue();
        assertThat(jwtProcessor.isTokenValid(token2)).isTrue();
        assertThat(jwtProcessor.extractUsername(token1)).isEqualTo(testUser.getUsername());
        assertThat(jwtProcessor.extractUsername(token2)).isEqualTo(testUser.getUsername());
        
        // Les tokens access et refresh sont différents par nature
        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    @DisplayName("Devrait gérer différents types d'utilisateurs")
    void shouldHandleDifferentUserTypes() throws JwtProcessingException {
        // Given
        TestUserDetails user1 = new TestUserDetails("user1@example.com", "pass1", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        TestUserDetails user2 = new TestUserDetails("admin@example.com", "pass2", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        // When
        String token1 = jwtProcessor.generateAccessToken(user1);
        String token2 = jwtProcessor.generateAccessToken(user2);

        // Then
        assertThat(jwtProcessor.extractUsername(token1)).isEqualTo("user1@example.com");
        assertThat(jwtProcessor.extractUsername(token2)).isEqualTo("admin@example.com");
        assertThat(jwtProcessor.isTokenValid(token1)).isTrue();
        assertThat(jwtProcessor.isTokenValid(token2)).isTrue();
    }

    @Test
    @DisplayName("Devrait gérer les caractères spéciaux dans le nom d'utilisateur")
    void shouldHandleSpecialCharactersInUsername() throws JwtProcessingException {
        // Given
        TestUserDetails userWithSpecialChars = new TestUserDetails(
            "user+test@domain.co.uk", 
            "password", 
            List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        // When
        String token = jwtProcessor.generateAccessToken(userWithSpecialChars);

        // Then
        assertThat(jwtProcessor.extractUsername(token)).isEqualTo("user+test@domain.co.uk");
        assertThat(jwtProcessor.isTokenValid(token)).isTrue();
    }

    @Test
    @DisplayName("Devrait maintenir la cohérence entre access et refresh tokens")
    void shouldMaintainConsistencyBetweenAccessAndRefreshTokens() throws JwtProcessingException, InvalidTokenException {
        // When
        String accessToken = jwtProcessor.generateAccessToken(testUser);
        String refreshToken = jwtProcessor.generateRefreshToken(testUser);

        // Then
        assertThat(jwtProcessor.extractUsername(accessToken)).isEqualTo(jwtProcessor.extractUsername(refreshToken));
        assertThat(jwtProcessor.isTokenValid(accessToken)).isTrue();
        assertThat(jwtProcessor.isRefreshTokenValid(refreshToken)).isTrue();
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
                assertThat(jwtProcessor.isTokenValid(token)).isTrue();
                assertThat(jwtProcessor.extractUsername(token)).isEqualTo(testUser.getUsername());
            } catch (JwtProcessingException e) {
                fail("Exception during token validation: " + e.getMessage());
            }
        }
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
