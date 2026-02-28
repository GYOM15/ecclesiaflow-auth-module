package com.ecclesiaflow.springsecurity.web.exception.advice;

import com.ecclesiaflow.springsecurity.business.exceptions.CompensationFailedException;
import com.ecclesiaflow.springsecurity.web.exception.*;
import com.ecclesiaflow.springsecurity.web.exception.model.ApiErrorResponse;
import com.ecclesiaflow.springsecurity.web.exception.model.ValidationError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.ArrayList;
import java.util.List;

/**
 * Global exception handler for the EcclesiaFlow Auth Module REST API.
 * <p>
 * This class centralizes the handling of all exceptions thrown by authentication controllers
 * and transforms them into standardized HTTP responses using the {@link ApiErrorResponse} format.
 * </p>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 * @see ApiErrorResponse
 * @see ValidationError
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidToken(InvalidTokenException ex, HttpServletRequest request) {
        return buildUnauthorizedErrorResponse("Invalid or expired token", request.getRequestURI());
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidRequest(InvalidRequestException ex, HttpServletRequest request) {
        return buildBadRequestErrorResponse(ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidJson(HttpMessageNotReadableException ex, HttpServletRequest request) {
        List<ValidationError> errors = new ArrayList<>();
        errors.add(new ValidationError(
            "Malformed JSON request",
            "request",
            "parsing",
            "MalformedJson",
            "Malformed JSON request",
            "MalformedJson",
            null,
            null
        ));

        ApiErrorResponse errorResponse = ApiErrorResponse.builder()
            .status(HttpStatus.BAD_REQUEST.value())
            .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
            .message("Malformed JSON request")
            .path(request.getRequestURI())
            .errors(errors)
            .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ValidationError> errors = new ArrayList<>();
        
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = error instanceof FieldError ? ((FieldError) error).getField() : error.getObjectName();
            errors.add(new ValidationError(
                error.getDefaultMessage(),
                fieldName,
                "validation",
                error.getCode(),
                error.getDefaultMessage(),
                error.getCode(),
                null,
                null
            ));
        });

        ApiErrorResponse errorResponse = ApiErrorResponse.builder()
            .status(HttpStatus.BAD_REQUEST.value())
            .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
            .message("Authentication data validation error")
            .path(request.getRequestURI())
            .errors(errors)
            .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        List<ValidationError> errors = new ArrayList<>();
        
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            errors.add(new ValidationError(
                violation.getMessage(),
                violation.getPropertyPath().toString(),
                "constraint",
                violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName(),
                violation.getInvalidValue() != null ? violation.getInvalidValue().toString() : "null",
                violation.getConstraintDescriptor().getAttributes().get("groups") != null ? 
                    violation.getConstraintDescriptor().getAttributes().get("groups").toString() : "CONSTRAINT_VIOLATION",
                null,
                null
            ));
        }

        ApiErrorResponse errorResponse = ApiErrorResponse.builder()
            .status(HttpStatus.BAD_REQUEST.value())
            .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
            .message("Constraint validation error")
            .path(request.getRequestURI())
            .errors(errors)
            .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingRequestHeader(MissingRequestHeaderException ex, HttpServletRequest request) {
        String error = "Missing required header: " + ex.getHeaderName();
        return buildBadRequestErrorResponse(error, request.getRequestURI());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        StringBuilder message = new StringBuilder("HTTP method not supported");
        if (ex.getSupportedHttpMethods() != null && !ex.getSupportedHttpMethods().isEmpty()) {
            message.append(": allowed → ")
                    .append(ex.getSupportedHttpMethods());
        }

        return buildSimpleErrorResponse(HttpStatus.NOT_FOUND, message.toString(), request.getRequestURI());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest request) {
        return buildSimpleErrorResponse(HttpStatus.NOT_FOUND, "Resource not found", request.getRequestURI());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return buildSimpleErrorResponse(HttpStatus.FORBIDDEN, "Access denied", request.getRequestURI());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return buildBadRequestErrorResponse(ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(CompensationFailedException.class)
    public ResponseEntity<ApiErrorResponse> handleCompensationFailed(CompensationFailedException ex, HttpServletRequest request) {
        return buildSimpleErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred", request.getRequestURI());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiErrorResponse> handleRuntimeException(RuntimeException ex, HttpServletRequest request) {
        return buildSimpleErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred", request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleAll(Exception ex, HttpServletRequest request) {
        return buildSimpleErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred", request.getRequestURI());
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFoundExceptions(
            NoHandlerFoundException ex, HttpServletRequest request) {

        HttpStatus status = HttpStatus.NOT_FOUND;

        ValidationError error = new ValidationError(
                "The requested resource was not found",
                "resource",
                "routing",
                "NOT_FOUND",
                ex.getMessage(),
                "NOT_FOUND",
                null,
                null
        );

        ApiErrorResponse response = ApiErrorResponse.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .message("Resource not found")
                .path(request.getRequestURI())
                .errors(List.of(error))
                .build();

        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ApiErrorResponse> handle406(HttpMediaTypeNotAcceptableException ex,
                                                      HttpServletRequest request) {
        return buildSimpleErrorResponse(
                HttpStatus.NOT_ACCEPTABLE,
                "Unsupported media type. Check the Accept header.",
                request.getRequestURI()
        );
    }


    /**
     * Builds a 400 Bad Request error response with validation errors.
     */
    private ResponseEntity<ApiErrorResponse> buildBadRequestErrorResponse(String message, String path) {
        List<ValidationError> errors = new ArrayList<>();
        errors.add(new ValidationError(
            message,
            "request",
            "validation",
            "BadRequest",
            message,
            "BAD_REQUEST",
            null,
            null
        ));

        ApiErrorResponse errorResponse = ApiErrorResponse.builder()
            .status(HttpStatus.BAD_REQUEST.value())
            .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
            .message(message)
            .path(path)
            .errors(errors)
            .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Builds a 401 Unauthorized error response for authentication errors.
     */
    private ResponseEntity<ApiErrorResponse> buildUnauthorizedErrorResponse(String message, String path) {
        List<ValidationError> errors = new ArrayList<>();
        errors.add(new ValidationError(
            message,
            "authentication",
            "security",
            "Unauthorized",
            message,
            "UNAUTHORIZED",
            null,
            null
        ));

        ApiErrorResponse errorResponse = ApiErrorResponse.builder()
            .status(HttpStatus.UNAUTHORIZED.value())
            .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
            .message(message)
            .path(path)
            .errors(errors)
            .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    private ResponseEntity<ApiErrorResponse> buildSimpleErrorResponse(HttpStatus status, String message, String path) {
        ApiErrorResponse errorResponse = ApiErrorResponse.builder()
            .status(status.value())
            .error(status.getReasonPhrase())
            .message(message)
            .path(path)
            .errors(null)
            .build();
            
        return new ResponseEntity<>(errorResponse, status);
    }
}