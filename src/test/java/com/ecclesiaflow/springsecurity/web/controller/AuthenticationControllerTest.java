package com.ecclesiaflow.springsecurity.web.controller;

import com.ecclesiaflow.springsecurity.business.domain.token.UserTokens;
import com.ecclesiaflow.springsecurity.business.domain.password.SigninCredentials;
import com.ecclesiaflow.springsecurity.business.domain.token.TokenCredentials;
import com.ecclesiaflow.springsecurity.business.services.AuthenticationService;
import com.ecclesiaflow.springsecurity.business.domain.member.Member;
import com.ecclesiaflow.springsecurity.web.dto.JwtAuthenticationResponse;
import com.ecclesiaflow.springsecurity.web.payloads.RefreshTokenRequest;
import com.ecclesiaflow.springsecurity.web.payloads.SigninRequest;
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

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthenticationControllerTest {

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private Jwt jwt;

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
        Member member = Member.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .password("password123")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .enabled(true)
                .build();

        UserTokens userTokens = new UserTokens("access-token-123", "refresh-token-456");
        JwtAuthenticationResponse expectedResponse = new JwtAuthenticationResponse();
        expectedResponse.setToken("access-token-123");
        expectedResponse.setRefreshToken("refresh-token-456");

        try (MockedStatic<AuthenticationMapper> mockedMapper = mockStatic(AuthenticationMapper.class);
             MockedStatic<MemberMapper> mockedMemberMapper = mockStatic(MemberMapper.class)) {

            mockedMemberMapper.when(() -> MemberMapper.fromSigninRequest(request))
                    .thenReturn(credentials);

            mockedMapper.when(() -> AuthenticationMapper.toDto(userTokens))
                    .thenReturn(expectedResponse);

            when(authenticationService.getAuthenticatedMember(any(SigninCredentials.class))).thenReturn(member);
            when(jwt.generateUserTokens(any(Member.class))).thenReturn(userTokens);

            // When
            ResponseEntity<JwtAuthenticationResponse> response = authenticationController.generateToken(request);

            // Then
            assertNotNull(response);
            assertEquals(200, response.getStatusCode().value());
            assertEquals(expectedResponse, response.getBody());

            verify(authenticationService).getAuthenticatedMember(any(SigninCredentials.class));
            verify(jwt).generateUserTokens(any(Member.class));
        }
    }


    @Test
    void refreshToken_ShouldReturnNewJwtAuthenticationResponse() {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("refresh-token-456");

        TokenCredentials tokenCredentials = new TokenCredentials("refresh-token-456");
        Member member = Member.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .password("password123")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .enabled(true)
                .build();

        UserTokens userTokens = new UserTokens("new-access-token-123", "new-refresh-token-456");
        JwtAuthenticationResponse expectedResponse = new JwtAuthenticationResponse();
        expectedResponse.setToken("new-access-token-123");
        expectedResponse.setRefreshToken("new-refresh-token-456");

        try (MockedStatic<AuthenticationMapper> mockedMapper = mockStatic(AuthenticationMapper.class)) {
            mockedMapper.when(() -> AuthenticationMapper.fromRefreshTokenRequest(request))
                    .thenReturn(tokenCredentials);
            mockedMapper.when(() -> AuthenticationMapper.toDto(userTokens))
                    .thenReturn(expectedResponse);

            when(jwt.validateAndExtractEmail("refresh-token-456")).thenReturn("user@example.com");
            when(authenticationService.getMemberByEmail("user@example.com")).thenReturn(member);
            when(jwt.refreshTokenForMember("refresh-token-456", member)).thenReturn(userTokens);

            // When
            ResponseEntity<JwtAuthenticationResponse> response = authenticationController.refreshToken(request);

            // Then
            assertNotNull(response);
            assertEquals(200, response.getStatusCode().value());
            assertEquals(expectedResponse, response.getBody());

            verify(jwt).validateAndExtractEmail("refresh-token-456");
            verify(authenticationService).getMemberByEmail("user@example.com");
            verify(jwt).refreshTokenForMember("refresh-token-456", member);
        }
    }


}
