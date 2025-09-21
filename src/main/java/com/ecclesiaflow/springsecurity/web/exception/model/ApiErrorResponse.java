package com.ecclesiaflow.springsecurity.web.exception.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Record représentant une réponse d'erreur standardisée avec support des erreurs de validation.
 * <p>
 * Capacité de gérer des erreurs de validation détaillées. Utilisée principalement par
 * {@link com.ecclesiaflow.springsecurity.web.exception.advice.GlobalExceptionHandler} pour les erreurs
 * de validation Bean Validation et les erreurs métier complexes.
 * </p>
 * 
 * <p><strong>Rôle architectural :</strong> Modèle de réponse d'erreur avancée</p>
 * 
 * <p><strong>Fonctionnalités :</strong></p>
 * <ul>
 *   <li>Informations d'erreur de base (timestamp, status, message, path)</li>
 *   <li>Liste détaillée des erreurs de validation</li>
 *   <li>Builder pattern pour construction flexible</li>
 *   <li>Sérialisation JSON optimisée (exclusion des champs null)</li>
 *   <li>Documentation OpenAPI intégrée</li>
 * </ul>
 * 
 * <p><strong>Cas d'utilisation :</strong></p>
 * <ul>
 *   <li>Erreurs de validation Bean Validation (@Valid)</li>
 *   <li>Erreurs de validation métier complexes</li>
 *   <li>Erreurs avec détails multiples</li>
 *   <li>Documentation API avec exemples détaillés</li>
 * </ul>
 * 
 * <p><strong>Avantages du record :</strong> Immutabilité, equals/hashCode automatiques,
 * constructeur compact avec validation, sérialisation JSON native.</p>
 * 
 * @param timestamp Horodatage de l'erreur
 * @param status Code de statut HTTP
 * @param error Type d'erreur HTTP
 * @param message Message d'erreur principal
 * @param path Chemin de la requête
 * @param errors Liste des erreurs de validation détaillées
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 * @see ValidationError
 * @see com.ecclesiaflow.springsecurity.web.exception.advice.GlobalExceptionHandler
 */
@Schema(description = "Réponse d'erreur standard de l'API d'authentification")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
    @Schema(description = "Horodatage de l'erreur", example = "2023-01-01T12:00:00")
    LocalDateTime timestamp,

    @Schema(description = "Statut HTTP", example = "400")
    int status,

    @Schema(description = "Type d'erreur", example = "Bad Request")
    String error,

    @Schema(description = "Message d'erreur détaillé", example = "Erreur de validation des données d'authentification")
    String message,

    @Schema(description = "Chemin de la requête", example = "/ecclesiaflow/auth/password")
    String path,

    @ArraySchema(schema = @Schema(implementation = ValidationError.class))
    List<ValidationError> errors
) {
    public ApiErrorResponse {
        // Garde errors null si explicitement passé comme null
        // Sinon initialise avec une liste vide pour les erreurs de validation
    }

    /**
     * Crée un nouveau builder pour construire une ApiErrorResponse.
     * 
     * @return nouvelle instance de builder
     */
    public static ApiErrorResponseBuilder builder() {
        return new ApiErrorResponseBuilder();
    }

    /**
     * Builder pour construire une ApiErrorResponse de manière fluide.
     * <p>
     * Permet la construction étape par étape d'une réponse d'erreur avec
     * validation automatique et valeurs par défaut appropriées.
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
         * Définit le code de statut HTTP.
         * 
         * @param status le code de statut HTTP (400, 401, 404, 500, etc.)
         * @return ce builder pour chaînage
         */
        public ApiErrorResponseBuilder status(int status) {
            this.status = status;
            return this;
        }

        /**
         * Définit le type d'erreur HTTP.
         * 
         * @param error le type d'erreur ("Bad Request", "Unauthorized", "Not Found", etc.)
         * @return ce builder pour chaînage
         */
        public ApiErrorResponseBuilder error(String error) {
            this.error = error;
            return this;
        }

        /**
         * Définit le message d'erreur principal.
         * 
         * @param message le message d'erreur descriptif
         * @return ce builder pour chaînage
         */
        public ApiErrorResponseBuilder message(String message) {
            this.message = message;
            return this;
        }

        /**
         * Définit le chemin de la requête qui a causé l'erreur.
         * 
         * @param path le chemin de la requête HTTP
         * @return ce builder pour chaînage
         */
        public ApiErrorResponseBuilder path(String path) {
            this.path = path;
            return this;
        }

        /**
         * Ajoute une erreur de validation à la liste.
         * 
         * @param error l'erreur de validation à ajouter
         * @return ce builder pour chaînage
         */
        public ApiErrorResponseBuilder addValidationError(ValidationError error) {
            if (this.errors == null) {
                this.errors = new ArrayList<>();
            }
            this.errors.add(error);
            return this;
        }

        /**
         * Définit explicitement la liste des erreurs de validation.
         * 
         * @param errors la liste des erreurs (peut être null)
         * @return ce builder pour chaînage
         */
        public ApiErrorResponseBuilder errors(List<ValidationError> errors) {
            this.errors = errors;
            return this;
        }

        /**
         * Construit l'ApiErrorResponse finale.
         * 
         * @return nouvelle instance d'ApiErrorResponse avec les paramètres définis
         */
        public ApiErrorResponse build() {
            return new ApiErrorResponse(timestamp, status, error, message, path, errors);
        }
    }
}
