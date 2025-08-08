package com.ecclesiaflow.springsecurity.business.services.impl;

import com.ecclesiaflow.springsecurity.business.domain.AuthenticationResult;
import com.ecclesiaflow.springsecurity.business.domain.MemberRegistration;
import com.ecclesiaflow.springsecurity.business.domain.SigninCredentials;
import com.ecclesiaflow.springsecurity.web.dto.RefreshTokenRequest;
import com.ecclesiaflow.springsecurity.io.entities.Member;
import com.ecclesiaflow.springsecurity.web.exception.InvalidCredentialsException;
import com.ecclesiaflow.springsecurity.web.exception.InvalidTokenException;
import com.ecclesiaflow.springsecurity.web.exception.JwtProcessingException;
import com.ecclesiaflow.springsecurity.io.repository.MemberRepository;
import com.ecclesiaflow.springsecurity.business.services.AuthenticationService;
import com.ecclesiaflow.springsecurity.business.services.JWTService;
import com.ecclesiaflow.springsecurity.business.services.MemberRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implémentation du service d'authentification EcclesiaFlow.
 * <p>
 * Cette classe orchestre l'authentification des membres via Spring Security,
 * la génération de tokens JWT et les opérations de rafraîchissement de tokens.
 * Respecte le principe de responsabilité unique en déléguant les tâches spécialisées
 * aux services dédiés.
 * </p>
 * 
 * <p><strong>Rôle architectural :</strong> Service de domaine - Orchestration d'authentification</p>
 * 
 * <p><strong>Dépendances critiques :</strong></p>
 * <ul>
 *   <li>{@link AuthenticationManager} - Authentification Spring Security</li>
 *   <li>{@link JWTService} - Génération et validation des tokens JWT</li>
 *   <li>{@link MemberRegistrationService} - Inscription des nouveaux membres</li>
 *   <li>{@link MemberRepository} - Accès aux données des membres</li>
 * </ul>
 * 
 * <p><strong>Cas d'utilisation typiques :</strong></p>
 * <ul>
 *   <li>Inscription de nouveaux membres via délégation</li>
 *   <li>Authentification avec génération de tokens JWT</li>
 *   <li>Rafraîchissement des tokens d'accès expirés</li>
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
    private final MemberRepository memberRepository;
    private final AuthenticationManager authenticationManager;
    private final JWTService jwtService;
    private final MemberRegistrationService memberRegistrationService;

    @Override
    @Transactional
    public Member registerMember(MemberRegistration registration) {
        return memberRegistrationService.registerMember(registration);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthenticationResult getAuthenticatedMember(SigninCredentials credentials) throws InvalidCredentialsException, JwtProcessingException {
        Member authenticatedMember = authenticateMemberWithCredentials(credentials);
        return createAuthenticationResultWithTokens(authenticatedMember);
    }

    private Member authenticateMemberWithCredentials(SigninCredentials credentials) throws InvalidCredentialsException {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(credentials.getEmail(), credentials.getPassword())
        );
        return (Member) authentication.getPrincipal();
    }

    private AuthenticationResult createAuthenticationResultWithTokens(Member member) throws JwtProcessingException {
        String accessToken = jwtService.generateAccessToken(member);
        String refreshToken = jwtService.generateRefreshToken(member);
        return new AuthenticationResult(member, accessToken, refreshToken);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthenticationResult refreshToken(RefreshTokenRequest refreshTokenRequest) 
            throws InvalidTokenException, JwtProcessingException {

        String refreshToken = refreshTokenRequest.getToken();
        if (!jwtService.isRefreshTokenValid(refreshToken)) throw new InvalidTokenException("Le token de rafraîchissement est invalide ou n'est pas du bon type.");

        String username = jwtService.extractUsername(refreshToken);

        // On récupère l'utilisateur sans utiliser d'identifiants externes injectables
        Member member = memberRepository.findByEmail(username).orElseThrow(() -> new InvalidTokenException("Aucun membre correspondant au token."));

        // Génération du nouveau token d'accès
        String newAccessToken = jwtService.generateAccessToken(member);

        return new AuthenticationResult(member, newAccessToken, refreshToken);
    }
}
