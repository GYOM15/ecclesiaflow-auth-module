package com.ecclesiaflow.springsecurity.web.mappers;

import com.ecclesiaflow.springsecurity.business.domain.token.UserTokens;
import com.ecclesiaflow.springsecurity.web.model.JwtAuthenticationResponse;
import com.ecclesiaflow.springsecurity.web.model.PasswordManagementResponse;
import com.ecclesiaflow.springsecurity.web.model.TemporaryTokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Mapper pour créer les modèles OpenAPI générés à partir des objets métier.
 * <p>
 * Ce mapper crée directement les modèles OpenAPI générés à partir des entités métier,
 * permettant une séparation claire entre les DTOs existants et les modèles OpenAPI.
 * </p>
 * 
 * <p><strong>Architecture :</strong></p>
 * <pre>
 * Objets métier (UserTokens, etc.)
 *    ↓ transformation via
 * OpenApiModelMapper ← Cette classe
 *    ↓ produit
 * Modèles OpenAPI (JwtAuthenticationResponse, etc.)
 * </pre>
 * 
 * <p><strong>Responsabilités :</strong></p>
 * <ul>
 *   <li>Conversion UserTokens → JwtAuthenticationResponse</li>
 *   <li>Conversion UserTokens → PasswordManagementResponse (avec message)</li>
 *   <li>Création TemporaryTokenResponse à partir d'un token string</li>
 * </ul>
 * 
 * @author EcclesiaFlow Team
 * @since 2.0.0
 */
@Component
public class OpenApiModelMapper {

    @Value("${jwt.temporary-token.expiration:900}")
    private int temporaryTokenExpiration;

    /**
     * Crée un JwtAuthenticationResponse OpenAPI à partir d'un UserTokens.
     * 
     * @param userTokens Tokens utilisateur métier
     * @return Modèle OpenAPI JwtAuthenticationResponse
     */
    public JwtAuthenticationResponse createJwtAuthenticationResponse(UserTokens userTokens) {
        JwtAuthenticationResponse response = new JwtAuthenticationResponse();
        
        if (userTokens != null) {
            response.setToken(userTokens.accessToken());
            response.setRefreshToken(userTokens.refreshToken());
        }
        
        return response;
    }

    /**
     * Crée un PasswordManagementResponse OpenAPI à partir d'un UserTokens et d'un message.
     * <p>
     * Utilisé après la définition du mot de passe initial pour retourner
     * les tokens d'authentification avec un message de succès.
     * </p>
     * 
     * @param message Message de confirmation
     * @param userTokens Tokens utilisateur métier
     * @param expiresIn Durée de validité du token d'accès en secondes
     * @return Modèle OpenAPI PasswordManagementResponse
     */
    public PasswordManagementResponse createPasswordManagementResponse(
            String message, UserTokens userTokens, Long expiresIn) {
        PasswordManagementResponse response = new PasswordManagementResponse();
        
        response.setMessage(message);
        
        if (userTokens != null) {
            response.setAccessToken(userTokens.accessToken());
            response.setRefreshToken(userTokens.refreshToken());
        }
        
        response.setExpiresIn(expiresIn);
        
        return response;
    }

    /**
     * Crée un TemporaryTokenResponse OpenAPI à partir d'un token temporaire.
     * 
     * @param temporaryToken Token temporaire JWT
     * @return Modèle OpenAPI TemporaryTokenResponse
     */
    public TemporaryTokenResponse createTemporaryTokenResponse(String temporaryToken) {
        TemporaryTokenResponse response = new TemporaryTokenResponse();
        
        response.setTemporaryToken(temporaryToken);
        response.setExpiresIn(temporaryTokenExpiration);
        response.setMessage("Token temporaire généré avec succès");
        
        return response;
    }
}
