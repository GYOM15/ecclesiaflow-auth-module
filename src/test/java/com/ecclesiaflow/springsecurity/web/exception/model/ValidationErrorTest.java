package com.ecclesiaflow.springsecurity.web.exception.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitaires pour ValidationError.
 * Vérifie la construction, la sérialisation JSON et l'immutabilité du record.
 */
@DisplayName("ValidationError - Tests Unitaires")
class ValidationErrorTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    // === TESTS DE CONSTRUCTION ===

    @Test
    @DisplayName("Devrait créer un ValidationError avec tous les paramètres")
    void constructor_WithAllParameters_ShouldCreateValidError() {
        // When
        ValidationError error = new ValidationError(
                "Le champ est obligatoire",
                "user.firstName",
                "validation",
                "string non vide",
                "null",
                "REQUIRED_FIELD",
                5,
                12
        );

        // Then
        assertThat(error.message()).isEqualTo("Le champ est obligatoire");
        assertThat(error.path()).isEqualTo("user.firstName");
        assertThat(error.type()).isEqualTo("validation");
        assertThat(error.expected()).isEqualTo("string non vide");
        assertThat(error.received()).isEqualTo("null");
        assertThat(error.code()).isEqualTo("REQUIRED_FIELD");
        assertThat(error.line()).isEqualTo(5);
        assertThat(error.column()).isEqualTo(12);
    }

    @Test
    @DisplayName("Devrait créer un ValidationError avec des valeurs null")
    void constructor_WithNullValues_ShouldCreateValidError() {
        // When
        ValidationError error = new ValidationError(
                "Erreur de type",
                "field",
                "type",
                null,
                null,
                "TYPE_ERROR",
                null,
                null
        );

        // Then
        assertThat(error.message()).isEqualTo("Erreur de type");
        assertThat(error.path()).isEqualTo("field");
        assertThat(error.type()).isEqualTo("type");
        assertThat(error.expected()).isNull();
        assertThat(error.received()).isNull();
        assertThat(error.code()).isEqualTo("TYPE_ERROR");
        assertThat(error.line()).isNull();
        assertThat(error.column()).isNull();
    }

    // === TESTS DE SÉRIALISATION JSON ===

    @Test
    @DisplayName("Devrait sérialiser correctement en JSON")
    void serialization_ShouldProduceValidJson() throws JsonProcessingException {
        // Given
        ValidationError error = new ValidationError(
                "La taille doit être entre 2 et 50 caractères",
                "user.lastName",
                "validation",
                "string[2-50]",
                "A",
                "SIZE_CONSTRAINT",
                3,
                8
        );

        // When
        String json = objectMapper.writeValueAsString(error);

        // Then
        assertThat(json).isNotNull();
        assertThat(json).contains("\"message\":\"La taille doit être entre 2 et 50 caractères\"");
        assertThat(json).contains("\"path\":\"user.lastName\"");
        assertThat(json).contains("\"type\":\"validation\"");
        assertThat(json).contains("\"expected\":\"string[2-50]\"");
        assertThat(json).contains("\"received\":\"A\"");
        assertThat(json).contains("\"code\":\"SIZE_CONSTRAINT\"");
        assertThat(json).contains("\"line\":3");
        assertThat(json).contains("\"column\":8");
    }

    @Test
    @DisplayName("Devrait désérialiser correctement depuis JSON")
    void deserialization_ShouldProduceValidObject() throws JsonProcessingException {
        // Given
        String json = """
            {
                "message": "Format d'email invalide",
                "path": "user.email",
                "type": "format",
                "expected": "email format",
                "received": "invalid-email",
                "code": "EMAIL_FORMAT",
                "line": 7,
                "column": 15
            }
            """;

        // When
        ValidationError error = objectMapper.readValue(json, ValidationError.class);

        // Then
        assertThat(error.message()).isEqualTo("Format d'email invalide");
        assertThat(error.path()).isEqualTo("user.email");
        assertThat(error.type()).isEqualTo("format");
        assertThat(error.expected()).isEqualTo("email format");
        assertThat(error.received()).isEqualTo("invalid-email");
        assertThat(error.code()).isEqualTo("EMAIL_FORMAT");
        assertThat(error.line()).isEqualTo(7);
        assertThat(error.column()).isEqualTo(15);
    }

    @Test
    @DisplayName("Devrait gérer les valeurs null lors de la désérialisation")
    void deserialization_WithNullValues_ShouldHandleCorrectly() throws JsonProcessingException {
        // Given
        String json = """
            {
                "message": "Erreur simple",
                "path": "field",
                "type": "simple",
                "expected": null,
                "received": null,
                "code": "SIMPLE_ERROR",
                "line": null,
                "column": null
            }
            """;

        // When
        ValidationError error = objectMapper.readValue(json, ValidationError.class);

        // Then
        assertThat(error.message()).isEqualTo("Erreur simple");
        assertThat(error.path()).isEqualTo("field");
        assertThat(error.type()).isEqualTo("simple");
        assertThat(error.expected()).isNull();
        assertThat(error.received()).isNull();
        assertThat(error.code()).isEqualTo("SIMPLE_ERROR");
        assertThat(error.line()).isNull();
        assertThat(error.column()).isNull();
    }

    // === TESTS D'ÉGALITÉ ET HASHCODE ===

    @Test
    @DisplayName("Devrait implémenter equals correctement")
    void equals_WithSameContent_ShouldReturnTrue() {
        // Given
        ValidationError error1 = new ValidationError(
                "Message", "path", "type", "expected", "received", "CODE", 1, 2
        );
        ValidationError error2 = new ValidationError(
                "Message", "path", "type", "expected", "received", "CODE", 1, 2
        );

        // When & Then
        assertThat(error1).isEqualTo(error2);
        assertThat(error1.hashCode()).isEqualTo(error2.hashCode());
    }

    @Test
    @DisplayName("Devrait implémenter equals correctement pour des contenus différents")
    void equals_WithDifferentContent_ShouldReturnFalse() {
        // Given
        ValidationError error1 = new ValidationError(
                "Message 1", "path", "type", "expected", "received", "CODE", 1, 2
        );
        ValidationError error2 = new ValidationError(
                "Message 2", "path", "type", "expected", "received", "CODE", 1, 2
        );

        // When & Then
        assertThat(error1).isNotEqualTo(error2);
    }

    // === TESTS DE toString ===

    @Test
    @DisplayName("Devrait produire une représentation string lisible")
    void toString_ShouldProduceReadableString() {
        // Given
        ValidationError error = new ValidationError(
                "Test message", "test.field", "validation", "expected", "received", "TEST_CODE", 5, 10
        );

        // When
        String stringRepresentation = error.toString();

        // Then
        assertThat(stringRepresentation).contains("ValidationError");
        assertThat(stringRepresentation).contains("Test message");
        assertThat(stringRepresentation).contains("test.field");
        assertThat(stringRepresentation).contains("validation");
        assertThat(stringRepresentation).contains("TEST_CODE");
    }

    // === TESTS DE CAS D'UTILISATION SPÉCIFIQUES ===

    @Test
    @DisplayName("Devrait supporter les chemins de champs imbriqués")
    void nestedFieldPaths_ShouldBeSupported() {
        // Given
        ValidationError error = new ValidationError(
                "Adresse invalide",
                "user.address.street.number",
                "validation",
                "number",
                "abc",
                "INVALID_NUMBER",
                null,
                null
        );

        // When & Then
        assertThat(error.path()).isEqualTo("user.address.street.number");
        assertThat(error.message()).isEqualTo("Adresse invalide");
    }

    @Test
    @DisplayName("Devrait supporter différents types d'erreur")
    void differentErrorTypes_ShouldBeSupported() {
        // Given
        ValidationError validationError = new ValidationError(
                "Validation failed", "field", "validation", "valid", "invalid", "VALIDATION", null, null
        );
        ValidationError typeError = new ValidationError(
                "Type mismatch", "field", "type", "string", "number", "TYPE_MISMATCH", null, null
        );
        ValidationError formatError = new ValidationError(
                "Format invalid", "field", "format", "yyyy-MM-dd", "invalid-date", "FORMAT", null, null
        );

        // When & Then
        assertThat(validationError.type()).isEqualTo("validation");
        assertThat(typeError.type()).isEqualTo("type");
        assertThat(formatError.type()).isEqualTo("format");
    }

    @Test
    @DisplayName("Devrait supporter les informations de position pour le debugging")
    void positionInformation_ShouldSupportDebugging() {
        // Given
        ValidationError errorWithPosition = new ValidationError(
                "Syntax error", "document", "syntax", "valid JSON", "invalid JSON", "SYNTAX", 15, 23
        );

        // When & Then
        assertThat(errorWithPosition.line()).isEqualTo(15);
        assertThat(errorWithPosition.column()).isEqualTo(23);
        assertThat(errorWithPosition.type()).isEqualTo("syntax");
    }
}
