package com.ecclesiaflow.springsecurity.web.mappers;

import com.ecclesiaflow.springsecurity.web.dto.TemporaryTokenRequest;
import com.ecclesiaflow.springsecurity.web.dto.TemporaryTokenResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper pour les opérations de token temporaire.
 * <p>
 * Cette classe gère la transformation entre les DTOs web et les données métier
 * pour les opérations de génération de token temporaire.
 * </p>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
@Component
public class TemporaryTokenMapper {

    /**
     * Extrait l'email depuis la requête DTO.
     * 
     * @param request la requête de token temporaire
     * @return l'email extrait
     */
    public String extractEmail(TemporaryTokenRequest request) {
        return request.getEmail();
    }
    
    /**
     * Crée une réponse DTO à partir du token généré.
     * 
     * @param temporaryToken le token temporaire généré
     * @return la réponse DTO
     */
    public TemporaryTokenResponse toResponse(String temporaryToken) {
        return TemporaryTokenResponse.builder()
                .temporaryToken(temporaryToken)
                .expiresIn(900) // 15 min
                .message("Token temporaire JWT généré avec succès")
                .build();
    }
}
