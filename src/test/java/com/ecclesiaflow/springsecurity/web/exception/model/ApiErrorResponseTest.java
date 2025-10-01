package com.ecclesiaflow.springsecurity.web.exception.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitaires pour ApiErrorResponse.
 * Vérifie la construction, la sérialisation JSON et le comportement du builder.
 */
@DisplayName("ApiErrorResponse - Tests Unitaires")
class ApiErrorResponseTest {

    private ObjectMapper objectMapper;
    private LocalDateTime testTimestamp;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        testTimestamp = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
    }

    // === TESTS DE CONSTRUCTION ===

    @Test
    @DisplayName("Devrait créer une ApiErrorResponse avec tous les paramètres")
    void constructor_WithAllParameters_ShouldCreateValidResponse() {
        // Given
        List<ValidationError> errors = List.of(
                new ValidationError("Erreur test", "field1", "validation", "expected", "received", "CODE_001", 1, 5)
        );

        // When
        ApiErrorResponse response = new ApiErrorResponse(
                testTimestamp, 400, "Bad Request", "Message d'erreur", "/api/test", errors
        );

        // Then
        assertThat(response.timestamp()).isEqualTo(testTimestamp);
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.error()).isEqualTo("Bad Request");
        assertThat(response.message()).isEqualTo("Message d'erreur");
        assertThat(response.path()).isEqualTo("/api/test");
        assertThat(response.errors()).hasSize(1);
        assertThat(response.errors().getFirst().message()).isEqualTo("Erreur test");
    }

    @Test
    @DisplayName("Devrait garder errors null si null est passé explicitement")
    void constructor_WithNullErrors_ShouldKeepNull() {
        // When
        ApiErrorResponse response = new ApiErrorResponse(
                testTimestamp, 404, "Not Found", "Ressource non trouvée", "/api/test", null
        );

        // Then
        assertThat(response.errors()).isNull();
    }

    // === TESTS DU BUILDER ===

    @Test
    @DisplayName("Devrait créer un builder avec valeurs par défaut")
    void builder_ShouldCreateBuilderWithDefaults() {
        // When
        ApiErrorResponse.ApiErrorResponseBuilder builder = ApiErrorResponse.builder();

        // Then
        assertThat(builder).isNotNull();
    }

    @Test
    @DisplayName("Devrait construire une réponse complète avec le builder")
    void builder_WithAllFields_ShouldBuildCompleteResponse() {
        // When
        ApiErrorResponse response = ApiErrorResponse.builder()
                .status(404)
                .error("Not Found")
                .message("Ressource non trouvée")
                .path("/api/members/123")
                .addValidationError(new ValidationError("Erreur", "field", "type", "exp", "rec", "CODE", null, null))
                .build();

        // Then
        assertThat(response.status()).isEqualTo(404);
        assertThat(response.error()).isEqualTo("Not Found");
        assertThat(response.message()).isEqualTo("Ressource non trouvée");
        assertThat(response.path()).isEqualTo("/api/members/123");
        assertThat(response.timestamp()).isNotNull();
        assertThat(response.errors()).hasSize(1);
    }

    @Test
    @DisplayName("Devrait permettre l'ajout de plusieurs erreurs de validation")
    void builder_WithMultipleValidationErrors_ShouldAddAllErrors() {
        // Given
        ValidationError error1 = new ValidationError("Erreur 1", "field1", "type1", "exp1", "rec1", "CODE1", null, null);
        ValidationError error2 = new ValidationError("Erreur 2", "field2", "type2", "exp2", "rec2", "CODE2", null, null);

        // When
        ApiErrorResponse response = ApiErrorResponse.builder()
                .status(400)
                .error("Bad Request")
                .message("Erreurs de validation")
                .path("/api/test")
                .addValidationError(error1)
                .addValidationError(error2)
                .build();

        // Then
        assertThat(response.errors()).hasSize(2);
        assertThat(response.errors()).containsExactly(error1, error2);
    }

    @Test
    @DisplayName("Devrait générer automatiquement un timestamp lors de la construction")
    void builder_ShouldGenerateTimestampAutomatically() {
        // Given
        LocalDateTime beforeBuild = LocalDateTime.now();

        // When
        ApiErrorResponse response = ApiErrorResponse.builder()
                .status(500)
                .error("Internal Server Error")
                .message("Erreur interne")
                .path("/api/test")
                .build();

        // Then
        LocalDateTime afterBuild = LocalDateTime.now();
        assertThat(response.timestamp()).isNotNull();
        assertThat(response.timestamp()).isBetween(beforeBuild, afterBuild);
    }

    // === TESTS DE SÉRIALISATION JSON ===

    @Test
    @DisplayName("Devrait sérialiser correctement en JSON")
    void serialization_ShouldProduceValidJson() throws JsonProcessingException {
        // Given
        ApiErrorResponse response = ApiErrorResponse.builder()
                .status(400)
                .error("Bad Request")
                .message("Erreur de validation")
                .path("/api/members")
                .addValidationError(new ValidationError("Champ requis", "firstName", "validation", "string", "null", "REQUIRED", null, null))
                .build();

        // When
        String json = objectMapper.writeValueAsString(response);

        // Then
        assertThat(json).isNotNull();
        assertThat(json).contains("\"status\":400");
        assertThat(json).contains("\"error\":\"Bad Request\"");
        assertThat(json).contains("\"message\":\"Erreur de validation\"");
        assertThat(json).contains("\"path\":\"/api/members\"");
        assertThat(json).contains("\"errors\":");
        assertThat(json).contains("\"firstName\"");
    }

    @Test
    @DisplayName("Devrait désérialiser correctement depuis JSON")
    void deserialization_ShouldProduceValidObject() throws JsonProcessingException {
        // Given
        String json = """
            {
                "timestamp": "2024-01-15T10:30:00",
                "status": 404,
                "error": "Not Found",
                "message": "Membre non trouvé",
                "path": "/api/members/123",
                "errors": [
                    {
                        "message": "ID invalide",
                        "path": "id",
                        "type": "validation",
                        "expected": "UUID",
                        "received": "123",
                        "code": "INVALID_UUID",
                        "line": null,
                        "column": null
                    }
                ]
            }
            """;

        // When
        ApiErrorResponse response = objectMapper.readValue(json, ApiErrorResponse.class);

        // Then
        assertThat(response.timestamp()).isEqualTo(LocalDateTime.of(2024, 1, 15, 10, 30, 0));
        assertThat(response.status()).isEqualTo(404);
        assertThat(response.error()).isEqualTo("Not Found");
        assertThat(response.message()).isEqualTo("Membre non trouvé");
        assertThat(response.path()).isEqualTo("/api/members/123");
        assertThat(response.errors()).hasSize(1);

        ValidationError error = response.errors().get(0);
        assertThat(error.message()).isEqualTo("ID invalide");
        assertThat(error.path()).isEqualTo("id");
        assertThat(error.code()).isEqualTo("INVALID_UUID");
    }

    @Test
    @DisplayName("Devrait exclure les champs null lors de la sérialisation JSON")
    void serialization_ShouldExcludeNullFields() throws JsonProcessingException {
        // Given
        ApiErrorResponse response = new ApiErrorResponse(
                testTimestamp, 400, "Bad Request", "Message", "/path", new ArrayList<>()
        );

        // When
        String json = objectMapper.writeValueAsString(response);

        // Then
        assertThat(json).doesNotContain("null");
        // Les erreurs vides devraient être incluses car c'est une liste vide, pas null
        assertThat(json).contains("\"errors\":[]");
    }

    // === TESTS D'ÉGALITÉ ET HASHCODE ===

    @Test
    @DisplayName("Devrait implémenter equals correctement")
    void equals_WithSameContent_ShouldReturnTrue() {
        // Given
        List<ValidationError> errors = List.of(
                new ValidationError("Test", "field", "type", "exp", "rec", "CODE", null, null)
        );

        ApiErrorResponse response1 = new ApiErrorResponse(
                testTimestamp, 400, "Bad Request", "Message", "/path", errors
        );
        ApiErrorResponse response2 = new ApiErrorResponse(
                testTimestamp, 400, "Bad Request", "Message", "/path", errors
        );

        // When & Then
        assertThat(response1).isEqualTo(response2);
        assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
    }

    @Test
    @DisplayName("Devrait implémenter equals correctement pour des contenus différents")
    void equals_WithDifferentContent_ShouldReturnFalse() {
        // Given
        ApiErrorResponse response1 = new ApiErrorResponse(
                testTimestamp, 400, "Bad Request", "Message 1", "/path", new ArrayList<>()
        );
        ApiErrorResponse response2 = new ApiErrorResponse(
                testTimestamp, 400, "Bad Request", "Message 2", "/path", new ArrayList<>()
        );

        // When & Then
        assertThat(response1).isNotEqualTo(response2);
    }

    // === TESTS DE toString ===

    @Test
    @DisplayName("Devrait produire une représentation string lisible")
    void toString_ShouldProduceReadableString() {
        // Given
        ApiErrorResponse response = ApiErrorResponse.builder()
                .status(400)
                .error("Bad Request")
                .message("Test message")
                .path("/test")
                .build();

        // When
        String stringRepresentation = response.toString();

        // Then
        assertThat(stringRepresentation).contains("ApiErrorResponse");
        assertThat(stringRepresentation).contains("400");
        assertThat(stringRepresentation).contains("Bad Request");
        assertThat(stringRepresentation).contains("Test message");
        assertThat(stringRepresentation).contains("/test");
    }
}
