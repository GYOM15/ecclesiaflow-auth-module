package com.ecclesiaflow.springsecurity.business.services.impl;

import com.ecclesiaflow.springsecurity.business.domain.MemberRegistration;
import com.ecclesiaflow.springsecurity.business.domain.SigninCredentials;
import com.ecclesiaflow.springsecurity.io.entities.Member;
import com.ecclesiaflow.springsecurity.io.repository.MemberRepository;
import com.ecclesiaflow.springsecurity.web.exception.InvalidCredentialsException;
import com.ecclesiaflow.springsecurity.business.services.AuthenticationService;
import com.ecclesiaflow.springsecurity.business.services.MemberRegistrationService;
import com.ecclesiaflow.springsecurity.web.exception.JwtProcessingException;
import com.ecclesiaflow.springsecurity.web.exception.MemberNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implémentation du service d'authentification EcclesiaFlow.
 * <p>
 * Cette classe orchestre l'authentification des membres via Spring Security
 * et la délégation des opérations d'inscription. Respecte le principe de 
 * responsabilité unique en se concentrant uniquement sur l'authentification métier,
 * sans gestion des tokens JWT (responsabilité de la couche web).
 * </p>
 * 
 * <p><strong>Rôle architectural :</strong> Service de domaine - Authentification pure</p>
 * 
 * <p><strong>Dépendances critiques :</strong></p>
 * <ul>
 *   <li>{@link AuthenticationManager} - Authentification Spring Security</li>
 *   <li>{@link MemberRegistrationService} - Inscription des nouveaux membres</li>
 * </ul>
 * 
 * <p><strong>Cas d'utilisation typiques :</strong></p>
 * <ul>
 *   <li>Inscription de nouveaux membres via délégation</li>
 *   <li>Authentification pure avec validation des identifiants</li>
 * </ul>
 * 
 * <p><strong>Garanties :</strong> Thread-safe (stateless), transactionnel selon les méthodes.</p>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final MemberRegistrationService memberRegistrationService;
    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public Member registerMember(MemberRegistration registration) {
        return memberRegistrationService.registerMember(registration);
    }

    @Override
    @Transactional(readOnly = true)
    public Member getAuthenticatedMember(SigninCredentials credentials) throws InvalidCredentialsException, JwtProcessingException {
        return authenticateMember(credentials);
    }

    private Member authenticateMember(SigninCredentials credentials) throws InvalidCredentialsException, JwtProcessingException {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(credentials.getEmail(), credentials.getPassword())
        );
        return (Member) authentication.getPrincipal();
    }

    @Override
    @Transactional(readOnly = true)
    public Member getMemberByEmail(String email) throws MemberNotFoundException {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new MemberNotFoundException("Membre introuvable pour l'email: " + email));
    }
}
