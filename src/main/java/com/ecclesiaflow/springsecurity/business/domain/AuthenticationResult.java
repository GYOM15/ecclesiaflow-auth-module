package com.ecclesiaflow.springsecurity.business.domain;

import com.ecclesiaflow.springsecurity.io.entities.Member;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Objet métier représentant le résultat d'une authentification
 * Utilisé dans la couche service, indépendant des DTOs web
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationResult {
    private Member member;
    private String accessToken;
    private String refreshToken;
    
    /**
     * Constructeur pour les cas de refresh où on réutilise le refresh token existant
     */
    public AuthenticationResult(Member member, String accessToken, String existingRefreshToken, boolean reuseRefreshToken) {
        this.member = member;
        this.accessToken = accessToken;
        this.refreshToken = existingRefreshToken;
    }
}
