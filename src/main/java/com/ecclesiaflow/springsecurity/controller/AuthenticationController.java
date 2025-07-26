package com.ecclesiaflow.springsecurity.controller;

import com.ecclesiaflow.springsecurity.domain.SigninCredentials;
import com.ecclesiaflow.springsecurity.dto.JwtAuthenticationResponse;
import com.ecclesiaflow.springsecurity.dto.RefreshTokenRequest;
import com.ecclesiaflow.springsecurity.dto.SigninRequest;
import com.ecclesiaflow.springsecurity.services.AuthenticationService;
import com.ecclesiaflow.springsecurity.util.MemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    private final AuthenticationService authenticationService;

    @PostMapping(value = "/members/token", produces = "application/vnd.ecclesiaflow.members.v2+json")
    public ResponseEntity<JwtAuthenticationResponse> getAuthenticatedMember(@RequestBody SigninRequest signinRequest) {
        SigninCredentials credentials = MemberMapper.fromSigninRequest(signinRequest);
        return ResponseEntity.ok(authenticationService.getAuthenticatedMember(credentials));
    }

    @PostMapping(value = "/refresh", produces = "application/vnd.ecclesiaflow.members.v2+json")
    public ResponseEntity<JwtAuthenticationResponse> refresh(@RequestBody RefreshTokenRequest refreshTokenRequest) {
        return ResponseEntity.ok(authenticationService.refreshToken(refreshTokenRequest));
    }
}
