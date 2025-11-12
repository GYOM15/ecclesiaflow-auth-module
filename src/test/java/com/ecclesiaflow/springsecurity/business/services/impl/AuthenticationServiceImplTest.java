package com.ecclesiaflow.springsecurity.business.services.impl;

import com.ecclesiaflow.springsecurity.business.domain.password.SigninCredentials;
import com.ecclesiaflow.springsecurity.business.domain.member.Member;
import com.ecclesiaflow.springsecurity.business.domain.member.MemberRepository;
import com.ecclesiaflow.springsecurity.web.exception.JwtProcessingException;
import com.ecclesiaflow.springsecurity.web.exception.InvalidCredentialsException;
import com.ecclesiaflow.springsecurity.business.exceptions.MemberNotFoundException;
import com.ecclesiaflow.springsecurity.web.security.Jwt;
import com.ecclesiaflow.springsecurity.business.services.adapters.MemberUserDetailsAdapter;
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
import org.springframework.security.core.userdetails.UserDetails;

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
    @DisplayName("Devrait retourner le Member si le principal est un MemberUserDetailsAdapter")
    void getAuthenticatedMember_ShouldReturnMember_WhenPrincipalIsAdapter() {
        // Arrange
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(EMAIL, credentials.password());
        MemberUserDetailsAdapter adapter = new MemberUserDetailsAdapter(mockMember);
        when(authenticationManager.authenticate(token)).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(adapter);

        // Act
        Member result = authenticationService.getAuthenticatedMember(credentials);

        // Assert
        assertThat(result).isEqualTo(mockMember);
        verify(authenticationManager).authenticate(token);
    }

    @Test
    @DisplayName("Devrait lever InvalidCredentialsException si le principal UserDetails ne contient pas de Member")
    void getAuthenticatedMember_ShouldThrowInvalidCredentials_WhenUserDetailsWithoutMember() {
        // Arrange
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(EMAIL, credentials.password());
        UserDetails unrelatedUserDetails = mock(UserDetails.class);
        when(authenticationManager.authenticate(token)).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(unrelatedUserDetails);

        // Act & Assert
        assertThatThrownBy(() -> authenticationService.getAuthenticatedMember(credentials))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Impossible de récupérer le membre authentifié");

        verify(authenticationManager).authenticate(token);
    }

    @Test
    @DisplayName("Devrait lever InvalidCredentialsException si le principal n'est pas supporté")
    void getAuthenticatedMember_ShouldThrowInvalidCredentials_WhenPrincipalUnsupported() {
        // Arrange
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(EMAIL, credentials.password());
        Object unsupportedPrincipal = new Object();
        when(authenticationManager.authenticate(token)).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(unsupportedPrincipal);

        // Act & Assert
        assertThatThrownBy(() -> authenticationService.getAuthenticatedMember(credentials))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Impossible de récupérer le membre authentifié");

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
        when(memberRepository.getByEmail(EMAIL)).thenReturn(Optional.of(mockMember));

        // Act
        Member result = authenticationService.getMemberByEmail(EMAIL);

        // Assert
        assertThat(result).isEqualTo(mockMember);
        verify(memberRepository).getByEmail(EMAIL);
    }

    @Test
    @DisplayName("Devrait lever MemberNotFoundException si l'email n'est pas trouvé")
    void getMemberByEmail_ShouldThrowMemberNotFoundException_OnNotFound() {
        // Arrange
        when(memberRepository.getByEmail(EMAIL)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authenticationService.getMemberByEmail(EMAIL))
                .isInstanceOf(MemberNotFoundException.class)
                .hasMessageContaining("Membre introuvable pour l'email: " + EMAIL);

        verify(memberRepository).getByEmail(EMAIL);
    }

    // ====================================================================
    // Note: Tests pour getEmailFromValidatedTempToken supprimés
    // Cette méthode a été déplacée dans PasswordManagementDelegate
    // sous forme de méthodes privées (validatePasswordSetupToken/validatePasswordResetToken)
    // Les tests correspondants sont maintenant dans PasswordManagementDelegateTest
    // ====================================================================
}