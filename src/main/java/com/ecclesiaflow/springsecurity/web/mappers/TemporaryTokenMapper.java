package com.ecclesiaflow.springsecurity.web.mappers;

import com.ecclesiaflow.springsecurity.business.domain.token.TemporaryToken;
import com.ecclesiaflow.springsecurity.web.model.TemporaryTokenRequest;
import org.springframework.stereotype.Component;

/**
 * Mapper pour les opérations de token temporaire - Couche Web vers Domain.
 * <p>
 * Transforme les DTOs OpenAPI (couche web) en objets domain purs (couche métier).
 * Respecte la séparation des couches de la Clean Architecture.
 * </p>
 * 
 * <p><strong>Rôle architectural:</strong> Mapper Web → Domain</p>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
@Component
public class TemporaryTokenMapper {

    /**
     * Transforme un DTO OpenAPI en Value Object domain.
     * <p>
     * Effectue la transformation de la couche web vers la couche domain,
     * en assurant l'indépendance du domain vis-à-vis du framework web.
     * </p>
     * 
     * @param temporaryTokenRequest la requête OpenAPI (couche web)
     * @return le Value Object domain correspondant
     * @throws IllegalArgumentException si les données du DTO sont invalides
     */
    public TemporaryToken toDomain(
            TemporaryTokenRequest temporaryTokenRequest) {
        
        return new TemporaryToken(
                temporaryTokenRequest.getEmail(),
                temporaryTokenRequest.getMemberId()
        );
    }
}
