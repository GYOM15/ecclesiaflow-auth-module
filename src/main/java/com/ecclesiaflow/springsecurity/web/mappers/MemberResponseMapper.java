package com.ecclesiaflow.springsecurity.web.mappers;

import com.ecclesiaflow.springsecurity.web.dto.MemberResponse;
import com.ecclesiaflow.springsecurity.io.entities.Member;

/**
 * Mapper statique pour la conversion des entités Member vers les DTOs de réponse web.
 * <p>
 * Cette classe fournit des méthodes utilitaires pour transformer les entités Member
 * en objets de réponse web {@link MemberResponse} avec des informations contextuelles
 * comme les messages et tokens. Respecte le pattern Static Utility Class.
 * </p>
 * 
 * <p><strong>Rôle architectural :</strong> Mapper - Conversion entité vers DTO de réponse</p>
 * 
 * <p><strong>Responsabilités principales :</strong></p>
 * <ul>
 *   <li>Conversion des entités Member vers DTOs de réponse</li>
 *   <li>Enrichissement avec informations contextuelles (messages, tokens)</li>
 *   <li>Séparation claire entre couche persistance et couche web</li>
 * </ul>
 * 
 * <p><strong>Cas d'utilisation typiques :</strong></p>
 * <ul>
 *   <li>Transformation des réponses d'authentification</li>
 *   <li>Enrichissement avec tokens JWT après connexion</li>
 *   <li>Ajout de messages contextuels pour l'interface utilisateur</li>
 * </ul>
 * 
 * <p><strong>Garanties :</strong> Thread-safe, stateless, opérations pures.</p>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
public class MemberResponseMapper {
    
    /**
     * Convertit une entité Member en DTO de réponse avec message et token.
     * <p>
     * Cette méthode effectue une transformation complète de l'entité Member
     * vers un DTO de réponse enrichi avec un message contextuel et un token JWT.
     * </p>
     * 
     * @param member l'entité Member à convertir, non null
     * @param message le message contextuel à inclure, non null
     * @param token le token JWT à inclure, peut être null
     * @return un {@link MemberResponse} contenant toutes les informations du membre
     * @throws NullPointerException si member ou message est null
     */
    public static MemberResponse fromMember(Member member, String message, String token) {
        return MemberResponse.builder()
                .message(message)
                .password(member.getPassword())
                .email(member.getEmail())
                .role(member.getRole().name())
                .accountNonLocked(member.isAccountNonLocked())
                .enabled(member.isEnabled())
                .authorities(member.getAuthorities())
                .username(member.getUsername())
                .accountNonExpired(member.isAccountNonExpired())
                .credentialsNonExpired(member.isCredentialsNonExpired())
                .token(token)
                .build();
    }
    
    /**
     * Convertit une entité Member en DTO de réponse avec message seulement.
     * <p>
     * Méthode de convenance qui appelle la méthode principale avec token null.
     * Utilisée quand aucun token n'est nécessaire dans la réponse.
     * </p>
     * 
     * @param member l'entité Member à convertir, non null
     * @param message le message contextuel à inclure, non null
     * @return un {@link MemberResponse} sans token
     * @throws NullPointerException si member ou message est null
     */
    public static MemberResponse fromMember(Member member, String message) {
        return fromMember(member, message, null);
    }
}
