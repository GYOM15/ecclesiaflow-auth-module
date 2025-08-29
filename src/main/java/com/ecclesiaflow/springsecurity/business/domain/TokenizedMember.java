package com.ecclesiaflow.springsecurity.business.domain;

import com.ecclesiaflow.springsecurity.io.entities.Member;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Objet métier représentant le résultat d'une authentification réussie.
 * <p>
 * Cette classe encapsule les données retournées après une authentification réussie :
 * le membre authentifié et ses tokens JWT d'accès et de rafraîchissement.
 * Fait partie de la couche métier et reste indépendant des DTOs de la couche web.
 * </p>
 * 
 * <p><strong>Rôle architectural :</strong> Objet métier - Résultat d'authentification</p>
 * 
 * <p><strong>Responsabilités principales :</strong></p>
 * <ul>
 *   <li>Encapsulation des données d'authentification</li>
 *   <li>Transport des informations entre couches service et web</li>
 *   <li>Séparation claire entre domaine métier et DTOs</li>
 * </ul>
 * 
 * <p><strong>Cas d'utilisation typiques :</strong></p>
 * <ul>
 *   <li>Retour des services d'authentification</li>
 *   <li>Conversion vers DTOs web par les mappers</li>
 *   <li>Opérations de rafraîchissement de tokens</li>
 * </ul>
 * 
 * <p><strong>Garanties :</strong> Immutable après construction, thread-safe.</p>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenizedMember {
    private Member member;
    private String accessToken;
    private String refreshToken;
    
    /**
     * Constructeur pour les cas de refresh où on réutilise le refresh token existant
     */
    public TokenizedMember(Member member, String accessToken, String existingRefreshToken, boolean reuseRefreshToken) {
        this.member = member;
        this.accessToken = accessToken;
        this.refreshToken = existingRefreshToken;
    }
}
