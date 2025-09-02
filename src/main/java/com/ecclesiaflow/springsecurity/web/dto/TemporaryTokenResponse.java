package com.ecclesiaflow.springsecurity.web.dto;

import lombok.Builder;
import lombok.Data;

/**
 * DTO pour la réponse de génération de token temporaire.
 * <p>
 * Cette classe représente la réponse contenant le token temporaire généré
 * et ses informations d'expiration.
 * </p>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
@Data
@Builder
public class TemporaryTokenResponse {
    
    /**
     * Token temporaire JWT généré.
     */
    private String temporaryToken;
    
    /**
     * Durée d'expiration en secondes.
     */
    private int expiresIn;
    
    /**
     * Message de confirmation.
     */
    private String message;
}
