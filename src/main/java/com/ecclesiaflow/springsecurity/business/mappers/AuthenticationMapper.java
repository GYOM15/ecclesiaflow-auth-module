package com.ecclesiaflow.springsecurity.business.mappers;

import com.ecclesiaflow.springsecurity.business.domain.AuthenticationResult;
import com.ecclesiaflow.springsecurity.web.dto.JwtAuthenticationResponse;

/**
 * Mapper statique pour la conversion entre objets métier d'authentification et DTOs web.
 * <p>
 * Cette classe fournit des méthodes utilitaires pour transformer les résultats d'authentification
 * du domaine métier en DTOs de réponse web. Assure l'isolation complète entre la couche
 * service et la couche web selon les principes d'architecture hexagonale.
 * </p>
 * 
 * <p><strong>Rôle architectural :</strong> Mapper - Conversion domaine métier vers DTO web</p>
 * 
 * <p><strong>Responsabilités principales :</strong></p>
 * <ul>
 *   <li>Conversion des AuthenticationResult vers JwtAuthenticationResponse</li>
 *   <li>Isolation complète entre couches service et web</li>
 *   <li>Transformation pure sans logique métier</li>
 * </ul>
 * 
 * <p><strong>Cas d'utilisation typiques :</strong></p>
 * <ul>
 *   <li>Transformation des réponses d'authentification pour l'API REST</li>
 *   <li>Conversion des résultats de rafraîchissement de tokens</li>
 *   <li>Orchestration par les contrôleurs d'authentification</li>
 * </ul>
 * 
 * <p><strong>Garanties :</strong> Thread-safe, stateless, opérations pures, classe finale.</p>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
public final class AuthenticationMapper {
    /**
     * Convertit un résultat d'authentification métier en DTO de réponse web.
     * <p>
     * Cette méthode effectue une transformation pure des données d'authentification
     * du domaine métier vers le format attendu par la couche web. Extrait uniquement
     * les tokens JWT nécessaires à la réponse API.
     * </p>
     * 
     * @param authResult le résultat d'authentification métier, non null
     * @return un {@link JwtAuthenticationResponse} contenant les tokens JWT
     * @throws NullPointerException si authResult est null
     * 
     * @implNote Transformation pure sans validation - la validation est assurée par l'architecture.
     */
    public static JwtAuthenticationResponse toDto(AuthenticationResult authResult) {
        JwtAuthenticationResponse dto = new JwtAuthenticationResponse();
        dto.setToken(authResult.getAccessToken());
        dto.setRefreshToken(authResult.getRefreshToken());
        return dto;
    }
}
