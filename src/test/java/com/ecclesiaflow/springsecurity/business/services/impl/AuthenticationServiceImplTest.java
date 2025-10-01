package com.ecclesiaflow.springsecurity.business.services.impl;

import com.ecclesiaflow.springsecurity.business.domain.password.SigninCredentials;
import com.ecclesiaflow.springsecurity.business.domain.member.Member;
import com.ecclesiaflow.springsecurity.business.domain.member.MemberRepository;
import com.ecclesiaflow.springsecurity.web.exception.InvalidCredentialsException;
import com.ecclesiaflow.springsecurity.business.exceptions.MemberNotFoundException;
import com.ecclesiaflow.springsecurity.web.security.Jwt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationServiceImpl - Tests Unitaires")
class AuthenticationServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private Jwt jwt;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthenticationServiceImpl authenticationService;

    private Member mockMember;
    private SigninCredentials credentials;
    private static final String EMAIL = "test@ecclesiaflow.com";

    @BeforeEach
    void setUp() {
        mockMember = Member.builder().email(EMAIL).id(UUID.randomUUID()).build();
        credentials = new SigninCredentials(EMAIL, "password");
    }

    // ====================================================================
    // Tests getAuthenticatedMember
    // ====================================================================

    @Test
    @DisplayName("Devrait retourner le Member authentifié en cas de succès")
    void getAuthenticatedMember_ShouldReturnMember_OnSuccess() {
        // Arrange
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(EMAIL, credentials.password());
        when(authenticationManager.authenticate(token)).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(mockMember);

        // Act
        Member result = authenticationService.getAuthenticatedMember(credentials);

        // Assert
        assertThat(result).isEqualTo(mockMember);
        verify(authenticationManager).authenticate(token);
    }

    @Test
    @DisplayName("Devrait lever InvalidCredentialsException si l'authentification échoue")
    void getAuthenticatedMember_ShouldThrowInvalidCredentialsException_OnAuthFailure() {
        // Arrange
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(EMAIL, credentials.password());
        when(authenticationManager.authenticate(token)).thenThrow(new BadCredentialsException("Bad Credentials"));

        // Act & Assert
        assertThatThrownBy(() -> authenticationService.getAuthenticatedMember(credentials))
                .isInstanceOf(BadCredentialsException.class); // BadCredentialsException est levée par Spring Security

        verify(authenticationManager).authenticate(token);
    }

    // ====================================================================
    // Tests getMemberByEmail
    // ====================================================================

    @Test
    @DisplayName("Devrait retourner le Member par email en cas de succès")
    void getMemberByEmail_ShouldReturnMember_OnSuccess() {
        // Arrange
        when(memberRepository.findByEmail(EMAIL)).thenReturn(Optional.of(mockMember));

        // Act
        Member result = authenticationService.getMemberByEmail(EMAIL);

        // Assert
        assertThat(result).isEqualTo(mockMember);
        verify(memberRepository).findByEmail(EMAIL);
    }

    @Test
    @DisplayName("Devrait lever MemberNotFoundException si l'email n'est pas trouvé")
    void getMemberByEmail_ShouldThrowMemberNotFoundException_OnNotFound() {
        // Arrange
        when(memberRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authenticationService.getMemberByEmail(EMAIL))
                .isInstanceOf(MemberNotFoundException.class)
                .hasMessageContaining("Membre introuvable pour l'email: " + EMAIL);

        verify(memberRepository).findByEmail(EMAIL);
    }

    // ====================================================================
    // Tests getEmailFromValidatedTempToken
    // ====================================================================

    @Test
    @DisplayName("Devrait retourner l'email si le token temporaire est valide")
    void getEmailFromValidatedTempToken_ShouldReturnEmail_OnValidToken() {
        // Arrange
        String token = "valid_temp_token";
        String extractedEmail = "user_from_token@test.com";
        when(jwt.extractEmailFromTemporaryToken(token)).thenReturn(extractedEmail);
        when(jwt.validateTemporaryToken(token, extractedEmail)).thenReturn(true);

        // Act
        String resultEmail = authenticationService.getEmailFromValidatedTempToken(token);

        // Assert
        assertThat(resultEmail).isEqualTo(extractedEmail);
        verify(jwt).extractEmailFromTemporaryToken(token);
        verify(jwt).validateTemporaryToken(token, extractedEmail);
    }

    @Test
    @DisplayName("Devrait lever InvalidCredentialsException si le token temporaire est invalide")
    void getEmailFromValidatedTempToken_ShouldThrowInvalidCredentialsException_OnInvalidToken() {
        // Arrange
        String token = "invalid_temp_token";
        String extractedEmail = "user_from_token@test.com";
        when(jwt.extractEmailFromTemporaryToken(token)).thenReturn(extractedEmail);
        when(jwt.validateTemporaryToken(token, extractedEmail)).thenReturn(false); // Validation échoue

        // Act & Assert
        assertThatThrownBy(() -> authenticationService.getEmailFromValidatedTempToken(token))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Token temporaire invalide ou expiré");

        verify(jwt).validateTemporaryToken(token, extractedEmail);
    }

    @Test
    @DisplayName("Devrait lever IllegalArgumentException si le token est null")
    void getEmailFromValidatedTempToken_ShouldThrowIllegalArgumentException_OnNullToken() {
        // Act & Assert
        assertThatThrownBy(() -> authenticationService.getEmailFromValidatedTempToken(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Le token temporaire ne peut pas être null ou vide");
        verifyNoInteractions(jwt);
    }

    @Test
    @DisplayName("Devrait lever IllegalArgumentException si le token est vide")
    void getEmailFromValidatedTempToken_ShouldThrowIllegalArgumentException_OnEmptyToken() {
        // Act & Assert
        assertThatThrownBy(() -> authenticationService.getEmailFromValidatedTempToken(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Le token temporaire ne peut pas être null ou vide");
        verifyNoInteractions(jwt);
    }
}