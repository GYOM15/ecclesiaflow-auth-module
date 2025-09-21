package com.ecclesiaflow.springsecurity.web.exception.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Record représentant le détail d'une erreur de validation spécifique.
 * <p>
 * Cette classe encapsule toutes les informations nécessaires pour décrire
 * précisément une erreur de validation, incluant le contexte, la localisation
 * et les détails techniques. Utilisée dans {@link ApiErrorResponse} pour
 * fournir des informations détaillées sur les erreurs de validation.
 * </p>
 * 
 * <p><strong>Rôle architectural :</strong> Modèle de détail d'erreur de validation</p>
 * 
 * <p><strong>Informations capturées :</strong></p>
 * <ul>
 *   <li>Message d'erreur localisé pour l'utilisateur</li>
 *   <li>Chemin du champ en erreur (notation pointée)</li>
 *   <li>Type d'erreur (validation, type, format, etc.)</li>
 *   <li>Valeurs attendues vs reçues pour comparaison</li>
 *   <li>Code d'erreur standardisé pour traitement automatique</li>
 *   <li>Position dans le document (ligne/colonne) si applicable</li>
 * </ul>
 * 
 * <p><strong>Cas d'utilisation :</strong></p>
 * <ul>
 *   <li>Erreurs de validation Bean Validation (@NotNull, @Size, etc.)</li>
 *   <li>Erreurs de désérialisation JSON</li>
 *   <li>Erreurs de validation métier avec localisation précise</li>
 *   <li>Debugging et diagnostic des problèmes de validation</li>
 * </ul>
 * 
 * <p><strong>Format du chemin :</strong> Notation pointée (ex: "user.address.street")
 * pour identifier précisément le champ en erreur dans des structures imbriquées.</p>
 * 
 * @param message Message d'erreur localisé
 * @param path Chemin du champ en erreur (notation pointée)
 * @param type Type d'erreur (validation, type, format, etc.)
 * @param expected Valeur ou type attendu
 * @param received Valeur reçue qui a causé l'erreur
 * @param code Code d'erreur standardisé
 * @param line Numéro de ligne dans le document source (optionnel)
 * @param column Numéro de colonne dans le document source (optionnel)
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 * @see ApiErrorResponse
 * @see com.ecclesiaflow.springsecurity.web.exception.advices.GlobalExceptionHandler
 */
@Schema(description = "Détail d'une erreur de validation")
public record ValidationError(
    @Schema(description = "Message d'erreur", example = "Le mot de passe est obligatoire")
    String message,

    @Schema(description = "Chemin du champ en erreur", example = "password")
    String path,

    @Schema(description = "Type d'erreur", example = "validation")
    String type,

    @Schema(description = "Type attendu", example = "string")
    String expected,

    @Schema(description = "Valeur reçue", example = "null")
    String received,

    @Schema(description = "Code d'erreur", example = "NotBlank")
    String code,

    @Schema(description = "Numéro de ligne", example = "6")
    Integer line,

    @Schema(description = "Numéro de colonne", example = "15")
    Integer column
) {}
