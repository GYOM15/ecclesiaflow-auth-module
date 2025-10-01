package com.ecclesiaflow.springsecurity.business.services.impl;

import com.ecclesiaflow.springsecurity.business.domain.password.SigninCredentials;
import com.ecclesiaflow.springsecurity.business.domain.member.Member;
import com.ecclesiaflow.springsecurity.business.domain.member.MemberRepository;
import com.ecclesiaflow.springsecurity.web.exception.InvalidCredentialsException;
import com.ecclesiaflow.springsecurity.business.services.AuthenticationService;
import com.ecclesiaflow.springsecurity.web.exception.JwtProcessingException;
import com.ecclesiaflow.springsecurity.business.exceptions.MemberNotFoundException;
import com.ecclesiaflow.springsecurity.web.security.Jwt;
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
    private final MemberRepository memberRepository;
    private final Jwt jwt;

    @Override
    @Transactional(readOnly = true)
    public Member getAuthenticatedMember(SigninCredentials credentials) throws InvalidCredentialsException, JwtProcessingException {
        return authenticateMember(credentials);
    }

    private Member authenticateMember(SigninCredentials credentials) throws InvalidCredentialsException, JwtProcessingException {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(credentials.email(), credentials.password())
        );
        return (Member) authentication.getPrincipal();
    }

    @Override
    @Transactional(readOnly = true)
    public Member getMemberByEmail(String email) throws MemberNotFoundException {
        return memberRepository.getByEmail(email)
                .orElseThrow(() -> new MemberNotFoundException("Membre introuvable pour l'email: " + email));
    }

    @Override
    public String getEmailFromValidatedTempToken(String temporaryToken) throws InvalidCredentialsException {
        if (temporaryToken == null || temporaryToken.trim().isEmpty()) {
            throw new IllegalArgumentException("Le token temporaire ne peut pas être null ou vide");
        }
        String email = jwt.extractEmailFromTemporaryToken(temporaryToken);
        if (!jwt.validateTemporaryToken(temporaryToken, email)) {
            throw new InvalidCredentialsException("Token temporaire invalide ou expiré");
        }
        return email;
    }
}
