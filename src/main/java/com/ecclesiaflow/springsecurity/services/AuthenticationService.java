package com.ecclesiaflow.springsecurity.services;

import com.ecclesiaflow.springsecurity.domain.MemberRegistration;
import com.ecclesiaflow.springsecurity.domain.SigninCredentials;
import com.ecclesiaflow.springsecurity.dto.JwtAuthenticationResponse;
import com.ecclesiaflow.springsecurity.dto.RefreshTokenRequest;
import com.ecclesiaflow.springsecurity.entities.Member;

public interface AuthenticationService {
    Member registerMember(MemberRegistration memberRegistration);
    JwtAuthenticationResponse getAuthenticatedMember(SigninCredentials signinCredentials);
    JwtAuthenticationResponse refreshToken(RefreshTokenRequest refreshTokenRequest);
}
