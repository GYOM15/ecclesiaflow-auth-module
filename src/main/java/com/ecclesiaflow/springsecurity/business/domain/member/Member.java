package com.ecclesiaflow.springsecurity.business.domain.member;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Objet de domaine représentant un membre EcclesiaFlow.
 * <p>
 * Cette classe représente un membre dans la couche business, complètement indépendante
 * des frameworks externes (persistance, sécurité). C'est un objet de domaine pur
 * qui ne contient que la logique métier essentielle.
 * </p>
 * 
 * <p><strong>Rôle architectural :</strong> Objet de domaine pur - Entité métier</p>
 * 
 * <p><strong>Principe SOLID respecté :</strong></p>
 * <ul>
 *   <li><strong>SRP</strong> - Responsabilité unique : représenter un membre</li>
 *   <li><strong>DIP</strong> - Complètement indépendant des frameworks externes</li>
 *   <li><strong>OCP</strong> - Extensible sans modification</li>
 * </ul>
 * 
 * <p><strong>Avantages de l'objet domain pur :</strong></p>
 * <ul>
 *   <li>Logique métier centralisée sans dépendances externes</li>
 *   <li>Testable unitairement sans aucun contexte framework</li>
 *   <li>Indépendant de Spring Security, JPA, etc.</li>
 *   <li>Respecte parfaitement l'architecture hexagonale</li>
 * </ul>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
@Getter
@Builder(toBuilder = true)
public class Member {
    private final UUID id;
    private final UUID memberId;
    private final String email;
    private final String password;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    @Builder.Default
    private final Role role = Role.MEMBER;
    @Builder.Default
    private final boolean enabled = false;
}
