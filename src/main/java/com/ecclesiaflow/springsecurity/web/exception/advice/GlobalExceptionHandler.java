package com.ecclesiaflow.springsecurity.web.exception.advice;

import com.ecclesiaflow.springsecurity.business.exceptions.MemberNotFoundException;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.reactive.resource.NoResourceFoundException;

import java.util.ArrayList;
import java.util.List;

/**
 * Gestionnaire global d'exceptions pour l'API REST EcclesiaFlow Auth Module.
 * <p>
 * Cette classe centralise la gestion de toutes les exceptions levées par les contrôleurs
 * d'authentification et les transforme en réponses HTTP standardisées avec le format {@link ApiErrorResponse}.
 * </p>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 * @see ApiErrorResponse
 * @see ValidationError
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MemberNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleMemberNotFound(MemberNotFoundException ex, HttpServletRequest request) {
        return buildSimpleErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidToken(InvalidTokenException ex, HttpServletRequest request) {
        return buildUnauthorizedErrorResponse("Token invalide ou expiré", request.getRequestURI());
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidRequest(InvalidRequestException ex, HttpServletRequest request) {
        return buildBadRequestErrorResponse(ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(JwtProcessingException.class)
    public ResponseEntity<ApiErrorResponse> handleJwtProcessing(JwtProcessingException ex, HttpServletRequest request) {
        return buildUnauthorizedErrorResponse("Erreur de traitement du token", request.getRequestURI());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidJson(HttpMessageNotReadableException ex, HttpServletRequest request) {
        List<ValidationError> errors = new ArrayList<>();
        errors.add(new ValidationError(
            "Requête JSON mal formée",
            "request",
            "parsing",
            "MalformedJson",
            "Requête JSON mal formée",
            "MalformedJson",
            null,
            null
        ));

        ApiErrorResponse errorResponse = ApiErrorResponse.builder()
            .status(HttpStatus.BAD_REQUEST.value())
            .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
            .message("Requête JSON mal formée")
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
            .message("Erreur de validation des données d'authentification")
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
            .message("Erreur de validation des contraintes")
            .path(request.getRequestURI())
            .errors(errors)
            .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest request) {
        return buildUnauthorizedErrorResponse("Identifiants invalides", request.getRequestURI());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        return buildUnauthorizedErrorResponse("Identifiants invalides", request.getRequestURI());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return buildUnauthorizedErrorResponse("Erreur d'authentification", request.getRequestURI());
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingRequestHeader(MissingRequestHeaderException ex, HttpServletRequest request) {
        String error = "En-tête requis manquant: " + ex.getHeaderName();
        return buildBadRequestErrorResponse(error, request.getRequestURI());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return buildSimpleErrorResponse(HttpStatus.FORBIDDEN, "Accès refusé", request.getRequestURI());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return buildBadRequestErrorResponse(ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiErrorResponse> handleRuntimeException(RuntimeException ex, HttpServletRequest request) {
        return buildSimpleErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur interne est survenue", request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleAll(Exception ex, HttpServletRequest request) {
        return buildSimpleErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur interne est survenue", request.getRequestURI());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFoundExceptions(
            NoResourceFoundException ex, WebRequest request) {

        HttpStatus status = HttpStatus.NOT_FOUND;

        ValidationError error = new ValidationError(
                "La ressource demandée est introuvable",
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
                .message("Ressource non trouvée")
                .path(request.getDescription(false).replace("uri=", ""))
                .errors(List.of(error))
                .build();

        return ResponseEntity.status(status).body(response);
    }


    /**
     * Construit une réponse d'erreur 400 Bad Request avec des erreurs de validation.
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
     * Construit une réponse d'erreur 401 Unauthorized pour les erreurs d'authentification.
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
