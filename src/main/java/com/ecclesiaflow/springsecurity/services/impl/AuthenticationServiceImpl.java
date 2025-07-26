package com.ecclesiaflow.springsecurity.services.impl;

import com.ecclesiaflow.springsecurity.domain.MemberRegistration;
import com.ecclesiaflow.springsecurity.domain.SigninCredentials;
import com.ecclesiaflow.springsecurity.dto.JwtAuthenticationResponse;
import com.ecclesiaflow.springsecurity.dto.RefreshTokenRequest;
import com.ecclesiaflow.springsecurity.entities.Member;
import com.ecclesiaflow.springsecurity.entities.Role;
import com.ecclesiaflow.springsecurity.repository.MemberRepository;
import com.ecclesiaflow.springsecurity.services.AuthenticationService;
import com.ecclesiaflow.springsecurity.services.JWTService;
import com.ecclesiaflow.springsecurity.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JWTService jwtService;

    public Member registerMember(MemberRegistration registration) {
        if (memberRepository.findByEmail(registration.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Un compte avec cet email existe déjà.");
        }
        Member member = new Member();

        member.setEmail(registration.getEmail());
        member.setPassword(EncryptionUtil.hashPassword(registration.getPassword()));
        member.setFirstName(registration.getFirstName());
        member.setLastName(registration.getLastName());
        member.setRole(Role.MEMBER);

        return memberRepository.save(member);
    }

    public JwtAuthenticationResponse getAuthenticatedMember(SigninCredentials credentials) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(credentials.getEmail(), credentials.getPassword()));

        var member = memberRepository.findByEmail(credentials.getEmail()).orElseThrow(()->new IllegalArgumentException("Invalid email or password"));
        var jwt = jwtService.generateToken(member);
        var refreshToken = jwtService.generateRefreshToken(new HashMap<>(), member);

        JwtAuthenticationResponse jwtAuthenticationResponse = new JwtAuthenticationResponse();
        jwtAuthenticationResponse.setToken(jwt);
        jwtAuthenticationResponse.setRefreshToken(refreshToken);
        return jwtAuthenticationResponse;
    }

    public JwtAuthenticationResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        String userEmail = jwtService.extractUserName(refreshTokenRequest.getToken());
        Member member = memberRepository.findByEmail(userEmail).orElseThrow(()->new IllegalArgumentException("Invalid email or password"));
        if (jwtService.isTokenValid(refreshTokenRequest.getToken(), member)) {
            var jwt = jwtService.generateToken(member);

            JwtAuthenticationResponse jwtAuthenticationResponse = new JwtAuthenticationResponse();
            jwtAuthenticationResponse.setToken(jwt);
            jwtAuthenticationResponse.setRefreshToken(refreshTokenRequest.getToken());
            return jwtAuthenticationResponse;
        }
        throw new IllegalArgumentException("Invalid refresh token.");
        //return null;
    }
}
