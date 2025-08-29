package com.ecclesiaflow.springsecurity.business.encryption;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitaires pour PasswordEncoderUtil
 * 
 * Teste les fonctionnalités critiques d'encodage et de validation des mots de passe
 * avec l'algorithme BCrypt pour garantir la sécurité du système d'authentification.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordEncoderUtil - Tests de sécurité")
class PasswordEncoderUtilTest {

    private PasswordEncoderUtil passwordEncoderUtil;

    @BeforeEach
    void setUp() {
        passwordEncoderUtil = new PasswordEncoderUtil();
    }

    @Test
    @DisplayName("Devrait encoder un mot de passe simple avec succès")
    void shouldEncodeSimplePasswordSuccessfully() {
        // Given
        String rawPassword = "password123";

        // When
        String encodedPassword = passwordEncoderUtil.encode(rawPassword);

        // Then
        assertThat(encodedPassword).isNotNull();
        assertThat(encodedPassword).isNotEmpty();
        assertThat(encodedPassword).isNotEqualTo(rawPassword);
        assertThat(encodedPassword).startsWith("$2a$"); // BCrypt prefix
    }

    @Test
    @DisplayName("Devrait générer des hachages différents pour le même mot de passe")
    void shouldGenerateDifferentHashesForSamePassword() {
        // Given
        String rawPassword = "samePassword";

        // When
        String hash1 = passwordEncoderUtil.encode(rawPassword);
        String hash2 = passwordEncoderUtil.encode(rawPassword);

        // Then
        assertThat(hash1).isNotEqualTo(hash2);
        assertThat(hash1).startsWith("$2a$");
        assertThat(hash2).startsWith("$2a$");
        
        // Mais les deux doivent matcher avec le mot de passe original
        assertThat(passwordEncoderUtil.matches(rawPassword, hash1)).isTrue();
        assertThat(passwordEncoderUtil.matches(rawPassword, hash2)).isTrue();
    }

    @Test
    @DisplayName("Devrait valider correctement un mot de passe encodé")
    void shouldValidateEncodedPasswordCorrectly() {
        // Given
        String rawPassword = "mySecretPassword";
        String encodedPassword = passwordEncoderUtil.encode(rawPassword);

        // When
        boolean matches = passwordEncoderUtil.matches(rawPassword, encodedPassword);

        // Then
        assertThat(matches).isTrue();
    }

    @Test
    @DisplayName("Devrait rejeter un mot de passe incorrect")
    void shouldRejectIncorrectPassword() {
        // Given
        String correctPassword = "correctPassword";
        String wrongPassword = "wrongPassword";
        String encodedPassword = passwordEncoderUtil.encode(correctPassword);

        // When
        boolean matches = passwordEncoderUtil.matches(wrongPassword, encodedPassword);

        // Then
        assertThat(matches).isFalse();
    }

    @Test
    @DisplayName("Devrait gérer les mots de passe avec espaces")
    void shouldHandlePasswordsWithSpaces() {
        // Given
        String passwordWithSpaces = " password with spaces ";

        // When
        String encodedPassword = passwordEncoderUtil.encode(passwordWithSpaces);

        // Then
        assertThat(encodedPassword).isNotNull();
        assertThat(passwordEncoderUtil.matches(passwordWithSpaces, encodedPassword)).isTrue();
        
        // Vérifier que les espaces sont préservés
        assertThat(passwordEncoderUtil.matches("password with spaces", encodedPassword)).isFalse();
        assertThat(passwordEncoderUtil.matches(passwordWithSpaces.trim(), encodedPassword)).isFalse();
    }

    @Test
    @DisplayName("Devrait gérer les caractères spéciaux")
    void shouldHandleSpecialCharacters() {
        // Given
        String passwordWithSpecialChars = "p@ssw0rd!#$%^&*()";

        // When
        String encodedPassword = passwordEncoderUtil.encode(passwordWithSpecialChars);

        // Then
        assertThat(encodedPassword).isNotNull();
        assertThat(passwordEncoderUtil.matches(passwordWithSpecialChars, encodedPassword)).isTrue();
    }

    @Test
    @DisplayName("Devrait gérer les mots de passe très longs")
    void shouldHandleLongPasswords() {
        // Given
        String longPassword = "a".repeat(100); // 100 caractères

        // When
        String encodedPassword = passwordEncoderUtil.encode(longPassword);

        // Then
        assertThat(encodedPassword).isNotNull();
        assertThat(passwordEncoderUtil.matches(longPassword, encodedPassword)).isTrue();
    }

    @Test
    @DisplayName("Devrait gérer les mots de passe vides")
    void shouldHandleEmptyPasswords() {
        // Given
        String emptyPassword = "";

        // When
        String encodedPassword = passwordEncoderUtil.encode(emptyPassword);

        // Then
        assertThat(encodedPassword).isNotNull();
        assertThat(passwordEncoderUtil.matches(emptyPassword, encodedPassword)).isTrue();
    }

    @Test
    @DisplayName("Devrait lancer une exception pour un mot de passe null lors de l'encodage")
    void shouldThrowExceptionForNullPasswordEncoding() {
        // When & Then
        assertThatThrownBy(() -> passwordEncoderUtil.encode(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Devrait lancer une exception pour un mot de passe null dans matches()")
    void shouldThrowExceptionForNullPasswordInMatches() {
        String validPassword = "password";
        String encodedPassword = passwordEncoderUtil.encode(validPassword);

        assertThatThrownBy(() -> passwordEncoderUtil.matches(null, encodedPassword))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rawPassword cannot be null");
    }

    @Test
    @DisplayName("Devrait utiliser l'algorithme BCrypt")
    void shouldUseBCryptAlgorithm() {
        // Given
        String password = "testPassword";

        // When
        String encodedPassword = passwordEncoderUtil.encode(password);

        // Then
        // BCrypt hash format: $2a$rounds$salt+hash
        assertThat(encodedPassword).matches("\\$2a\\$\\d{2}\\$.{53}");
        assertThat(encodedPassword).hasSize(60); // BCrypt hash length
    }

    @Test
    @DisplayName("Devrait être thread-safe pour l'encodage concurrent")
    void shouldBeThreadSafeForConcurrentEncoding() throws InterruptedException {
        // Given
        String password = "concurrentPassword";
        int threadCount = 10;
        String[] results = new String[threadCount];
        Thread[] threads = new Thread[threadCount];

        // When
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                results[index] = passwordEncoderUtil.encode(password);
            });
            threads[i].start();
        }

        // Attendre que tous les threads se terminent
        for (Thread thread : threads) {
            thread.join();
        }

        // Then
        for (String result : results) {
            assertThat(result).isNotNull();
            assertThat(result).startsWith("$2a$");
            assertThat(passwordEncoderUtil.matches(password, result)).isTrue();
        }
    }

    @Test
    @DisplayName("Devrait maintenir la cohérence entre encode() et matches()")
    void shouldMaintainConsistencyBetweenEncodeAndMatches() {
        // Given
        String[] testPasswords = {
            "simple",
            "complex!@#$%^&*()",
            "with spaces",
            "très_long_mot_de_passe_avec_accents_éàùç",
            "123456789",
            "MixedCasePassword123!"
        };

        // When & Then
        for (String password : testPasswords) {
            String encoded = passwordEncoderUtil.encode(password);
            
            assertThat(passwordEncoderUtil.matches(password, encoded))
                .as("Password '%s' should match its encoded version", password)
                .isTrue();
                
            assertThat(passwordEncoderUtil.matches(password + "wrong", encoded))
                .as("Wrong password should not match encoded version of '%s'", password)
                .isFalse();
        }
    }
}
