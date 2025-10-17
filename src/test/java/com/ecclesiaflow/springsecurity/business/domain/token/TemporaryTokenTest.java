package com.ecclesiaflow.springsecurity.business.domain.token;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitaires pour le Value Object TemporaryToken.
 * <p>
 * Vérifie les invariants du domain et la validation des données.
 * </p>
 *
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
@DisplayName("TemporaryToken - Value Object Domain")
class TemporaryTokenTest {

    private static final String VALID_EMAIL = "test@example.com";
    private static final UUID VALID_MEMBER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    // ====================================================================
    // Tests de Création Réussie
    // ====================================================================

    @Test
    @DisplayName("Devrait créer un TemporaryToken valide avec email et memberId")
    void shouldCreateValidTemporaryToken() {
        // When
        TemporaryToken token = new TemporaryToken(VALID_EMAIL, VALID_MEMBER_ID);

        // Then
        assertThat(token).isNotNull();
        assertThat(token.email()).isEqualTo(VALID_EMAIL);
        assertThat(token.memberId()).isEqualTo(VALID_MEMBER_ID);
    }

    @Test
    @DisplayName("Devrait créer avec un email complexe valide")
    void shouldCreateWithComplexEmail() {
        // Given
        String complexEmail = "user.name+tag@sub.domain.example.com";

        // When
        TemporaryToken token = new TemporaryToken(complexEmail, VALID_MEMBER_ID);

        // Then
        assertThat(token.email()).isEqualTo(complexEmail);
    }

    // ====================================================================
    // Tests de Validation - Email Invalide
    // ====================================================================

    @Test
    @DisplayName("Devrait lever une exception si email est null")
    void shouldThrowException_WhenEmailIsNull() {
        // When/Then
        assertThatThrownBy(() -> new TemporaryToken(null, VALID_MEMBER_ID))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("L'email ne peut pas être null ou vide");
    }

    @Test
    @DisplayName("Devrait lever une exception si email est vide")
    void shouldThrowException_WhenEmailIsEmpty() {
        // When/Then
        assertThatThrownBy(() -> new TemporaryToken("", VALID_MEMBER_ID))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("L'email ne peut pas être null ou vide");
    }

    @Test
    @DisplayName("Devrait lever une exception si email contient uniquement des espaces")
    void shouldThrowException_WhenEmailIsBlank() {
        // When/Then
        assertThatThrownBy(() -> new TemporaryToken("   ", VALID_MEMBER_ID))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("L'email ne peut pas être null ou vide");
    }

    // ====================================================================
    // Tests de Validation - MemberId Invalide
    // ====================================================================

    @Test
    @DisplayName("Devrait lever une exception si memberId est null")
    void shouldThrowException_WhenMemberIdIsNull() {
        // When/Then
        assertThatThrownBy(() -> new TemporaryToken(VALID_EMAIL, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Le memberId ne peut pas être null");
    }

    // ====================================================================
    // Tests d'Égalité et HashCode (Records)
    // ====================================================================

    @Test
    @DisplayName("Deux TemporaryToken avec mêmes valeurs devraient être égaux")
    void shouldBeEqual_WhenSameValues() {
        // Given
        TemporaryToken token1 = new TemporaryToken(VALID_EMAIL, VALID_MEMBER_ID);
        TemporaryToken token2 = new TemporaryToken(VALID_EMAIL, VALID_MEMBER_ID);

        // Then
        assertThat(token1).isEqualTo(token2);
        assertThat(token1.hashCode()).isEqualTo(token2.hashCode());
    }

    @Test
    @DisplayName("Deux TemporaryToken avec emails différents ne devraient pas être égaux")
    void shouldNotBeEqual_WhenDifferentEmails() {
        // Given
        TemporaryToken token1 = new TemporaryToken("email1@example.com", VALID_MEMBER_ID);
        TemporaryToken token2 = new TemporaryToken("email2@example.com", VALID_MEMBER_ID);

        // Then
        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    @DisplayName("Deux TemporaryToken avec memberIds différents ne devraient pas être égaux")
    void shouldNotBeEqual_WhenDifferentMemberIds() {
        // Given
        UUID memberId1 = UUID.randomUUID();
        UUID memberId2 = UUID.randomUUID();
        TemporaryToken token1 = new TemporaryToken(VALID_EMAIL, memberId1);
        TemporaryToken token2 = new TemporaryToken(VALID_EMAIL, memberId2);

        // Then
        assertThat(token1).isNotEqualTo(token2);
    }

    // ====================================================================
    // Tests toString (Records)
    // ====================================================================

    @Test
    @DisplayName("toString() devrait contenir email et memberId")
    void shouldContainFieldsInToString() {
        // Given
        TemporaryToken token = new TemporaryToken(VALID_EMAIL, VALID_MEMBER_ID);

        // When
        String toString = token.toString();

        // Then
        assertThat(toString)
            .contains(VALID_EMAIL)
            .contains(VALID_MEMBER_ID.toString());
    }
}
