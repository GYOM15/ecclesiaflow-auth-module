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
 * Unit tests for ApiErrorResponse.
 * Tests construction, JSON serialization and builder behavior.
 */
@DisplayName("ApiErrorResponse - Unit Tests")
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
    @DisplayName("Should create une ApiErrorResponse with all parameters")
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
    @DisplayName("Should keep errors null if null is passed explicitly")
    void constructor_WithNullErrors_ShouldKeepNull() {
        // When
        ApiErrorResponse response = new ApiErrorResponse(
                testTimestamp, 404, "Not Found", "Resource not found", "/api/test", null
        );

        // Then
        assertThat(response.errors()).isNull();
    }

    // === TESTS DU BUILDER ===

    @Test
    @DisplayName("Should create a builder with default values")
    void builder_ShouldCreateBuilderWithDefaults() {
        // When
        ApiErrorResponse.ApiErrorResponseBuilder builder = ApiErrorResponse.builder();

        // Then
        assertThat(builder).isNotNull();
    }

    @Test
    @DisplayName("Should build a complete response with the builder")
    void builder_WithAllFields_ShouldBuildCompleteResponse() {
        // When
        ApiErrorResponse response = ApiErrorResponse.builder()
                .status(404)
                .error("Not Found")
                .message("Resource not found")
                .path("/api/members/123")
                .addValidationError(new ValidationError("Erreur", "field", "type", "exp", "rec", "CODE", null, null))
                .build();

        // Then
        assertThat(response.status()).isEqualTo(404);
        assertThat(response.error()).isEqualTo("Not Found");
        assertThat(response.message()).isEqualTo("Resource not found");
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
    @DisplayName("Should generate automatically a timestamp during construction")
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

    // === JSON SERIALIZATION TESTS ===

    @Test
    @DisplayName("Should serialize correctly to JSON")
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
    @DisplayName("Should deserialize correctly from JSON")
    void deserialization_ShouldProduceValidObject() throws JsonProcessingException {
        // Given
        String json = """
            {
                "timestamp": "2024-01-15T10:30:00",
                "status": 404,
                "error": "Not Found",
                "message": "Member not found",
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
        assertThat(response.message()).isEqualTo("Member not found");
        assertThat(response.path()).isEqualTo("/api/members/123");
        assertThat(response.errors()).hasSize(1);

        ValidationError error = response.errors().get(0);
        assertThat(error.message()).isEqualTo("ID invalide");
        assertThat(error.path()).isEqualTo("id");
        assertThat(error.code()).isEqualTo("INVALID_UUID");
    }

    @Test
    @DisplayName("Should exclude null fields during JSON serialization")
    void serialization_ShouldExcludeNullFields() throws JsonProcessingException {
        // Given
        ApiErrorResponse response = new ApiErrorResponse(
                testTimestamp, 400, "Bad Request", "Message", "/path", new ArrayList<>()
        );

        // When
        String json = objectMapper.writeValueAsString(response);

        // Then
        assertThat(json).doesNotContain("null");
        // Empty errors should be included since it is an empty list, not null
        assertThat(json).contains("\"errors\":[]");
    }

    // === EQUALS AND HASHCODE TESTS ===

    @Test
    @DisplayName("Should implement equals correctly")
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
    @DisplayName("Should implement equals correctly for different contents")
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
    @DisplayName("Should produce a readable string representation")
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
