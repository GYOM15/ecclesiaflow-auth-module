package com.ecclesiaflow.springsecurity.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Réponse de gestion de mot de passe avec tokens d'authentification")
public class PasswordManagementResponse {
    
    @Schema(description = "Message de confirmation", example = "Mot de passe défini avec succès")
    private String message;
    
    @Schema(description = "Token d'accès JWT", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String accessToken;
    
    @Schema(description = "Token de rafraîchissement JWT", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String refreshToken;
    
    @Schema(description = "Durée de validité du token d'accès en secondes", example = "3600")
    private Long expiresIn;
}
