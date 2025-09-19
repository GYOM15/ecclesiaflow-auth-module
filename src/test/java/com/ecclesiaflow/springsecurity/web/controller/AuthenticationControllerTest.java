package com.ecclesiaflow.springsecurity.web.controller;

import com.ecclesiaflow.springsecurity.business.domain.Tokens;
import com.ecclesiaflow.springsecurity.business.domain.SigninCredentials;
import com.ecclesiaflow.springsecurity.business.domain.RefreshTokenCredentials;
import com.ecclesiaflow.springsecurity.business.services.AuthenticationService;
import com.ecclesiaflow.springsecurity.io.entities.Member;
import com.ecclesiaflow.springsecurity.web.dto.JwtAuthenticationResponse;
import com.ecclesiaflow.springsecurity.web.dto.RefreshTokenRequest;
import com.ecclesiaflow.springsecurity.web.dto.SigninRequest;
import com.ecclesiaflow.springsecurity.web.mappers.AuthenticationMapper;
import com.ecclesiaflow.springsecurity.web.mappers.MemberMapper;
import com.ecclesiaflow.springsecurity.web.security.Jwt;
import com.ecclesiaflow.springsecurity.web.security.JwtProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthenticationControllerTest {

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private Jwt jwt;

    @Mock
    private JwtProcessor jwtProcessor;

    @InjectMocks
    private AuthenticationController authenticationController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void generateToken_ShouldReturnJwtAuthenticationResponse() {
        // Given
        SigninRequest request = new SigninRequest();
        request.setEmail("user@example.com");
        request.setPassword("password123");

        SigninCredentials credentials = new SigninCredentials("user@example.com", "password123");
        Member member = new Member();
        Tokens tokens = new Tokens("access-token-123", "refresh-token-456");
        JwtAuthenticationResponse expectedResponse = new JwtAuthenticationResponse();
        expectedResponse.setToken("access-token-123");
        expectedResponse.setRefreshToken("refresh-token-456");

        try (MockedStatic<AuthenticationMapper> mockedMapper = mockStatic(AuthenticationMapper.class);
             MockedStatic<MemberMapper> mockedMemberMapper = mockStatic(MemberMapper.class)) {

            // ✅ Mock statique MemberMapper
            mockedMemberMapper.when(() -> MemberMapper.fromSigninRequest(request))
                    .thenReturn(credentials);

            // ✅ Mock statique AuthenticationMapper
            mockedMapper.when(() -> AuthenticationMapper.toDto(tokens))
                    .thenReturn(expectedResponse);

            when(authenticationService.getAuthenticatedMember(any(SigninCredentials.class))).thenReturn(member);
            when(jwt.generateUserTokens(any(Member.class))).thenReturn(tokens);

            // When
            ResponseEntity<JwtAuthenticationResponse> response = authenticationController.generateToken(request);

            // Then
            assertNotNull(response);
            assertEquals(200, response.getStatusCodeValue());
            assertEquals(expectedResponse, response.getBody());

            verify(authenticationService).getAuthenticatedMember(any(SigninCredentials.class));
            verify(jwt).generateUserTokens(any(Member.class));
        }
    }


    @Test
    void refreshToken_ShouldReturnNewJwtAuthenticationResponse() {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setToken("refresh-token-456");

        RefreshTokenCredentials refreshTokenCredentials = new RefreshTokenCredentials("refresh-token-456");
        Member member = new Member();
        Tokens tokens = new Tokens("new-access-token-123", "new-refresh-token-456");
        JwtAuthenticationResponse expectedResponse = new JwtAuthenticationResponse();
        expectedResponse.setToken("new-access-token-123");
        expectedResponse.setRefreshToken("new-refresh-token-456");

        try (MockedStatic<AuthenticationMapper> mockedMapper = mockStatic(AuthenticationMapper.class)) {
            // ✅ mock statiques correctement
            mockedMapper.when(() -> AuthenticationMapper.fromRefreshTokenRequest(request))
                    .thenReturn(refreshTokenCredentials);
            mockedMapper.when(() -> AuthenticationMapper.toDto(tokens))
                    .thenReturn(expectedResponse);

            when(jwt.validateAndExtractEmail("refresh-token-456")).thenReturn("user@example.com");
            when(authenticationService.getMemberByEmail("user@example.com")).thenReturn(member);
            when(jwt.refreshTokenForMember("refresh-token-456", member)).thenReturn(tokens);

            // When
            ResponseEntity<JwtAuthenticationResponse> response = authenticationController.refreshToken(request);

            // Then
            assertNotNull(response);
            assertEquals(200, response.getStatusCodeValue());
            assertEquals(expectedResponse, response.getBody());

            verify(jwt).validateAndExtractEmail("refresh-token-456");
            verify(authenticationService).getMemberByEmail("user@example.com");
            verify(jwt).refreshTokenForMember("refresh-token-456", member);
        }
    }


}
