package com.ecclesiaflow.springsecurity.services.impl;

import com.ecclesiaflow.springsecurity.domain.AuthenticationResult;
import com.ecclesiaflow.springsecurity.domain.MemberRegistration;
import com.ecclesiaflow.springsecurity.domain.SigninCredentials;
import com.ecclesiaflow.springsecurity.dto.JwtAuthenticationResponse;
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

import java.util.HashMap;

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
        String accessToken = jwtService.generateToken(member);
        String refreshToken = jwtService.generateRefreshToken(new HashMap<>(),member);
        return new AuthenticationResult(member, accessToken, refreshToken);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthenticationResult refreshToken(RefreshTokenRequest refreshTokenRequest) throws InvalidTokenException, JwtProcessingException {
            String memberEmail = jwtService.extractUserName(refreshTokenRequest.getToken());
            Member member = memberRepository.findByEmail(memberEmail)
                .orElseThrow(() -> new InvalidTokenException("Membre introuvable"));
            
            if (!jwtService.isTokenValid(refreshTokenRequest.getToken(), member)) {
                throw new InvalidTokenException("Token de rafraîchissement invalide");
            }

            // Génération du nouveau token d'accès
            String newAccessToken = jwtService.generateToken(member);
            return new AuthenticationResult(member, newAccessToken, refreshTokenRequest.getToken());

    }
}
