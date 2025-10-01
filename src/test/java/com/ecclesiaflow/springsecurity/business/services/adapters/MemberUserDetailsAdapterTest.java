package com.ecclesiaflow.springsecurity.business.services.adapters;

import com.ecclesiaflow.springsecurity.business.domain.member.Member;
import com.ecclesiaflow.springsecurity.business.domain.member.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MemberUserDetailsAdapter - Tests Unitaires")
class MemberUserDetailsAdapterTest {

    private MemberUserDetailsAdapter adapter;
    private Member mockMember;
    private static final String EMAIL = "user@ecclesiaflow.com";
    private static final String PASSWORD = "encoded_password_hash";
    private static final UUID ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMember = Member.builder()
                .id(ID)
                .email(EMAIL)
                .password(PASSWORD)
                .role(Role.ADMIN)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        adapter = new MemberUserDetailsAdapter(mockMember);
    }

    // ====================================================================
    // Tests du contrat UserDetails (Méthodes Spring Security)
    // ====================================================================

    @Test
    @DisplayName("getUsername - Devrait retourner l'email du membre")
    void getUsername_ShouldReturnMemberEmail() {
        assertThat(adapter.getUsername()).isEqualTo(EMAIL);
        assertThat(adapter.getUsername()).isEqualTo(mockMember.getEmail());
    }

    @Test
    @DisplayName("getPassword - Devrait retourner le mot de passe encodé du membre")
    void getPassword_ShouldReturnMemberPassword() {
        assertThat(adapter.getPassword()).isEqualTo(PASSWORD);
        assertThat(adapter.getPassword()).isEqualTo(mockMember.getPassword());
    }

    @Test
    @DisplayName("getAuthorities - Devrait retourner une collection avec le rôle préfixé 'ROLE_'")
    void getAuthorities_ShouldReturnRoleWithPrefix() {
        // Act
        Collection<? extends GrantedAuthority> authorities = adapter.getAuthorities();

        // Assert
        assertThat(authorities).hasSize(1);
        GrantedAuthority authority = authorities.iterator().next();

        // Vérifie la convention Spring Security : ROLE_ROLE_NAME
        assertThat(authority).isInstanceOf(SimpleGrantedAuthority.class);
        assertThat(authority.getAuthority()).isEqualTo("ROLE_" + Role.ADMIN.name());
    }

    @Test
    @DisplayName("isEnabled - Devrait retourner l'état 'enabled' du membre")
    void isEnabled_ShouldReturnMemberEnabledState() {
        // Case 1: Enabled = true
        assertThat(adapter.isEnabled()).isTrue();

        // Case 2: Enabled = false
        Member disabledMember = mockMember.toBuilder().enabled(false).build();
        MemberUserDetailsAdapter disabledAdapter = new MemberUserDetailsAdapter(disabledMember);
        assertThat(disabledAdapter.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("Les méthodes d'expiration/verrouillage doivent retourner true par défaut (selon les TODO)")
    void expirationMethods_ShouldReturnTrueByDefault() {
        assertThat(adapter.isAccountNonExpired()).isTrue();
        assertThat(adapter.isAccountNonLocked()).isTrue();
        assertThat(adapter.isCredentialsNonExpired()).isTrue();
    }

    // ====================================================================
    // Tests des méthodes d'accès direct au domaine
    // ====================================================================

    @Nested
    @DisplayName("Méthodes d'Accès Domaine")
    class DomainAccessTests {

        @Test
        @DisplayName("getMember - Devrait retourner l'objet Member encapsulé")
        void getMember_ShouldReturnEncapsulatedMember() {
            assertThat(adapter.getMember()).isEqualTo(mockMember);
            assertThat(adapter.getMember().getId()).isEqualTo(ID);
        }

        @Test
        @DisplayName("getEmail - Devrait retourner l'email du membre")
        void getEmail_ShouldReturnMemberEmail() {
            assertThat(adapter.getEmail()).isEqualTo(EMAIL);
        }

        @Test
        @DisplayName("getRole - Devrait retourner le rôle du membre")
        void getRole_ShouldReturnMemberRole() {
            assertThat(adapter.getRole()).isEqualTo(Role.ADMIN);
        }
    }

    // ====================================================================
    // Tests de la méthode utilitaire statique
    // ====================================================================

    @Nested
    @DisplayName("Méthode Statique ExtractMember")
    class ExtractMemberTests {

        @Test
        @DisplayName("extractMember - Devrait retourner le Member si l'instance est MemberUserDetailsAdapter")
        void extractMember_ShouldReturnMember_WhenInstanceMatches() {
            // Act
            Member extractedMember = MemberUserDetailsAdapter.extractMember(adapter);

            // Assert
            assertThat(extractedMember).isEqualTo(mockMember);
            assertThat(extractedMember.getEmail()).isEqualTo(EMAIL);
        }

        @Test
        @DisplayName("extractMember - Devrait retourner null si l'instance n'est pas MemberUserDetailsAdapter")
        void extractMember_ShouldReturnNull_WhenInstanceDoesNotMatch() {
            // Arrange: Utilise une implémentation UserDetails standard de Spring Security
            UserDetails nonAdapter = org.springframework.security.core.userdetails.User.builder()
                    .username("other@test.com")
                    .password("other_pwd")
                    .roles("USER")
                    .build();

            // Act
            Member extractedMember = MemberUserDetailsAdapter.extractMember(nonAdapter);

            // Assert
            assertThat(extractedMember).isNull();
        }

        @Test
        @DisplayName("extractMember - Devrait retourner null si l'instance est null")
        void extractMember_ShouldReturnNull_WhenInstanceIsNull() {
            // Act
            Member extractedMember = MemberUserDetailsAdapter.extractMember(null);

            // Assert
            assertThat(extractedMember).isNull();
        }
    }
}