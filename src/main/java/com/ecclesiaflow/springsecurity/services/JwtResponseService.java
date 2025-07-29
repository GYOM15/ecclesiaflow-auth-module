package com.ecclesiaflow.springsecurity.services;

import com.ecclesiaflow.springsecurity.dto.JwtAuthenticationResponse;
import com.ecclesiaflow.springsecurity.entities.Member;

/**
 * Service dédié à la construction des réponses JWT
 * Responsabilité unique : créer et formater les réponses d'authentification
 */
public interface JwtResponseService {
    
    /**
     * Crée une réponse JWT complète avec token et refresh token
     * @param member le membre authentifié
     * @return la réponse JWT formatée
     */
    JwtAuthenticationResponse createAuthenticationResponse(Member member);
    
    /**
     * Crée une réponse JWT pour un refresh token
     * @param member le membre
     * @param existingRefreshToken le refresh token existant à réutiliser
     * @return la réponse JWT avec nouveau token et ancien refresh token
     */
    JwtAuthenticationResponse createRefreshResponse(Member member, String existingRefreshToken);
}
