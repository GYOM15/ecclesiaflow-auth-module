package com.ecclesiaflow.springsecurity.business.services.impl;

import com.ecclesiaflow.springsecurity.business.encryption.PasswordEncoderUtil;
import com.ecclesiaflow.springsecurity.business.domain.member.MembersClient;
import com.ecclesiaflow.springsecurity.business.events.PasswordChangedEvent;
import com.ecclesiaflow.springsecurity.business.events.PasswordResetEvent;
import com.ecclesiaflow.springsecurity.business.events.PasswordResetRequestedEvent;
import com.ecclesiaflow.springsecurity.business.events.PasswordSetEvent;
import com.ecclesiaflow.springsecurity.business.services.PasswordService;
import com.ecclesiaflow.springsecurity.business.domain.member.Member;
import com.ecclesiaflow.springsecurity.business.domain.member.Role;
import com.ecclesiaflow.springsecurity.business.domain.member.MemberRepository;
import com.ecclesiaflow.springsecurity.web.exception.InvalidRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordServiceImpl implements PasswordService {

    private final MemberRepository memberRepository;
    private final PasswordEncoderUtil passwordEncoder;
    private final MembersClient membersClient;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public Member setInitialPassword(String email, String password, UUID memberId) {
        if (membersClient.isEmailNotConfirmed(email)) {
            throw new InvalidRequestException("Impossible de traiter votre demande.");
        }
        LocalDateTime now = LocalDateTime.now();
        Member member = memberRepository.getByEmail(email)
                .map(existing -> {
                    if (existing.isEnabled()) {
                        throw new InvalidRequestException("Impossible de traiter votre demande.");
                    }
                    return existing.toBuilder()
                            .memberId(memberId)
                            .password(passwordEncoder.encode(password))
                            .passwordUpdatedAt(now)
                            .enabled(true)
                            .build();
                })
                .orElse(Member.builder()
                        .memberId(memberId)
                        .email(email.toLowerCase().trim())
                        .password(passwordEncoder.encode(password))
                        .passwordUpdatedAt(now)
                        .role(Role.MEMBER)
                        .enabled(true)
                        .build());
        Member savedMember = memberRepository.save(member);
        
        // Publier l'événement pour déclencher l'envoi de l'email de bienvenue
        eventPublisher.publishEvent(new PasswordSetEvent(this, savedMember.getEmail()));
        
        return savedMember;
    }

    @Override
    @Transactional
    public Member changePassword(String email, String currentPassword, String newPassword) {
        Member member = getMemberByEmail(email);
        
        if (currentPassword != null && currentPassword.equals(newPassword)) {
            throw new InvalidRequestException("Le nouveau mot de passe n'est pas valide.");
        }
        
        validateCurrentPassword(member, currentPassword);
        Member updatedMember = updatePassword(member, newPassword);
        
        // Publier l'événement pour déclencher l'envoi de la notification
        eventPublisher.publishEvent(new PasswordChangedEvent(this, updatedMember.getEmail()));
        
        return updatedMember;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Member> requestPasswordReset(String email) {
        Optional<Member> memberOpt = memberRepository.getByEmail(email);
        
        // Publier l'événement métier si le membre existe
        memberOpt.ifPresent(member -> 
            eventPublisher.publishEvent(
                new PasswordResetRequestedEvent(this, member.getEmail(), member.getMemberId())
            )
        );
        
        return memberOpt;
    }

    @Override
    @Transactional
    public Member resetPasswordWithToken(String email, String newPassword) {
        Member member = getMemberByEmail(email);
        Member updatedMember = updatePassword(member, newPassword);
        
        // Publier l'événement pour déclencher l'envoi de la notification
        eventPublisher.publishEvent(new PasswordResetEvent(this, updatedMember.getEmail()));
        
        return updatedMember;
    }
    
    // ==================== PRIVATE METHODS (SRP) ====================

    private Member getMemberByEmail(String email) {
        return memberRepository.getByEmail(email)
                .orElseThrow(() -> new RuntimeException("Membre non trouvé avec l'email: " + email));
    }

    private void validateCurrentPassword(Member member, String currentPassword) {
        if (member.getPassword() == null || member.getPassword().isEmpty()) {
            throw new InvalidRequestException("Authentification échouée.");
        }

        if (!passwordEncoder.matches(currentPassword, member.getPassword())) {
            throw new InvalidRequestException("Authentification échouée.");
        }
    }

    private Member updatePassword(Member member, String newPassword) {
        LocalDateTime now = LocalDateTime.now();
        Member updatedMember = member.toBuilder()
                .password(passwordEncoder.encode(newPassword))
                .passwordUpdatedAt(now)
                .updatedAt(now)
                .build();
        return memberRepository.save(updatedMember);
    }
}
