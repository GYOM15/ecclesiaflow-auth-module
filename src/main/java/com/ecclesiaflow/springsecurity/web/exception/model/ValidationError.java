package com.ecclesiaflow.springsecurity.web.exception.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Record representing the detail of a specific validation error.
 * <p>
 * This class encapsulates all information needed to precisely describe
 * a validation error, including context, location, and technical details.
 * Used in {@link ApiErrorResponse} to provide detailed validation error information.
 * </p>
 * 
 * <p><strong>Architectural role:</strong> Validation error detail model</p>
 * 
 * <p><strong>Captured information:</strong></p>
 * <ul>
 *   <li>Localized error message for the user</li>
 *   <li>Field path in error (dot notation)</li>
 *   <li>Error type (validation, type, format, etc.)</li>
 *   <li>Expected vs received values for comparison</li>
 *   <li>Standardized error code for automated processing</li>
 *   <li>Position in the document (line/column) if applicable</li>
 * </ul>
 * 
 * <p><strong>Use cases:</strong></p>
 * <ul>
 *   <li>Bean Validation errors (@NotNull, @Size, etc.)</li>
 *   <li>JSON deserialization errors</li>
 *   <li>Business validation errors with precise location</li>
 *   <li>Debugging and diagnosing validation issues</li>
 * </ul>
 * 
 * <p><strong>Path format:</strong> Dot notation (e.g., "user.address.street")
 * to precisely identify the field in error within nested structures.</p>
 * 
 * @param message Localized error message
 * @param path Field path in error (dot notation)
 * @param type Error type (validation, type, format, etc.)
 * @param expected Expected value or type
 * @param received Received value that caused the error
 * @param code Standardized error code
 * @param line Line number in the source document (optional)
 * @param column Column number in the source document (optional)
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 * @see ApiErrorResponse
 * @see com.ecclesiaflow.springsecurity.web.exception.advice.GlobalExceptionHandler
 */
@Schema(description = "Validation error detail")
public record ValidationError(
    @Schema(description = "Error message", example = "Password is required")
    String message,

    @Schema(description = "Field path in error", example = "password")
    String path,

    @Schema(description = "Error type", example = "validation")
    String type,

    @Schema(description = "Expected type", example = "string")
    String expected,

    @Schema(description = "Received value", example = "null")
    String received,

    @Schema(description = "Error code", example = "NotBlank")
    String code,

    @Schema(description = "Line number", example = "6")
    Integer line,

    @Schema(description = "Column number", example = "15")
    Integer column
) {}
