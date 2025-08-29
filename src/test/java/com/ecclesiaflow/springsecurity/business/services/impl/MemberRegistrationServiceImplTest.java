package com.ecclesiaflow.springsecurity.business.services.impl;

import com.ecclesiaflow.springsecurity.business.domain.MemberRegistration;
import com.ecclesiaflow.springsecurity.business.encryption.PasswordEncoderUtil;
import com.ecclesiaflow.springsecurity.io.entities.Member;
import com.ecclesiaflow.springsecurity.io.entities.Role;
import com.ecclesiaflow.springsecurity.io.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour MemberRegistrationServiceImpl
 * 
 * Teste les fonctionnalités d'enregistrement de nouveaux membres avec validation
 * de l'unicité des emails et encodage sécurisé des mots de passe.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MemberRegistrationService - Tests unitaires")
class MemberRegistrationServiceImplTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoderUtil passwordEncoderUtil;

    @InjectMocks
    private MemberRegistrationServiceImpl memberRegistrationService;

    private MemberRegistration testRegistration;
    private Member testMember;

    @BeforeEach
    void setUp() {
        testRegistration = createTestRegistration();
        testMember = createTestMember();
    }

    @Test
    @DisplayName("Devrait enregistrer un nouveau membre avec succès")
    void shouldRegisterNewMemberSuccessfully() {
        // Given
        String encodedPassword = "encodedPassword123";
        when(memberRepository.findByEmail(testRegistration.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoderUtil.encode(testRegistration.getPassword())).thenReturn(encodedPassword);
        when(memberRepository.save(any(Member.class))).thenReturn(testMember);

        // When
        Member result = memberRegistrationService.registerMember(testRegistration);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getFirstName()).isEqualTo("John");
        assertThat(result.getLastName()).isEqualTo("Doe");

        verify(memberRepository).findByEmail(testRegistration.getEmail());
        verify(passwordEncoderUtil).encode(testRegistration.getPassword());
        verify(memberRepository).save(any(Member.class));
    }

    @Test
    @DisplayName("Devrait créer un membre avec le bon rôle par défaut")
    void shouldCreateMemberWithCorrectDefaultRole() {
        // Given
        String encodedPassword = "encodedPassword123";
        when(memberRepository.findByEmail(testRegistration.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoderUtil.encode(testRegistration.getPassword())).thenReturn(encodedPassword);
        when(memberRepository.save(any(Member.class))).thenReturn(testMember);

        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);

        // When
        memberRegistrationService.registerMember(testRegistration);

        // Then
        verify(memberRepository).save(memberCaptor.capture());
        Member savedMember = memberCaptor.getValue();
        
        assertThat(savedMember.getRole()).isEqualTo(Role.MEMBER);
        assertThat(savedMember.getEmail()).isEqualTo(testRegistration.getEmail());
        assertThat(savedMember.getFirstName()).isEqualTo(testRegistration.getFirstName());
        assertThat(savedMember.getLastName()).isEqualTo(testRegistration.getLastName());
        assertThat(savedMember.getPassword()).isEqualTo(encodedPassword);
    }

    @Test
    @DisplayName("Devrait encoder le mot de passe avant sauvegarde")
    void shouldEncodePasswordBeforeSaving() {
        // Given
        String rawPassword = "plainPassword123";
        String encodedPassword = "encodedPassword123";
        MemberRegistration registration = new MemberRegistration(
                "John", "Doe", "test@example.com", rawPassword
        );
        
        when(memberRepository.findByEmail(registration.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoderUtil.encode(anyString())).thenReturn(encodedPassword);
        when(memberRepository.save(any(Member.class))).thenReturn(testMember);

        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);

        // When
        memberRegistrationService.registerMember(registration);

        // Then
        verify(passwordEncoderUtil).encode(rawPassword);
        verify(memberRepository).save(memberCaptor.capture());
        
        Member savedMember = memberCaptor.getValue();
        assertThat(savedMember.getPassword()).isEqualTo(encodedPassword);
        assertThat(savedMember.getPassword()).isNotEqualTo(rawPassword);
    }

    @Test
    @DisplayName("Devrait lancer une exception si l'email existe déjà")
    void shouldThrowExceptionWhenEmailAlreadyExists() {
        // Given
        when(memberRepository.findByEmail(testRegistration.getEmail())).thenReturn(Optional.of(testMember));

        // When & Then
        assertThatThrownBy(() -> memberRegistrationService.registerMember(testRegistration))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Un compte avec cet email existe déjà.");

        verify(memberRepository).findByEmail(testRegistration.getEmail());
        verify(passwordEncoderUtil, never()).encode(anyString());
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    @DisplayName("Devrait retourner true si l'email est déjà utilisé")
    void shouldReturnTrueWhenEmailIsAlreadyUsed() {
        // Given
        String email = "existing@example.com";
        when(memberRepository.findByEmail(email)).thenReturn(Optional.of(testMember));

        // When
        boolean result = memberRegistrationService.isEmailAlreadyUsed(email);

        // Then
        assertThat(result).isTrue();
        verify(memberRepository).findByEmail(email);
    }

    @Test
    @DisplayName("Devrait retourner false si l'email n'est pas utilisé")
    void shouldReturnFalseWhenEmailIsNotUsed() {
        // Given
        String email = "new@example.com";
        when(memberRepository.findByEmail(email)).thenReturn(Optional.empty());

        // When
        boolean result = memberRegistrationService.isEmailAlreadyUsed(email);

        // Then
        assertThat(result).isFalse();
        verify(memberRepository).findByEmail(email);
    }

    @Test
    @DisplayName("Devrait gérer les emails null dans la vérification d'unicité")
    void shouldHandleNullEmailInEmailCheck() {
        // Given
        when(memberRepository.findByEmail(null)).thenReturn(Optional.empty());

        // When
        boolean result = memberRegistrationService.isEmailAlreadyUsed(null);

        // Then
        assertThat(result).isFalse();
        verify(memberRepository).findByEmail(null);
    }

    @Test
    @DisplayName("Devrait gérer les emails vides dans la vérification d'unicité")
    void shouldHandleEmptyEmailInEmailCheck() {
        // Given
        String emptyEmail = "";
        when(memberRepository.findByEmail(emptyEmail)).thenReturn(Optional.empty());

        // When
        boolean result = memberRegistrationService.isEmailAlreadyUsed(emptyEmail);

        // Then
        assertThat(result).isFalse();
        verify(memberRepository).findByEmail(emptyEmail);
    }

    @Test
    @DisplayName("Devrait propager les exceptions du repository lors de la sauvegarde")
    void shouldPropagateRepositoryExceptionsOnSave() {
        // Given
        when(memberRepository.findByEmail(testRegistration.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoderUtil.encode(testRegistration.getPassword())).thenReturn("encodedPassword");
        when(memberRepository.save(any(Member.class))).thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertThatThrownBy(() -> memberRegistrationService.registerMember(testRegistration))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database error");

        verify(memberRepository).findByEmail(testRegistration.getEmail());
        verify(passwordEncoderUtil).encode(testRegistration.getPassword());
        verify(memberRepository).save(any(Member.class));
    }

    @Test
    @DisplayName("Devrait propager les exceptions de l'encodeur de mot de passe")
    void shouldPropagatePasswordEncoderExceptions() {
        // Given
        when(memberRepository.findByEmail(testRegistration.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoderUtil.encode(testRegistration.getPassword()))
                .thenThrow(new RuntimeException("Encoding error"));

        // When & Then
        assertThatThrownBy(() -> memberRegistrationService.registerMember(testRegistration))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Encoding error");

        verify(memberRepository).findByEmail(testRegistration.getEmail());
        verify(passwordEncoderUtil).encode(testRegistration.getPassword());
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    @DisplayName("Devrait vérifier l'ordre des opérations lors de l'enregistrement")
    void shouldVerifyOperationOrderDuringRegistration() {
        // Given
        when(memberRepository.findByEmail(testRegistration.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoderUtil.encode(testRegistration.getPassword())).thenReturn("encodedPassword");
        when(memberRepository.save(any(Member.class))).thenReturn(testMember);

        // When
        memberRegistrationService.registerMember(testRegistration);

        // Then - Vérifier l'ordre des appels
        var inOrder = inOrder(memberRepository, passwordEncoderUtil);
        inOrder.verify(memberRepository).findByEmail(testRegistration.getEmail());
        inOrder.verify(passwordEncoderUtil).encode(testRegistration.getPassword());
        inOrder.verify(memberRepository).save(any(Member.class));
    }

    // Méthodes utilitaires pour créer les objets de test
    private MemberRegistration createTestRegistration() {
        return new MemberRegistration(
                "John",
                "Doe",
                "test@example.com",
                "password123"
        );
    }

    private Member createTestMember() {
        Member member = new Member();
        member.setId(UUID.randomUUID());
        member.setEmail("test@example.com");
        member.setFirstName("John");
        member.setLastName("Doe");
        member.setPassword("encodedPassword123");
        member.setRole(Role.MEMBER);
        return member;
    }
}
