package com.ecclesiaflow.springsecurity.business.services.impl;

import com.ecclesiaflow.springsecurity.business.domain.MemberRegistration;
import com.ecclesiaflow.springsecurity.io.entities.Member;
import com.ecclesiaflow.springsecurity.io.entities.Role;
import com.ecclesiaflow.springsecurity.io.repository.MemberRepository;
import com.ecclesiaflow.springsecurity.business.services.MemberRegistrationService;
import com.ecclesiaflow.springsecurity.business.encryption.PasswordEncoderUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implémentation du service d'enregistrement de nouveaux membres EcclesiaFlow.
 * <p>
 * Cette classe gère exclusivement l'inscription de nouveaux membres dans le système :
 * validation de l'unicité de l'email, encodage sécurisé du mot de passe et persistance
 * en base de données. Respecte le principe de responsabilité unique.
 * </p>
 * 
 * <p><strong>Rôle architectural :</strong> Service de domaine - Gestion des inscriptions</p>
 * 
 * <p><strong>Dépendances critiques :</strong></p>
 * <ul>
 *   <li>{@link MemberRepository} - Persistance et vérification d'unicité</li>
 *   <li>{@link PasswordEncoderUtil} - Encodage sécurisé des mots de passe</li>
 * </ul>
 * 
 * <p><strong>Cas d'utilisation typiques :</strong></p>
 * <ul>
 *   <li>Inscription de nouveaux membres via formulaire web</li>
 *   <li>Validation de l'unicité des emails avant inscription</li>
 *   <li>Création automatique de comptes avec rôle USER par défaut</li>
 * </ul>
 * 
 * <p><strong>Garanties :</strong> Thread-safe (stateless), transactionnel, validation d'unicité.</p>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class MemberRegistrationServiceImpl implements MemberRegistrationService {
    
    private final MemberRepository memberRepository;
    private final PasswordEncoderUtil passwordEncoderUtil;
    
    @Override
    @Transactional
    public Member registerMember(MemberRegistration registration) {
        if (isEmailAlreadyUsed(registration.email())) {
            throw new IllegalArgumentException("Un compte avec cet email existe déjà.");
        }

        Member member = createMemberFromRegistration(registration);
        return memberRepository.save(member);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEmailAlreadyUsed(String email) {
        return memberRepository.findByEmail(email).isPresent();
    }

    /**
     * Crée une entité Member à partir des données d'enregistrement
     */
    private Member createMemberFromRegistration(MemberRegistration registration) {
        Member member = new Member();
        member.setEmail(registration.email());
        member.setPassword(passwordEncoderUtil.encode(registration.password()));
        member.setRole(Role.MEMBER);
        return member;
    }
}
