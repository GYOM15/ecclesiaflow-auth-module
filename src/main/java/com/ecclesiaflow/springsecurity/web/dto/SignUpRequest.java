package com.ecclesiaflow.springsecurity.web.dto;

import com.ecclesiaflow.springsecurity.business.domain.MemberRegistration;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO représentant une requête d'inscription d'un nouveau membre via l'API web.
 * <p>
 * Cette classe encapsule les données d'inscription envoyées par le client lors
 * d'une demande de création de compte. Utilise les annotations de validation Jakarta
 * pour assurer l'intégrité des données avant traitement.
 * </p>
 * 
 * <p><strong>Validations appliquées :</strong></p>
 * <ul>
 *   <li>Prénom et nom : obligatoires, entre 2 et 50 caractères</li>
 *   <li>Email : obligatoire, format valide, maximum 100 caractères</li>
 *   <li>Mot de passe : obligatoire, entre 6 et 100 caractères</li>
 * </ul>
 * 
 * <p><strong>Cas d'utilisation typiques :</strong></p>
 * <ul>
 *   <li>Réception des données d'inscription via formulaire web</li>
 *   <li>Validation automatique par Spring Boot</li>
 *   <li>Conversion en objet métier {@link MemberRegistration}</li>
 * </ul>
 * 
 * <p><strong>Garanties :</strong> Thread-safe, validation automatique.</p>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
@Data
public class SignUpRequest {
    /**
     * Prénom du membre.
     * <p>
     * Obligatoire, compris entre 2 et 50 caractères.
     */
    @NotBlank(message = "Le prénom est obligatoire")
    @Size(min = 2, max = 50, message = "Le prénom doit contenir entre 2 et 50 caractères")
    private String firstName;

    /**
     * Nom de famille du membre.
     * <p>
     * Obligatoire, compris entre 2 et 50 caractères.
     */
    @NotBlank(message = "Le nom de famille est obligatoire")
    @Size(min = 2, max = 50, message = "Le nom de famille doit contenir entre 2 et 50 caractères")
    private String lastName;

    /**
     * Adresse email du membre.
     * <p>
     * Obligatoire, format valide, maximum 100 caractères.
     */
    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "L'email doit être valide")
    @Size(max = 100, message = "L'email ne peut pas dépasser 100 caractères")
    private String email;

    /**
     * Mot de passe du membre.
     * <p>
     * Obligatoire, compris entre 6 et 100 caractères.
     */
    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 6, max = 100, message = "Le mot de passe doit contenir entre 6 et 100 caractères")
    private String password;
}
