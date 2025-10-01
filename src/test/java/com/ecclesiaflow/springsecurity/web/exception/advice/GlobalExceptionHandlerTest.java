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
public class GlobalExceptionHandlerTest {

    // =========================================================================
    // Exceptions Métier (404 NOT FOUND)
    // =========================================================================

    /**
     * Gère MemberNotFoundException -> HTTP 404 NOT FOUND
     */
    @ExceptionHandler(MemberNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleMemberNotFound(MemberNotFoundException ex, HttpServletRequest request) {
        return buildSimpleErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
    }

    // =========================================================================
    // Exceptions d'Authentification / Sécurité (401 UNAUTHORIZED)
    // =========================================================================

    /**
     * Gère InvalidTokenException -> HTTP 401 UNAUTHORIZED
     */
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidToken(InvalidTokenException ex, HttpServletRequest request) {
        return buildUnauthorizedErrorResponse(ex.getMessage(), request.getRequestURI());
    }

    /**
     * Gère JwtProcessingException -> HTTP 401 UNAUTHORIZED
     */
    @ExceptionHandler(JwtProcessingException.class)
    public ResponseEntity<ApiErrorResponse> handleJwtProcessing(JwtProcessingException ex, HttpServletRequest request) {
        return buildUnauthorizedErrorResponse(ex.getMessage(), request.getRequestURI());
    }

    /**
     * Gère InvalidCredentialsException (inclut BadCredentialsException de Spring Security) -> HTTP 401 UNAUTHORIZED
     */
    @ExceptionHandler({InvalidCredentialsException.class, BadCredentialsException.class})
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(AuthenticationException ex, HttpServletRequest request) {
        // Le message "Identifiants invalides" est plus générique que celui de l'exception
        return buildUnauthorizedErrorResponse("Identifiants invalides", request.getRequestURI());
    }

    /**
     * Gère les autres erreurs d'authentification génériques -> HTTP 401 UNAUTHORIZED
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return buildUnauthorizedErrorResponse("Erreur d'authentification", request.getRequestURI());
    }

    // =========================================================================
    // Exceptions de Requête / Validation (400 BAD REQUEST)
    // =========================================================================

    /**
     * Gère InvalidRequestException -> HTTP 400 BAD REQUEST
     */
    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidRequest(InvalidRequestException ex, HttpServletRequest request) {
        return buildBadRequestErrorResponse(ex.getMessage(), request.getRequestURI());
    }

    /**
     * Gère HttpMessageNotReadableException (JSON mal formé) -> HTTP 400 BAD REQUEST
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidJson(HttpMessageNotReadableException ex, HttpServletRequest request) {
        List<ValidationError> errors = new ArrayList<>();
        errors.add(new ValidationError(
                "Requête JSON mal formée",
                "request",
                "parsing",
                "MalformedJson",
                null, // Expected
                null, // Received
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

    /**
     * Gère MethodArgumentNotValidException (validation @Valid sur le corps) -> HTTP 400 BAD REQUEST
     */
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
                    null, // Valeur attendue non disponible ici
                    error.getDefaultMessage(), // On réutilise le message par défaut comme code d'erreur
                    null,
                    null
            ));
        });

        ApiErrorResponse errorResponse = ApiErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Erreur de validation des données")
                .path(request.getRequestURI())
                .errors(errors)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Gère ConstraintViolationException (validation Bean Validation sur les paramètres/services) -> HTTP 400 BAD REQUEST
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        List<ValidationError> errors = new ArrayList<>();

        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            // Utiliser le nom simple de l'annotation comme code d'erreur
            String code = violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName();

            errors.add(new ValidationError(
                    violation.getMessage(),
                    violation.getPropertyPath().toString(),
                    "constraint",
                    violation.getInvalidValue() != null ? violation.getInvalidValue().toString() : "null", // Received
                    code, // Code
                    violation.getMessage(), // Expected (pas vraiment attendu, mais pour le format)
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

    /**
     * Gère MissingRequestHeaderException -> HTTP 400 BAD REQUEST
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingRequestHeader(MissingRequestHeaderException ex, HttpServletRequest request) {
        String error = "En-tête requis manquant: " + ex.getHeaderName();
        return buildBadRequestErrorResponse(error, request.getRequestURI());
    }

    /**
     * Gère IllegalArgumentException (souvent utilisé pour les paramètres invalides) -> HTTP 400 BAD REQUEST
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return buildBadRequestErrorResponse(ex.getMessage(), request.getRequestURI());
    }

    // =========================================================================
    // Exceptions d'Autorisation / Cas généraux
    // =========================================================================

    /**
     * Gère AccessDeniedException (Spring Security) -> HTTP 403 FORBIDDEN
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return buildSimpleErrorResponse(HttpStatus.FORBIDDEN, "Accès refusé", request.getRequestURI());
    }

    /**
     * Gère RuntimeException non capturée -> HTTP 500 INTERNAL SERVER ERROR
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiErrorResponse> handleRuntimeException(RuntimeException ex, HttpServletRequest request) {
        return buildSimpleErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur interne est survenue", request.getRequestURI());
    }

    /**
     * Gère Exception générique -> HTTP 500 INTERNAL SERVER ERROR
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleAll(Exception ex, HttpServletRequest request) {
        return buildSimpleErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur interne est survenue", request.getRequestURI());
    }

    // =========================================================================
    // Méthodes utilitaires pour la construction de réponses
    // =========================================================================

    /**
     * Construit une réponse d'erreur 400 Bad Request avec des erreurs de validation.
     */
    private ResponseEntity<ApiErrorResponse> buildBadRequestErrorResponse(String message, String path) {
        // Ajout d'une erreur de validation générique pour les 400 simples
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
        // Ajout d'une erreur de validation pour l'authentification
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

    /**
     * Construit une réponse d'erreur simple sans liste d'erreurs détaillées.
     */
    private ResponseEntity<ApiErrorResponse> buildSimpleErrorResponse(HttpStatus status, String message, String path) {
        ApiErrorResponse errorResponse = ApiErrorResponse.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(path)
                .errors(null) // Pas d'erreurs de validation détaillées
                .build();

        return new ResponseEntity<>(errorResponse, status);
    }
}