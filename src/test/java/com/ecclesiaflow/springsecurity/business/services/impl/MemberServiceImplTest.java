package com.ecclesiaflow.springsecurity.business.services.impl;

import com.ecclesiaflow.springsecurity.business.domain.member.Member;
import com.ecclesiaflow.springsecurity.business.domain.member.MemberRepository;
import com.ecclesiaflow.springsecurity.business.domain.member.Role;
import com.ecclesiaflow.springsecurity.business.services.adapters.MemberUserDetailsAdapter;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberServiceImpl - Tests Unitaires")
class MemberServiceImplTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberServiceImpl memberService;

    private UserDetailsService userDetailsService;
    private static final String EMAIL = "test@ecclesiaflow.com";
    private Member mockMember;

    @BeforeEach
    void setUp() {
        userDetailsService = memberService.userDetailsService();
        mockMember = Member.builder()
                .email(EMAIL)
                .password("encodedPassword")
                .role(Role.ADMIN)
                .enabled(true)
                .build();
    }

    // ====================================================================
    // Tests userDetailsService
    // ====================================================================

    @Test
    @DisplayName("Devrait retourner MemberUserDetailsAdapter si le membre est trouvé")
    void userDetailsService_ShouldReturnMemberUserDetailsAdapter_OnMemberFound() {
        // Arrange
        when(memberRepository.findByEmail(EMAIL)).thenReturn(Optional.of(mockMember));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername(EMAIL);

        // Assert
        assertThat(userDetails)
                .isInstanceOf(MemberUserDetailsAdapter.class);
        assertThat(userDetails.getUsername()).isEqualTo(EMAIL);
        assertThat(userDetails.getPassword()).isEqualTo("encodedPassword");
        verify(memberRepository).findByEmail(EMAIL);
    }

    @Test
    @DisplayName("Devrait lever UsernameNotFoundException si le membre n'est pas trouvé")
    void userDetailsService_ShouldThrowUsernameNotFoundException_OnMemberNotFound() {
        // Arrange
        when(memberRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(EMAIL))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("Membre introuvable avec l'email : " + EMAIL);

        verify(memberRepository).findByEmail(EMAIL);
    }
}