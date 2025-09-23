package com.ecclesiaflow.springsecurity.business.services.impl;

import com.ecclesiaflow.springsecurity.business.encryption.PasswordEncoderUtil;
import com.ecclesiaflow.springsecurity.business.domain.member.MembersClient;
import com.ecclesiaflow.springsecurity.business.services.PasswordService;
import com.ecclesiaflow.springsecurity.io.entities.Member;
import com.ecclesiaflow.springsecurity.business.domain.member.Role;
import com.ecclesiaflow.springsecurity.io.repository.MemberRepository;
import com.ecclesiaflow.springsecurity.web.exception.InvalidRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PasswordServiceImpl implements PasswordService {

    private final MemberRepository memberRepository;
    private final PasswordEncoderUtil passwordEncoder;
    private final MembersClient membersClient;

    @Override
    @Transactional
    public void setInitialPassword(String email, String password) {
        if (membersClient.isEmailNotConfirmed(email)) {
            throw new InvalidRequestException("Le compte doit être confirmé avant de définir un mot de passe");
        }
        Member member =  new Member();
        member.setEmail(email);
        member.setPassword(passwordEncoder.encode(password));
        member.setRole(Role.MEMBER);
        member.setUpdatedAt(LocalDateTime.now());
        memberRepository.save(member);
    }

    @Override
    @Transactional
    public void changePassword(String email, String currentPassword, String newPassword) {
        // Vérifier la confirmation via le module Members
        if (membersClient.isEmailNotConfirmed(email)) {
            throw new InvalidRequestException("Le compte doit être confirmé pour changer le mot de passe");
        }

        // Récupérer le membre
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Membre non trouvé avec l'email: " + email));

        if (member.getPassword() == null || member.getPassword().isEmpty()) {
            throw new RuntimeException("Aucun mot de passe défini pour ce membre");
        }

        // Vérifier le mot de passe actuel
        if (!passwordEncoder.matches(currentPassword, member.getPassword())) {
            throw new RuntimeException("Mot de passe actuel incorrect");
        }

        // Encoder et stocker le nouveau mot de passe
        member.setPassword(passwordEncoder.encode(newPassword));
        member.setUpdatedAt(LocalDateTime.now());
        memberRepository.save(member);
    }
}
