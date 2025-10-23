package com.ecclesiaflow.springsecurity.business.services.impl;

import com.ecclesiaflow.springsecurity.business.encryption.PasswordEncoderUtil;
import com.ecclesiaflow.springsecurity.business.domain.member.MembersClient;
import com.ecclesiaflow.springsecurity.business.services.PasswordService;
import com.ecclesiaflow.springsecurity.business.domain.member.Member;
import com.ecclesiaflow.springsecurity.business.domain.member.Role;
import com.ecclesiaflow.springsecurity.business.domain.member.MemberRepository;
import com.ecclesiaflow.springsecurity.web.exception.InvalidRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordServiceImpl implements PasswordService {

    private final MemberRepository memberRepository;
    private final PasswordEncoderUtil passwordEncoder;
    private final MembersClient membersClient;

    @Override
    @Transactional
    public void setInitialPassword(String email, String password, UUID memberId) {
        if (membersClient.isEmailNotConfirmed(email)) {
            throw new InvalidRequestException("Le compte doit être confirmé avant de définir un mot de passe");
        }
        Member member = memberRepository.getByEmail(email)
                .map(existing -> {
                    if (existing.isEnabled()) {
                        throw new InvalidRequestException("Le mot de passe a déjà été défini pour ce compte");
                    }
                    return existing.toBuilder()
                            .memberId(memberId)
                            .password(passwordEncoder.encode(password))
                            .enabled(true)
                            .build();
                })
                .orElse(Member.builder()
                        .memberId(memberId)
                        .email(email.toLowerCase().trim())
                        .password(passwordEncoder.encode(password))
                        .role(Role.MEMBER)
                        .enabled(true)
                        .build());
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
        Member member = memberRepository.getByEmail(email)
                .orElseThrow(() -> new RuntimeException("Membre non trouvé avec l'email: " + email));

        if (member.getPassword() == null || member.getPassword().isEmpty()) {
            throw new RuntimeException("Aucun mot de passe défini pour ce membre");
        }

        // Vérifier le mot de passe actuel
        if (!passwordEncoder.matches(currentPassword, member.getPassword())) {
            throw new RuntimeException("Mot de passe actuel incorrect");
        }

        Member updatedMember = member.toBuilder().
                password(passwordEncoder.encode(newPassword)).
                updatedAt(LocalDateTime.now()).
                build();
        memberRepository.save(updatedMember);
    }
}
