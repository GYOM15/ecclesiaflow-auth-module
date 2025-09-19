package com.ecclesiaflow.springsecurity.web.payloads;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO pour la requête de génération de token temporaire.
 * <p>
 * Cette classe représente les données nécessaires pour générer un token temporaire
 * destiné à la définition de mot de passe après confirmation d'email.
 * </p>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
@Data
public class TemporaryTokenRequest {
    
    /**
     * Email du membre pour lequel générer le token temporaire.
     */
    @NotBlank(message = "L'email est requis")
    @Email(message = "L'email doit être valide")
    private String email;
}
