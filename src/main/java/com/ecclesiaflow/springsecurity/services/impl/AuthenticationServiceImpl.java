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
    public JwtAuthenticationResponse getAuthenticatedMember(SigninCredentials credentials) {
        // Authentification via Spring Security
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(credentials.getEmail(), credentials.getPassword())
        );

        // Récupération du membre authentifié
        Member member = memberRepository.findByEmail(credentials.getEmail())
            .orElseThrow(() -> new IllegalArgumentException("Email ou mot de passe incorrect"));

        // Génération de la réponse JWT
        return jwtResponseService.createAuthenticationResponse(member);
    }

    @Override
    @Transactional(readOnly = true)
    public JwtAuthenticationResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        String memberEmail = jwtService.extractUserName(refreshTokenRequest.getToken());
        Member member = memberRepository.findByEmail(memberEmail)
            .orElseThrow(() -> new IllegalArgumentException("Membre introuvable"));
        
        if (!jwtService.isTokenValid(refreshTokenRequest.getToken(), member)) {
            throw new IllegalArgumentException("Token de rafraîchissement invalide");
        }

        return jwtResponseService.createRefreshResponse(member, refreshTokenRequest.getToken());
    }
}
