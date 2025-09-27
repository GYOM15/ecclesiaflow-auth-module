package com.ecclesiaflow.springsecurity.business.services.adapters;

import com.ecclesiaflow.springsecurity.business.domain.member.Member;
import com.ecclesiaflow.springsecurity.business.domain.member.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Adaptateur Spring Security pour l'objet de domaine Member.
 * <p>
 * Cette classe fait le pont entre l'objet de domaine pur {@link Member} et l'interface
 * {@link UserDetails} requise par Spring Security. Elle respecte le principe d'inversion
 * de dépendance en gardant la couche domain indépendante des frameworks externes.
 * </p>
 *
 * <p><strong>Rôle architectural :</strong> Adaptateur - Couche Web/Security</p>
 *
 * <p><strong>Principe SOLID respecté :</strong></p>
 * <ul>
 *   <li><strong>SRP</strong> - Responsabilité unique : adapter Member pour Spring Security</li>
 *   <li><strong>DIP</strong> - La couche domain reste indépendante des frameworks</li>
 *   <li><strong>OCP</strong> - Extensible sans modifier l'objet domain</li>
 * </ul>
 *
 * <p><strong>Avantages de l'adaptateur :</strong></p>
 * <ul>
 *   <li>Séparation claire entre logique métier et framework</li>
 *   <li>Objet domain testable sans Spring Security</li>
 *   <li>Respect de l'architecture hexagonale</li>
 *   <li>Facilite les changements de framework de sécurité</li>
 * </ul>
 *
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
@RequiredArgsConstructor
public class MemberUserDetailsAdapter implements UserDetails {

    private final Member member;

    /**
     * {@inheritDoc}
     * <p>
     * Convertit les rôles métier en autorités Spring Security.
     * Chaque rôle est préfixé par "ROLE_" selon la convention Spring Security.
     * </p>
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + member.getRole().name()));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Retourne le mot de passe encodé du membre.
     * </p>
     */
    @Override
    public String getPassword() {
        return member.getPassword();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Utilise l'email comme nom d'utilisateur unique.
     * </p>
     */
    @Override
    public String getUsername() {
        return member.getEmail();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Pour EcclesiaFlow, les comptes ne sont jamais considérés comme expirés.
     * TODO: Implémenter la logique d'expiration quand le besoin métier se présentera.
     * </p>
     */
    @Override
    public boolean isAccountNonExpired() {
        // TODO: Retourner member.isAccountNonExpired() quand disponible
        return true;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Pour EcclesiaFlow, les comptes ne sont jamais verrouillés automatiquement.
     * TODO: Implémenter un système de verrouillage après X tentatives échouées.
     * </p>
     */
    @Override
    public boolean isAccountNonLocked() {
        // TODO: Retourner member.isAccountNonLocked() quand disponible
        return true;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Pour EcclesiaFlow, les mots de passe ne sont jamais considérés comme expirés.
     * TODO: Implémenter une politique d'expiration (ex: 90 jours) si nécessaire.
     * </p>
     */
    @Override
    public boolean isCredentialsNonExpired() {
        // TODO: Calculer depuis member.getPasswordChangedAt() + EXPIRATION_DURATION
        return true;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Pour EcclesiaFlow, tous les membres sont considérés comme actifs une fois créés.
     * TODO: Implémenter un système d'activation/désactivation d'administrateur.
     * </p>
     */
    @Override
    public boolean isEnabled() {
        return member.isEnabled();
    }

    /**
     * Accède à l'objet Member du domaine encapsulé.
     * <p>
     * Cette méthode permet d'accéder aux propriétés métier du membre
     * depuis les couches qui ont besoin de plus d'informations que
     * celles fournies par l'interface UserDetails.
     * </p>
     *
     * @return l'objet Member du domaine
     */
    public Member getMember() {
        return member;
    }

    /**
     * Utilitaire pour extraire le membre depuis un UserDetails.
     * <p>
     * Cette méthode statique permet de récupérer l'objet Member
     * à partir d'un UserDetails, si celui-ci est une instance
     * de MemberUserDetailsAdapter.
     * </p>
     *
     * @param userDetails l'objet UserDetails à convertir
     * @return le Member correspondant, ou null si la conversion n'est pas possible
     */
    public static Member extractMember(UserDetails userDetails) {
        if (userDetails instanceof MemberUserDetailsAdapter adapter) {
            return adapter.getMember();
        }
        return null;
    }

    /**
     * Retourne l'email du membre.
     *
     * @return l'adresse email du membre
     */
    public String getEmail() {
        return member.getEmail();
    }

    /**
     * Retourne le rôle du membre.
     *
     * @return le rôle du membre
     */
    public Role getRole() {
        return member.getRole();
    }
}
