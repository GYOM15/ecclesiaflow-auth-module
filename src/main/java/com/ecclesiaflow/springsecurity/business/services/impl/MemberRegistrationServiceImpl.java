package com.ecclesiaflow.springsecurity.business.services.impl;

import com.ecclesiaflow.springsecurity.business.domain.MemberRegistration;
import com.ecclesiaflow.springsecurity.io.entities.Member;
import com.ecclesiaflow.springsecurity.io.entities.Role;
import com.ecclesiaflow.springsecurity.io.repository.MemberRepository;
import com.ecclesiaflow.springsecurity.business.services.MemberRegistrationService;
import com.ecclesiaflow.springsecurity.business.encryption.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implémentation du service d'enregistrement de membres
 * Responsabilité unique : gérer l'enregistrement de nouveaux membres
 */
@Service
@RequiredArgsConstructor
public class MemberRegistrationServiceImpl implements MemberRegistrationService {
    
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    @Transactional
    public Member registerMember(MemberRegistration registration) {
        if (isEmailAlreadyUsed(registration.getEmail())) {
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
        member.setEmail(registration.getEmail());
        member.setPassword(passwordEncoder.encode(registration.getPassword()));
        member.setFirstName(registration.getFirstName());
        member.setLastName(registration.getLastName());
        member.setRole(Role.MEMBER);
        return member;
    }
}
