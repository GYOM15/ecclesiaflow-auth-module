package com.ecclesiaflow.springsecurity.business.domain.token;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Objet métier représentant les données nécessaires pour rafraîchir un token JWT.
 * <p>
 * Cette classe fait partie de la couche métier et encapsule les informations
 * nécessaires pour rafraîchir un token JWT. Elle assure le découplage entre
 * la couche API (DTOs) et la couche métier.
 * </p>
 * 
 * <p><strong>Responsabilité :</strong></p>
 * <ul>
 *   <li>Encapsuler les données de rafraîchissement pour les opérations métier</li>
 *   <li>Maintenir l'indépendance de la couche métier vis-à-vis des DTOs API</li>
 * </ul>
 * 
 * <p><strong>Cas d'utilisation typiques :</strong></p>
 * <ul>
 *   <li>Validation et rafraîchissement de tokens JWT</li>
 *   <li>Génération de nouveaux access tokens</li>
 * </ul>
 * 
 * <p><strong>Garanties :</strong> Immutable après construction, validation métier.</p>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenCredentials {
    
    /**
     * Le refresh token JWT à valider et utiliser pour générer un nouveau access token.
     */
    private String Token;
}
