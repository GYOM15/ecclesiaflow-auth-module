package com.ecclesiaflow.springsecurity.business.services.impl;

import com.ecclesiaflow.springsecurity.business.domain.member.MemberRegistration;
import com.ecclesiaflow.springsecurity.business.domain.password.SigninCredentials;
import com.ecclesiaflow.springsecurity.business.domain.member.Member;
import com.ecclesiaflow.springsecurity.business.domain.member.Role;
import com.ecclesiaflow.springsecurity.io.repository.MemberRepository;
import com.ecclesiaflow.springsecurity.business.exceptions.MemberNotFoundException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour AuthenticationServiceImpl
 * 
 * Teste les fonctionnalités d'authentification, d'inscription et de récupération des membres
 * avec une couverture complète des cas nominaux et d'erreur.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationService - Tests unitaires")
class AuthenticationServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private MemberRegistrationService memberRegistrationService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthenticationServiceImpl authenticationService;

    private Member testMember;
    private MemberRegistration testRegistration;
    private SigninCredentials testCredentials;

    @BeforeEach
    void setUp() {
        testMember = createTestMember();
        testRegistration = createTestRegistration();
        testCredentials = createTestCredentials();
    }

    @Test
    @DisplayName("Devrait enregistrer un nouveau membre avec succès")
    void shouldRegisterMemberSuccessfully() {
        // Given
        when(memberRegistrationService.registerMember(testRegistration)).thenReturn(testMember);

        // When
        Member result = authenticationService.registerMember(testRegistration);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(testMember.getEmail());
        assertThat(result.getFirstName()).isEqualTo(testMember.getFirstName());
        assertThat(result.getLastName()).isEqualTo(testMember.getLastName());
        
        verify(memberRegistrationService).registerMember(testRegistration);
        verifyNoMoreInteractions(memberRegistrationService);
    }

    @Test
    @DisplayName("Devrait propager l'exception lors de l'échec d'enregistrement")
    void shouldPropagateExceptionWhenRegistrationFails() {
        // Given
        when(memberRegistrationService.registerMember(testRegistration))
                .thenThrow(new IllegalArgumentException("Email déjà utilisé"));

        // When & Then
        assertThatThrownBy(() -> authenticationService.registerMember(testRegistration))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email déjà utilisé");

        verify(memberRegistrationService).registerMember(testRegistration);
    }

    @Test
    @DisplayName("Devrait authentifier un membre avec des identifiants valides")
    void shouldAuthenticateMemberWithValidCredentials() {
        // Given
        when(authentication.getPrincipal()).thenReturn(testMember);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        // When
        Member result = authenticationService.getAuthenticatedMember(testCredentials);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(testMember.getEmail());
        
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verifyNoMoreInteractions(authenticationManager);
    }

    @Test
    @DisplayName("Devrait lancer InvalidCredentialsException pour des identifiants invalides")
    void shouldThrowInvalidCredentialsExceptionForInvalidCredentials() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Identifiants incorrects"));

        // When & Then
        assertThatThrownBy(() -> authenticationService.getAuthenticatedMember(testCredentials))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Identifiants incorrects");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("Devrait créer le bon token d'authentification")
    void shouldCreateCorrectAuthenticationToken() {
        // Given
        when(authentication.getPrincipal()).thenReturn(testMember);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        // When
        authenticationService.getAuthenticatedMember(testCredentials);

        // Then
        verify(authenticationManager).authenticate(argThat(token -> {
            UsernamePasswordAuthenticationToken authToken = (UsernamePasswordAuthenticationToken) token;
            return testCredentials.email().equals(authToken.getName()) &&
                   testCredentials.password().equals(authToken.getCredentials());
        }));
    }

    @Test
    @DisplayName("Devrait récupérer un membre par email avec succès")
    void shouldGetMemberByEmailSuccessfully() {
        // Given
        String email = "test@example.com";
        when(memberRepository.findByEmail(email)).thenReturn(Optional.of(testMember));

        // When
        Member result = authenticationService.getMemberByEmail(email);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(testMember.getEmail());
        
        verify(memberRepository).findByEmail(email);
        verifyNoMoreInteractions(memberRepository);
    }

    @Test
    @DisplayName("Devrait lancer MemberNotFoundException quand le membre n'existe pas")
    void shouldThrowMemberNotFoundExceptionWhenMemberDoesNotExist() {
        // Given
        String email = "nonexistent@example.com";
        when(memberRepository.findByEmail(email)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authenticationService.getMemberByEmail(email))
                .isInstanceOf(MemberNotFoundException.class)
                .hasMessage("Membre introuvable pour l'email: " + email);

        verify(memberRepository).findByEmail(email);
    }

    @Test
    @DisplayName("Devrait gérer les emails null lors de la recherche")
    void shouldHandleNullEmailInGetMemberByEmail() {
        // Given
        when(memberRepository.findByEmail(null)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authenticationService.getMemberByEmail(null))
                .isInstanceOf(MemberNotFoundException.class)
                .hasMessage("Membre introuvable pour l'email: null");

        verify(memberRepository).findByEmail(null);
    }

    @Test
    @DisplayName("Devrait gérer les identifiants null lors de l'authentification")
    void shouldHandleNullCredentialsInAuthentication() {
        // Given
        SigninCredentials nullCredentials = new SigninCredentials(null, null);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Identifiants manquants"));

        // When & Then
        assertThatThrownBy(() -> authenticationService.getAuthenticatedMember(nullCredentials))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Identifiants manquants");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    // Méthodes utilitaires pour créer les objets de test
    private Member createTestMember() {
        Member member = new Member();
        member.setId(UUID.randomUUID());
        member.setEmail("test@example.com");
        member.setFirstName("John");
        member.setLastName("Doe");
        member.setPassword("encodedPassword");
        member.setRole(Role.MEMBER);
        return member;
    }

    private MemberRegistration createTestRegistration() {
        return new MemberRegistration(
                "test@example.com",
                "password123",
                "John",
                "Doe"
        );
    }

    private SigninCredentials createTestCredentials() {
        return new SigninCredentials("test@example.com", "password123");
    }
}
