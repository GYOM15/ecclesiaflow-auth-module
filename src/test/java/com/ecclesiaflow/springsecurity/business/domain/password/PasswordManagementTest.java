package com.ecclesiaflow.springsecurity.business.domain.password;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires pour {@link PasswordManagement}.
 */
@DisplayName("PasswordManagement - Tests du record de domaine")
class PasswordManagementTest {

    @Test
    @DisplayName("Devrait créer un PasswordManagement avec password et temporaryToken")
    void shouldCreatePasswordManagementWithPasswordAndToken() {
        // Given
        String password = "SecurePassword123!";
        String temporaryToken = "temp-token-abc123";

        // When
        PasswordManagement passwordManagement = new PasswordManagement(password, temporaryToken);

        // Then
        assertThat(passwordManagement).isNotNull();
        assertThat(passwordManagement.password()).isEqualTo(password);
        assertThat(passwordManagement.temporaryToken()).isEqualTo(temporaryToken);
    }

    @Test
    @DisplayName("Devrait créer un PasswordManagement avec valeurs nulles")
    void shouldCreatePasswordManagementWithNullValues() {
        // When
        PasswordManagement passwordManagement = new PasswordManagement(null, null);

        // Then
        assertThat(passwordManagement).isNotNull();
        assertThat(passwordManagement.password()).isNull();
        assertThat(passwordManagement.temporaryToken()).isNull();
    }

    @Test
    @DisplayName("Devrait supporter l'égalité entre deux instances avec mêmes valeurs")
    void shouldSupportEquality() {
        // Given
        String password = "password123";
        String token = "token456";
        
        PasswordManagement pm1 = new PasswordManagement(password, token);
        PasswordManagement pm2 = new PasswordManagement(password, token);

        // Then
        assertThat(pm1).isEqualTo(pm2);
        assertThat(pm1.hashCode()).isEqualTo(pm2.hashCode());
    }

    @Test
    @DisplayName("Devrait avoir un hashCode cohérent")
    void shouldHaveConsistentHashCode() {
        // Given
        PasswordManagement passwordManagement = new PasswordManagement("pwd", "token");

        // When
        int hash1 = passwordManagement.hashCode();
        int hash2 = passwordManagement.hashCode();

        // Then
        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    @DisplayName("Devrait avoir une représentation toString lisible")
    void shouldHaveReadableToString() {
        // Given
        PasswordManagement passwordManagement = new PasswordManagement("myPassword", "myToken");

        // When
        String toString = passwordManagement.toString();

        // Then
        assertThat(toString).contains("PasswordManagement");
        assertThat(toString).contains("myPassword");
        assertThat(toString).contains("myToken");
    }

    @Test
    @DisplayName("Deux instances avec valeurs différentes ne devraient pas être égales")
    void shouldNotBeEqualWithDifferentValues() {
        // Given
        PasswordManagement pm1 = new PasswordManagement("password1", "token1");
        PasswordManagement pm2 = new PasswordManagement("password2", "token2");

        // Then
        assertThat(pm1).isNotEqualTo(pm2);
    }

    @Test
    @DisplayName("Devrait permettre d'accéder aux champs via les accesseurs du record")
    void shouldAccessFieldsViaRecordAccessors() {
        // Given
        String expectedPassword = "TestPassword";
        String expectedToken = "TestToken";
        PasswordManagement passwordManagement = new PasswordManagement(expectedPassword, expectedToken);

        // When & Then
        assertThat(passwordManagement.password()).isEqualTo(expectedPassword);
        assertThat(passwordManagement.temporaryToken()).isEqualTo(expectedToken);
    }
}
