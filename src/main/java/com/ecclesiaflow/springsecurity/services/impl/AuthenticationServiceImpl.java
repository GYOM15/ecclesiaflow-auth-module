package com.ecclesiaflow.springsecurity.services.impl;

import com.ecclesiaflow.springsecurity.domain.AuthenticationResult;
import com.ecclesiaflow.springsecurity.domain.MemberRegistration;
import com.ecclesiaflow.springsecurity.domain.SigninCredentials;
import com.ecclesiaflow.springsecurity.dto.RefreshTokenRequest;
import com.ecclesiaflow.springsecurity.entities.Member;
import com.ecclesiaflow.springsecurity.exception.InvalidCredentialsException;
import com.ecclesiaflow.springsecurity.exception.InvalidTokenException;
import com.ecclesiaflow.springsecurity.exception.JwtProcessingException;
import com.ecclesiaflow.springsecurity.repository.MemberRepository;
import com.ecclesiaflow.springsecurity.services.AuthenticationService;
import com.ecclesiaflow.springsecurity.services.JWTService;
import com.ecclesiaflow.springsecurity.services.MemberRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service d'authentification refactorisé pour respecter le SRP
 * Délègue les responsabilités spécifiques aux services dédiés
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

    /**
     * Authentifie un membre avec ses identifiants
     * @param credentials les identifiants de connexion
     * @return le membre authentifié
     * @throws InvalidCredentialsException si les identifiants sont incorrects
     */
    private Member authenticateMemberWithCredentials(SigninCredentials credentials) throws InvalidCredentialsException {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(credentials.getEmail(), credentials.getPassword())
        );
        return (Member) authentication.getPrincipal();
    }

    /**
     * Génère les tokens JWT pour un membre authentifié
     * @param member le membre authentifié
     * @return le résultat d'authentification avec les tokens
     * @throws JwtProcessingException si la génération des tokens échoue
     */
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
