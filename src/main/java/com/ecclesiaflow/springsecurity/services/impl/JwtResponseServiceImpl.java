package com.ecclesiaflow.springsecurity.services.impl;

import com.ecclesiaflow.springsecurity.dto.JwtAuthenticationResponse;
import com.ecclesiaflow.springsecurity.entities.Member;
import com.ecclesiaflow.springsecurity.services.JWTService;
import com.ecclesiaflow.springsecurity.services.JwtResponseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;

/**
 * Implémentation du service de construction des réponses JWT
 * Responsabilité unique : créer et formater les réponses d'authentification
 */
@Service
@RequiredArgsConstructor
public class JwtResponseServiceImpl implements JwtResponseService {
    
    private final JWTService jwtService;
    
    @Override
    public JwtAuthenticationResponse createAuthenticationResponse(Member member) {
        String jwt = jwtService.generateToken(member);
        String refreshToken = jwtService.generateRefreshToken(new HashMap<>(), member);
        
        return buildJwtResponse(jwt, refreshToken);
    }
    
    @Override
    public JwtAuthenticationResponse createRefreshResponse(Member member, String existingRefreshToken) {
        String newJwt = jwtService.generateToken(member);
        
        return buildJwtResponse(newJwt, existingRefreshToken);
    }
    
    /**
     * Construit une réponse JWT avec les tokens fournis
     */
    private JwtAuthenticationResponse buildJwtResponse(String jwt, String refreshToken) {
        JwtAuthenticationResponse response = new JwtAuthenticationResponse();
        response.setToken(jwt);
        response.setRefreshToken(refreshToken);
        return response;
    }
}
