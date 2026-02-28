package com.ecclesiaflow.springsecurity.web.exception.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Record representing a standardized error response with validation error support.
 * <p>
 * Capable of handling detailed validation errors. Primarily used by
 * {@link com.ecclesiaflow.springsecurity.web.exception.advice.GlobalExceptionHandler} for
 * Bean Validation errors and complex business errors.
 * </p>
 * 
 *
 * <p><strong>Features:</strong></p>
 * <ul>
 *   <li>Basic error information (timestamp, status, message, path)</li>
 *   <li>Detailed list of validation errors</li>
 *   <li>Builder pattern for flexible construction</li>
 *   <li>Optimized JSON serialization (null fields excluded)</li>
 *   <li>Integrated OpenAPI documentation</li>
 * </ul>
 * 
 * <p><strong>Use cases:</strong></p>
 * <ul>
 *   <li>Bean Validation errors (@Valid)</li>
 *   <li>Complex business validation errors</li>
 *   <li>Errors with multiple details</li>
 *   <li>API documentation with detailed examples</li>
 * </ul>
 * 
 * <p><strong>Record advantages:</strong> Immutability, automatic equals/hashCode,
 * compact constructor with validation, native JSON serialization.</p>
 * 
 * @param timestamp Error timestamp
 * @param status HTTP status code
 * @param error HTTP error type
 * @param message Main error message
 * @param path Request path
 * @param errors Detailed list of validation errors
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 * @see ValidationError
 * @see com.ecclesiaflow.springsecurity.web.exception.advice.GlobalExceptionHandler
 */
@Schema(description = "Standard error response for the authentication API")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
    @Schema(description = "Error timestamp", example = "2023-01-01T12:00:00")
    LocalDateTime timestamp,

    @Schema(description = "HTTP status code", example = "400")
    int status,

    @Schema(description = "Error type", example = "Bad Request")
    String error,

    @Schema(description = "Detailed error message", example = "Authentication data validation error")
    String message,

    @Schema(description = "Request path", example = "/ecclesiaflow/auth/password")
    String path,

    @ArraySchema(schema = @Schema(implementation = ValidationError.class))
    List<ValidationError> errors
) {
    public ApiErrorResponse {
        // Keep errors null if explicitly passed as null
        // Otherwise initialize with an empty list for validation errors
    }

    /**
     * Creates a new builder to construct an ApiErrorResponse.
     * 
     * @return new builder instance
     */
    public static ApiErrorResponseBuilder builder() {
        return new ApiErrorResponseBuilder();
    }

    /**
     * Builder for fluent construction of an ApiErrorResponse.
     * <p>
     * Allows step-by-step construction of an error response with
     * automatic validation and appropriate default values.
     * </p>
     */
    public static class ApiErrorResponseBuilder {
        private LocalDateTime timestamp = LocalDateTime.now();
        private int status;
        private String error;
        private String message;
        private String path;
        private List<ValidationError> errors = null;

        /**
         * Sets the HTTP status code.
         * 
         * @param status the HTTP status code (400, 401, 404, 500, etc.)
         * @return this builder for chaining
         */
        public ApiErrorResponseBuilder status(int status) {
            this.status = status;
            return this;
        }

        /**
         * Sets the HTTP error type.
         * 
         * @param error the error type ("Bad Request", "Unauthorized", "Not Found", etc.)
         * @return this builder for chaining
         */
        public ApiErrorResponseBuilder error(String error) {
            this.error = error;
            return this;
        }

        /**
         * Sets the main error message.
         * 
         * @param message the descriptive error message
         * @return this builder for chaining
         */
        public ApiErrorResponseBuilder message(String message) {
            this.message = message;
            return this;
        }

        /**
         * Sets the request path that caused the error.
         * 
         * @param path the HTTP request path
         * @return this builder for chaining
         */
        public ApiErrorResponseBuilder path(String path) {
            this.path = path;
            return this;
        }

        /**
         * Adds a validation error to the list.
         * 
         * @param error the validation error to add
         * @return this builder for chaining
         */
        public ApiErrorResponseBuilder addValidationError(ValidationError error) {
            if (this.errors == null) {
                this.errors = new ArrayList<>();
            }
            this.errors.add(error);
            return this;
        }

        /**
         * Explicitly sets the list of validation errors.
         * 
         * @param errors the list of errors (can be null)
         * @return this builder for chaining
         */
        public ApiErrorResponseBuilder errors(List<ValidationError> errors) {
            this.errors = errors;
            return this;
        }

        /**
         * Builds the final ApiErrorResponse.
         * 
         * @return new ApiErrorResponse instance with the configured parameters
         */
        public ApiErrorResponse build() {
            return new ApiErrorResponse(timestamp, status, error, message, path, errors);
        }
    }
}
