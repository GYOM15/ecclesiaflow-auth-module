package com.ecclesiaflow.springsecurity.business.mappers;

import com.ecclesiaflow.springsecurity.business.domain.Tokens;
import com.ecclesiaflow.springsecurity.business.domain.RefreshTokenCredentials;
import com.ecclesiaflow.springsecurity.web.dto.JwtAuthenticationResponse;
import com.ecclesiaflow.springsecurity.web.dto.RefreshTokenRequest;

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
 *   <li>Conversion des Tokens vers JwtAuthenticationResponse</li>
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
     * @param tokens le résultat d'authentification métier, non null
     * @return un {@link JwtAuthenticationResponse} contenant les tokens JWT
     * @throws NullPointerException si tokens est null
     * 
     * @implNote Transformation pure sans validation - la validation est assurée par l'architecture.
     */
    public static JwtAuthenticationResponse toDto(Tokens tokens) {
        JwtAuthenticationResponse dto = new JwtAuthenticationResponse();
        dto.setToken(tokens.getAccessToken());
        dto.setRefreshToken(tokens.getRefreshToken());
        return dto;
    }

    /**
     * Convertit une requête de rafraîchissement de token API en objet métier.
     * <p>
     * Cette méthode effectue une transformation pure du DTO API vers l'objet métier
     * correspondant, assurant le découplage entre la couche web et la couche métier.
     * </p>
     * 
     * @param request la requête de rafraîchissement de token API, non null
     * @return un {@link RefreshTokenCredentials} contenant le refresh token
     * @throws NullPointerException si request est null
     * 
     * @implNote Transformation pure sans validation - la validation est assurée par l'architecture.
     */
    public static RefreshTokenCredentials fromRefreshTokenRequest(RefreshTokenRequest request) {
        return new RefreshTokenCredentials(request.getToken());
    }
}
