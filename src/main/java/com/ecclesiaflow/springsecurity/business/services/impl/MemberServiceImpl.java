package com.ecclesiaflow.springsecurity.business.services.impl;

import com.ecclesiaflow.springsecurity.business.domain.member.Member;
import com.ecclesiaflow.springsecurity.business.domain.member.MemberRepository;
import com.ecclesiaflow.springsecurity.business.services.MemberService;
import com.ecclesiaflow.springsecurity.business.services.adapters.MemberUserDetailsAdapter;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implémentation du service de gestion des membres pour l'intégration Spring Security.
 *
 * <p>Cette classe fournit les services nécessaires à l'intégration avec Spring Security
 * en utilisant l'objet de domaine {@link Member} et l'adaptateur {@link MemberUserDetailsAdapter}.
 * Elle respecte l'inversion de dépendance.</p>
 *
 * <p><strong>Rôle :</strong> Service de domaine - Intégration Spring Security</p>
 *
 * <p><strong>Dépendances critiques :</strong></p>
 * <ul>
 *   <li>{@link MemberRepository} - Accès aux données des membres</li>
 *   <li>Spring Security - Framework d'authentification et d'autorisation</li>
 * </ul>
 *
 * <p><strong>Cas d'utilisation :</strong></p>
 * <ul>
 *   <li>Configuration de UserDetailsService</li>
 *   <li>Chargement des détails utilisateur à l'authentification</li>
 *   <li>Intégration avec les filtres de sécurité</li>
 * </ul>
 *
 *  * <p><strong>Dépendances :</strong></p>
 *  *   <li>{@link MemberRepository} - Interface domain pour l'accès aux données</li>
 *  *   <li>{@link MemberUserDetailsAdapter} - Adaptateur Spring Security</li>
 * 
 * <p><strong>Garanties :</strong> Thread-safe (stateless), transactionnel en lecture seule.</p>
 *
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetailsService userDetailsService() {
        return username -> {
            Member member = memberRepository.findByEmail(username)
                    .orElseThrow(() -> new UsernameNotFoundException(
                            "Membre introuvable avec l'email : " + username));
            return new MemberUserDetailsAdapter(member);
        };
    }
}
