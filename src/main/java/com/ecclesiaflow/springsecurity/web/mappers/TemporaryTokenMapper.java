package com.ecclesiaflow.springsecurity.web.mappers;

import com.ecclesiaflow.springsecurity.web.model.TemporaryTokenRequest;
import org.springframework.stereotype.Component;

/**
 * Mapper pour les opérations de token temporaire avec modèles OpenAPI.
 * <p>
 * Cette classe gère la transformation entre les modèles OpenAPI et les données métier
 * pour les opérations de génération de token temporaire.
 * </p>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
@Component
public class TemporaryTokenMapper {

    /**
     * Extrait l'email depuis la requête OpenAPI.
     * 
     * @param request la requête de token temporaire (modèle OpenAPI)
     * @return l'email extrait
     */
    public String extractEmail(TemporaryTokenRequest request) {
        return request.getEmail();
    }
}
