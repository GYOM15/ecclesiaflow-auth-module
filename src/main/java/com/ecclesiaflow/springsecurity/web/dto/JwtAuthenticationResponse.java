package com.ecclesiaflow.springsecurity.web.dto;

import lombok.Data;

/**
 * DTO représentant une réponse d'authentification contenant les tokens JWT.
 * <p>
 * Cette classe encapsule les tokens JWT renvoyés au client après une authentification
 * réussie. Elle fait partie de la couche web et constitue la réponse standard
 * pour les endpoints d'authentification et de rafraîchissement.
 * </p>
 * 
 * <p><strong>Contenu de la réponse :</strong></p>
 * <ul>
 *   <li>Token d'accès : pour l'authentification des requêtes API</li>
 *   <li>Token de rafraîchissement : pour renouveler le token d'accès</li>
 * </ul>
 * 
 * <p><strong>Cas d'utilisation typiques :</strong></p>
 * <ul>
 *   <li>Réponse après authentification réussie</li>
 *   <li>Réponse après rafraîchissement de token</li>
 *   <li>Sérialisation JSON pour les clients web/mobile</li>
 * </ul>
 * 
 * <p><strong>Garanties :</strong> Thread-safe, sérialisation JSON automatique.</p>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
@Data
public class JwtAuthenticationResponse {
    /**
     * Token d'accès JWT.
     * <p>
     * Utilisé pour authentifier les requêtes API. A une durée de vie limitée
     * et doit être inclus dans l'en-tête Authorization des requêtes.
     * </p>
     */
    private String token;

    /**
     * Token de rafraîchissement JWT.
     * <p>
     * Utilisé pour obtenir un nouveau token d'accès lorsque celui-ci expire.
     * A une durée de vie plus longue que le token d'accès.
     * </p>
     */
    private String refreshToken;
}
