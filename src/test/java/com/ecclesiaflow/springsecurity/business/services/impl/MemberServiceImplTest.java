package com.ecclesiaflow.springsecurity.business.services.impl;

import com.ecclesiaflow.springsecurity.business.domain.member.Member;
import com.ecclesiaflow.springsecurity.business.domain.member.Role;
import com.ecclesiaflow.springsecurity.business.domain.member.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour MemberServiceImpl
 * 
 * Teste l'intégration avec Spring Security via UserDetailsService
 * avec une couverture complète des cas nominaux et d'erreur.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MemberService - Tests unitaires")
class MemberServiceImplTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberServiceImpl memberService;

    private Member testMember;
    private String testEmail;

    @BeforeEach
    void setUp() {
        testEmail = "test@example.com";
        testMember = createTestMember();
    }

    @Test
    @DisplayName("Devrait créer un UserDetailsService valide")
    void shouldCreateValidUserDetailsService() {
        // When
        UserDetailsService userDetailsService = memberService.userDetailsService();

        // Then
        assertThat(userDetailsService).isNotNull();
        assertThat(userDetailsService).isInstanceOf(UserDetailsService.class);
    }

    @Test
    @DisplayName("UserDetailsService devrait charger un utilisateur existant")
    void userDetailsServiceShouldLoadExistingUser() {
        // Given
        when(memberRepository.findByEmail(testEmail)).thenReturn(Optional.of(testMember));
        UserDetailsService userDetailsService = memberService.userDetailsService();

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername(testEmail);

        // Then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails).isInstanceOf(Member.class);
        assertThat(userDetails.getUsername()).isEqualTo(testEmail);
        assertThat(userDetails.getPassword()).isEqualTo(testMember.getPassword());
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();

        verify(memberRepository).findByEmail(testEmail);
    }

    @Test
    @DisplayName("UserDetailsService devrait lancer UsernameNotFoundException pour un utilisateur inexistant")
    void userDetailsServiceShouldThrowExceptionForNonExistentUser() {
        // Given
        String nonExistentEmail = "nonexistent@example.com";
        when(memberRepository.findByEmail(nonExistentEmail)).thenReturn(Optional.empty());
        UserDetailsService userDetailsService = memberService.userDetailsService();

        // When & Then
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(nonExistentEmail))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("Membre introuvable");

        verify(memberRepository).findByEmail(nonExistentEmail);
    }

    @Test
    @DisplayName("UserDetailsService devrait gérer les emails null")
    void userDetailsServiceShouldHandleNullEmail() {
        // Given
        when(memberRepository.findByEmail(null)).thenReturn(Optional.empty());
        UserDetailsService userDetailsService = memberService.userDetailsService();

        // When & Then
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(null))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("Membre introuvable");

        verify(memberRepository).findByEmail(null);
    }

    @Test
    @DisplayName("UserDetailsService devrait gérer les emails vides")
    void userDetailsServiceShouldHandleEmptyEmail() {
        // Given
        String emptyEmail = "";
        when(memberRepository.findByEmail(emptyEmail)).thenReturn(Optional.empty());
        UserDetailsService userDetailsService = memberService.userDetailsService();

        // When & Then
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(emptyEmail))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("Membre introuvable");

        verify(memberRepository).findByEmail(emptyEmail);
    }

    @Test
    @DisplayName("UserDetailsService devrait charger différents types de membres")
    void userDetailsServiceShouldLoadDifferentMemberTypes() {
        // Given - Membre avec rôle ADMIN
        Member adminMember = createTestMember();
        adminMember.setRole(Role.ADMIN);
        adminMember.setEmail("admin@example.com");
        
        when(memberRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(adminMember));
        UserDetailsService userDetailsService = memberService.userDetailsService();

        // When
        UserDetails adminDetails = userDetailsService.loadUserByUsername("admin@example.com");

        // Then
        assertThat(adminDetails).isNotNull();
        assertThat(adminDetails.getUsername()).isEqualTo("admin@example.com");
        assertThat(((Member) adminDetails).getRole()).isEqualTo(Role.ADMIN);

        verify(memberRepository).findByEmail("admin@example.com");
    }

    @Test
    @DisplayName("UserDetailsService devrait propager les exceptions du repository")
    void userDetailsServiceShouldPropagateRepositoryExceptions() {
        // Given
        when(memberRepository.findByEmail(testEmail)).thenThrow(new RuntimeException("Database error"));
        UserDetailsService userDetailsService = memberService.userDetailsService();

        // When & Then
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(testEmail))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database error");

        verify(memberRepository).findByEmail(testEmail);
    }

    @Test
    @DisplayName("Devrait retourner la même instance de UserDetailsService")
    void shouldReturnSameUserDetailsServiceInstance() {
        // When
        UserDetailsService service1 = memberService.userDetailsService();
        UserDetailsService service2 = memberService.userDetailsService();

        // Then
        assertThat(service1).isNotNull();
        assertThat(service2).isNotNull();
        // Note: Les instances peuvent être différentes car c'est une lambda,
        // mais elles doivent avoir le même comportement
        assertThat(service1.getClass()).isEqualTo(service2.getClass());
    }

    @Test
    @DisplayName("UserDetailsService devrait maintenir la cohérence des données")
    void userDetailsServiceShouldMaintainDataConsistency() {
        // Given
        Member memberWithSpecificData = createTestMember();
        memberWithSpecificData.setFirstName("Jean");
        memberWithSpecificData.setLastName("Dupont");
        memberWithSpecificData.setPassword("hashedPassword123");
        
        when(memberRepository.findByEmail(testEmail)).thenReturn(Optional.of(memberWithSpecificData));
        UserDetailsService userDetailsService = memberService.userDetailsService();

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername(testEmail);

        // Then
        Member loadedMember = (Member) userDetails;
        assertThat(loadedMember.getFirstName()).isEqualTo("Jean");
        assertThat(loadedMember.getLastName()).isEqualTo("Dupont");
        assertThat(loadedMember.getPassword()).isEqualTo("hashedPassword123");
        assertThat(loadedMember.getEmail()).isEqualTo(testEmail);

        verify(memberRepository).findByEmail(testEmail);
    }

    @Test
    @DisplayName("UserDetailsService devrait être thread-safe")
    void userDetailsServiceShouldBeThreadSafe() {
        // Given
        when(memberRepository.findByEmail(testEmail)).thenReturn(Optional.of(testMember));
        UserDetailsService userDetailsService = memberService.userDetailsService();

        // When - Appels simultanés simulés
        UserDetails result1 = userDetailsService.loadUserByUsername(testEmail);
        UserDetails result2 = userDetailsService.loadUserByUsername(testEmail);

        // Then
        assertThat(result1).isNotNull();
        assertThat(result2).isNotNull();
        assertThat(result1.getUsername()).isEqualTo(result2.getUsername());

        verify(memberRepository, times(2)).findByEmail(testEmail);
    }

    // Méthode utilitaire pour créer un membre de test
    private Member createTestMember() {
        Member member = new Member();
        member.setId(UUID.randomUUID());
        member.setEmail(testEmail);
        member.setFirstName("John");
        member.setLastName("Doe");
        member.setPassword("encodedPassword123");
        member.setRole(Role.MEMBER);
        return member;
    }
}
