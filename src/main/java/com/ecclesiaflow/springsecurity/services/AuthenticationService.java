package com.ecclesiaflow.springsecurity.services;

import com.ecclesiaflow.springsecurity.domain.MemberRegistration;
import com.ecclesiaflow.springsecurity.domain.SigninCredentials;
import com.ecclesiaflow.springsecurity.dto.JwtAuthenticationResponse;
import com.ecclesiaflow.springsecurity.dto.RefreshTokenRequest;
import com.ecclesiaflow.springsecurity.dto.SignUpRequest;
import com.ecclesiaflow.springsecurity.dto.SigninRequest;
import com.ecclesiaflow.springsecurity.entities.Member;

public interface AuthenticationService {
    Member signup(MemberRegistration memberRegistration);
    JwtAuthenticationResponse signin(SigninCredentials signinCredentials);
    JwtAuthenticationResponse refreshToken(RefreshTokenRequest refreshTokenRequest);
}
