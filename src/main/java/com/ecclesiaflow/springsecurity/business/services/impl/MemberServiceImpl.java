package com.ecclesiaflow.springsecurity.business.services.impl;

import com.ecclesiaflow.springsecurity.business.domain.member.MemberRepository;
import com.ecclesiaflow.springsecurity.business.services.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implémentation du service de gestion des membres pour l'intégration Spring Security.
 * <p>
 * Cette classe fournit les services nécessaires à l'intégration avec Spring Security,
 * notamment la création d'un {@link UserDetailsService} pour l'authentification.
 * </p>
 * 
 * <p><strong>Rôle architectural :</strong> Service de domaine - Intégration Spring Security</p>
 * 
 * <p><strong>Dépendances critiques :</strong></p>
 * <ul>
 *   <li>{@link MemberRepository} - Accès aux données des membres</li>
 *   <li>Spring Security - Framework d'authentification et autorisation</li>
 * </ul>
 * 
 * <p><strong>Cas d'utilisation typiques :</strong></p>
 * <ul>
 *   <li>Configuration du UserDetailsService pour Spring Security</li>
 *   <li>Chargement des détails utilisateur lors de l'authentification</li>
 *   <li>Intégration avec les filtres de sécurité Spring</li>
 * </ul>
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

    @Override
    @Transactional(readOnly = true)
    public UserDetailsService userDetailsService() {
        return member -> memberRepository.findByEmail(member)
                .orElseThrow(()-> new UsernameNotFoundException("Membre introuvable"));
    }
}
