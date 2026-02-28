package com.ecclesiaflow.springsecurity.web.exception.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for ValidationError.
 * Tests construction, JSON serialization and record immutability.
 */
@DisplayName("ValidationError - Unit Tests")
class ValidationErrorTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    // === CONSTRUCTION TESTS ===

    @Test
    @DisplayName("Should create a ValidationError with all parameters")
    void constructor_WithAllParameters_ShouldCreateValidError() {
        // When
        ValidationError error = new ValidationError(
                "Field is required",
                "user.firstName",
                "validation",
                "non-empty string",
                "null",
                "REQUIRED_FIELD",
                5,
                12
        );

        // Then
        assertThat(error.message()).isEqualTo("Field is required");
        assertThat(error.path()).isEqualTo("user.firstName");
        assertThat(error.type()).isEqualTo("validation");
        assertThat(error.expected()).isEqualTo("non-empty string");
        assertThat(error.received()).isEqualTo("null");
        assertThat(error.code()).isEqualTo("REQUIRED_FIELD");
        assertThat(error.line()).isEqualTo(5);
        assertThat(error.column()).isEqualTo(12);
    }

    @Test
    @DisplayName("Should create a ValidationError with null values")
    void constructor_WithNullValues_ShouldCreateValidError() {
        // When
        ValidationError error = new ValidationError(
                "Type error",
                "field",
                "type",
                null,
                null,
                "TYPE_ERROR",
                null,
                null
        );

        // Then
        assertThat(error.message()).isEqualTo("Type error");
        assertThat(error.path()).isEqualTo("field");
        assertThat(error.type()).isEqualTo("type");
        assertThat(error.expected()).isNull();
        assertThat(error.received()).isNull();
        assertThat(error.code()).isEqualTo("TYPE_ERROR");
        assertThat(error.line()).isNull();
        assertThat(error.column()).isNull();
    }

    // === JSON SERIALIZATION TESTS ===

    @Test
    @DisplayName("Should serialize correctly to JSON")
    void serialization_ShouldProduceValidJson() throws JsonProcessingException {
        // Given
        ValidationError error = new ValidationError(
                "Size must be between 2 and 50 characters",
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
        assertThat(json).contains("\"message\":\"Size must be between 2 and 50 characters\"");
        assertThat(json).contains("\"path\":\"user.lastName\"");
        assertThat(json).contains("\"type\":\"validation\"");
        assertThat(json).contains("\"expected\":\"string[2-50]\"");
        assertThat(json).contains("\"received\":\"A\"");
        assertThat(json).contains("\"code\":\"SIZE_CONSTRAINT\"");
        assertThat(json).contains("\"line\":3");
        assertThat(json).contains("\"column\":8");
    }

    @Test
    @DisplayName("Should deserialize correctly from JSON")
    void deserialization_ShouldProduceValidObject() throws JsonProcessingException {
        // Given
        String json = """
            {
                "message": "Invalid email format",
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
        assertThat(error.message()).isEqualTo("Invalid email format");
        assertThat(error.path()).isEqualTo("user.email");
        assertThat(error.type()).isEqualTo("format");
        assertThat(error.expected()).isEqualTo("email format");
        assertThat(error.received()).isEqualTo("invalid-email");
        assertThat(error.code()).isEqualTo("EMAIL_FORMAT");
        assertThat(error.line()).isEqualTo(7);
        assertThat(error.column()).isEqualTo(15);
    }

    @Test
    @DisplayName("Should handle null values during deserialization")
    void deserialization_WithNullValues_ShouldHandleCorrectly() throws JsonProcessingException {
        // Given
        String json = """
            {
                "message": "Simple error",
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
        assertThat(error.message()).isEqualTo("Simple error");
        assertThat(error.path()).isEqualTo("field");
        assertThat(error.type()).isEqualTo("simple");
        assertThat(error.expected()).isNull();
        assertThat(error.received()).isNull();
        assertThat(error.code()).isEqualTo("SIMPLE_ERROR");
        assertThat(error.line()).isNull();
        assertThat(error.column()).isNull();
    }

    // === EQUALS AND HASHCODE TESTS ===

    @Test
    @DisplayName("Should implement equals correctly")
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
    @DisplayName("Should implement equals correctly for different contents")
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

    // === toString TESTS ===

    @Test
    @DisplayName("Should produce a readable string representation")
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

    // === SPECIFIC USE CASE TESTS ===

    @Test
    @DisplayName("Should support nested field paths")
    void nestedFieldPaths_ShouldBeSupported() {
        // Given
        ValidationError error = new ValidationError(
                "Invalid address",
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
        assertThat(error.message()).isEqualTo("Invalid address");
    }

    @Test
    @DisplayName("Should support different error types")
    void differentErrorTypes_ShouldBeSupported() {
        // Given
        ValidationError validationError = new ValidationError(
                "Validation failed", "field", "validation", "valid", "invalid", "VALIDATION", null, null
        );
        ValidationError typeError = new ValidationError(
                "Type mismatch", "field", "type", "string", "number", "TYPE_MISMATCH", null, null
        );
        ValidationError formatError = new ValidationError(
                "Invalid format", "field", "format", "yyyy-MM-dd", "invalid-date", "FORMAT", null, null
        );

        // When & Then
        assertThat(validationError.type()).isEqualTo("validation");
        assertThat(typeError.type()).isEqualTo("type");
        assertThat(formatError.type()).isEqualTo("format");
    }

    @Test
    @DisplayName("Should support position information for debugging")
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
