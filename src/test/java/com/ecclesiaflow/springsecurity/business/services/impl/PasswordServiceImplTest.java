package com.ecclesiaflow.springsecurity.business.services.impl;

import com.ecclesiaflow.springsecurity.business.domain.member.Member;
import com.ecclesiaflow.springsecurity.business.domain.member.MemberRepository;
import com.ecclesiaflow.springsecurity.business.domain.member.MembersClient;
import com.ecclesiaflow.springsecurity.business.domain.member.Role;
import com.ecclesiaflow.springsecurity.business.encryption.PasswordEncoderUtil;
import com.ecclesiaflow.springsecurity.web.exception.InvalidRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordServiceImpl - Tests Unitaires")
class PasswordServiceImplTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private PasswordEncoderUtil passwordEncoder;
    @Mock
    private MembersClient membersClient;

    @InjectMocks
    private PasswordServiceImpl passwordService;

    private static final String EMAIL = "user@test.com";
    private static final String NEW_PASSWORD = "new_strong_password";
    private static final String ENCODED_PASSWORD = "encoded_new_password";

    @BeforeEach
    void setUp() {
        // Rendre ces stubs "lenient" car ils peuvent être surchargés ou non utilisés dans certains tests
        lenient().when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        lenient().when(membersClient.isEmailNotConfirmed(EMAIL)).thenReturn(false);
    }

    // ====================================================================
    // Tests setInitialPassword
    // ====================================================================

    @Nested
    @DisplayName("setInitialPassword")
    class SetInitialPasswordTests {

        @Test
        @DisplayName("Devrait créer et sauvegarder un nouveau Member si non existant")
        void shouldCreateAndSaveNewMember_IfNotFound() {
            // Arrange
            when(memberRepository.getByEmail(EMAIL)).thenReturn(Optional.empty());

            // Act
            passwordService.setInitialPassword(EMAIL, NEW_PASSWORD);

            // Assert
            ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
            verify(memberRepository).save(memberCaptor.capture());
            Member savedMember = memberCaptor.getValue();

            assertThat(savedMember.getEmail()).isEqualTo(EMAIL);
            assertThat(savedMember.getPassword()).isEqualTo(ENCODED_PASSWORD);
            assertThat(savedMember.getRole()).isEqualTo(Role.MEMBER);
            assertThat(savedMember.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("Devrait mettre à jour un Member existant si non 'enabled'")
        void shouldUpdateExistingMember_IfNotEnabled() {
            // Arrange
            Member existingMember = Member.builder().email(EMAIL).enabled(false).password(null).role(Role.MEMBER).build();
            when(memberRepository.getByEmail(EMAIL)).thenReturn(Optional.of(existingMember));

            // Act
            passwordService.setInitialPassword(EMAIL, NEW_PASSWORD);

            // Assert
            ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
            verify(memberRepository).save(memberCaptor.capture());
            Member savedMember = memberCaptor.getValue();

            assertThat(savedMember.getEmail()).isEqualTo(EMAIL);
            assertThat(savedMember.getPassword()).isEqualTo(ENCODED_PASSWORD);
            assertThat(savedMember.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("Devrait lever InvalidRequestException si le compte n'est pas confirmé")
        void shouldThrowInvalidRequestException_IfNotConfirmed() {
            // Arrange
            when(membersClient.isEmailNotConfirmed(EMAIL)).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> passwordService.setInitialPassword(EMAIL, NEW_PASSWORD))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("Le compte doit être confirmé avant de définir un mot de passe");
            verify(memberRepository, never()).save(any());
        }

        @Test
        @DisplayName("Devrait lever InvalidRequestException si le mot de passe est déjà défini")
        void shouldThrowInvalidRequestException_IfAlreadyEnabled() {
            // Arrange
            Member existingMember = Member.builder().email(EMAIL).enabled(true).password("old_encoded").role(Role.MEMBER).build();
            when(memberRepository.getByEmail(EMAIL)).thenReturn(Optional.of(existingMember));

            // Act & Assert
            assertThatThrownBy(() -> passwordService.setInitialPassword(EMAIL, NEW_PASSWORD))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("Le mot de passe a déjà été défini pour ce compte");
            verify(memberRepository, never()).save(any());
        }
    }

    // ====================================================================
    // Tests changePassword
    // ====================================================================

    @Nested
    @DisplayName("changePassword")
    class ChangePasswordTests {
        private static final String CURRENT_PASSWORD = "old_password";
        private static final String ENCODED_CURRENT_PASSWORD = "encoded_old_password";
        private Member enabledMember;

        @BeforeEach
        void setup() {
            enabledMember = Member.builder()
                    .email(EMAIL)
                    .password(ENCODED_CURRENT_PASSWORD)
                    .enabled(true)
                    .role(Role.MEMBER)
                    .build();

            // CORRECTION : Utiliser lenient() pour éviter UnnecessaryStubbingException,
            // car ce stub est surchargé dans shouldThrowRuntimeException_IfMemberNotFound
            lenient().when(memberRepository.getByEmail(EMAIL)).thenReturn(Optional.of(enabledMember));
        }

        @Test
        @DisplayName("Devrait changer et sauvegarder le nouveau mot de passe en cas de succès")
        void shouldChangePassword_OnSuccess() {
            // Arrange
            when(passwordEncoder.matches(CURRENT_PASSWORD, ENCODED_CURRENT_PASSWORD)).thenReturn(true);

            // Act
            passwordService.changePassword(EMAIL, CURRENT_PASSWORD, NEW_PASSWORD);

            // Assert
            ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
            verify(memberRepository).save(memberCaptor.capture());
            Member savedMember = memberCaptor.getValue();

            assertThat(savedMember.getPassword()).isEqualTo(ENCODED_PASSWORD);
            assertThat(savedMember.getUpdatedAt()).isNotNull();
            assertThat(savedMember.getUpdatedAt()).isAfterOrEqualTo(enabledMember.getUpdatedAt() != null ? enabledMember.getUpdatedAt() : LocalDateTime.MIN);
        }

        @Test
        @DisplayName("Devrait lever InvalidRequestException si le compte n'est pas confirmé")
        void shouldThrowInvalidRequestException_IfNotConfirmed() {
            // Arrange
            when(membersClient.isEmailNotConfirmed(EMAIL)).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> passwordService.changePassword(EMAIL, CURRENT_PASSWORD, NEW_PASSWORD))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("Le compte doit être confirmé pour changer le mot de passe");
            verify(memberRepository, never()).save(any());
        }

        @Test
        @DisplayName("Devrait lever RuntimeException si le membre n'est pas trouvé")
        void shouldThrowRuntimeException_IfMemberNotFound() {
            // Arrange
            // Surcharge du stub par défaut du setup pour simuler l'absence du membre
            when(memberRepository.getByEmail(EMAIL)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> passwordService.changePassword(EMAIL, CURRENT_PASSWORD, NEW_PASSWORD))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Membre non trouvé avec l'email: " + EMAIL);
            // On vérifie que la vérification de confirmation a eu lieu avant la levée de l'exception
            verify(membersClient).isEmailNotConfirmed(EMAIL);
        }

        @Test
        @DisplayName("Devrait lever RuntimeException si l'ancien mot de passe est null ou vide")
        void shouldThrowRuntimeException_IfPasswordIsNull() {
            // Arrange
            Member memberWithoutPassword = enabledMember.toBuilder().password(null).build();
            when(memberRepository.getByEmail(EMAIL)).thenReturn(Optional.of(memberWithoutPassword));

            // Act & Assert
            assertThatThrownBy(() -> passwordService.changePassword(EMAIL, CURRENT_PASSWORD, NEW_PASSWORD))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Aucun mot de passe défini pour ce membre");
        }

        @Test
        @DisplayName("Devrait lever RuntimeException si le mot de passe actuel est incorrect")
        void shouldThrowRuntimeException_IfCurrentPasswordIsIncorrect() {
            // Arrange
            when(passwordEncoder.matches(CURRENT_PASSWORD, ENCODED_CURRENT_PASSWORD)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> passwordService.changePassword(EMAIL, CURRENT_PASSWORD, NEW_PASSWORD))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Mot de passe actuel incorrect");
            verify(memberRepository, never()).save(any());
        }
    }
}