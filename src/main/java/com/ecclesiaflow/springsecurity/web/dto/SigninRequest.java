package com.ecclesiaflow.springsecurity.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO représentant une requête de connexion d'un utilisateur via l'API web.
 * <p>
 * Cette classe encapsule les identifiants d'authentification envoyés par le client
 * lors d'une tentative de connexion. Utilise les annotations de validation Jakarta
 * pour assurer l'intégrité des données avant traitement.
 * </p>
 * 
 * <p><strong>Validations appliquées :</strong></p>
 * <ul>
 *   <li>Email : obligatoire et au format valide</li>
 *   <li>Mot de passe : obligatoire (non vide)</li>
 * </ul>
 * 
 * <p><strong>Cas d'utilisation typiques :</strong></p>
 * <ul>
 *   <li>Réception des identifiants via formulaire de connexion</li>
 *   <li>Validation automatique par Spring Boot</li>
 *   <li>Conversion en objet métier {@link com.ecclesiaflow.springsecurity.business.domain.SigninCredentials}</li>
 * </ul>
 * 
 * <p><strong>Garanties :</strong> Thread-safe, validation automatique.</p>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
@Data
public class SigninRequest {

    /**
     * Adresse email de l'utilisateur.
     * <p>
     * Utilisée comme identifiant unique pour l'authentification.
     * Obligatoire et doit respecter le format email valide.
     * </p>
     */
    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "L'email doit être valide")
    private String email;

    /**
     * Mot de passe en clair de l'utilisateur.
     * <p>
     * Sera validé contre le hash stocké en base de données.
     * Obligatoire et ne doit pas être vide.
     * </p>
     */
    @NotBlank(message = "Le mot de passe est obligatoire")
    private String password;
}
