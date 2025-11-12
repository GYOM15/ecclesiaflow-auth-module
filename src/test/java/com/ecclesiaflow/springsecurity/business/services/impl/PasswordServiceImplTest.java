package com.ecclesiaflow.springsecurity.business.services.impl;

import com.ecclesiaflow.springsecurity.business.domain.member.Member;
import com.ecclesiaflow.springsecurity.business.domain.member.MemberRepository;
import com.ecclesiaflow.springsecurity.business.domain.member.MembersClient;
import com.ecclesiaflow.springsecurity.business.domain.member.Role;
import com.ecclesiaflow.springsecurity.business.encryption.PasswordEncoderUtil;
import com.ecclesiaflow.springsecurity.business.events.PasswordResetRequestedEvent;
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
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

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
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PasswordServiceImpl passwordService;

    private static final String EMAIL = "user@test.com";
    private static final String NEW_PASSWORD = "new_strong_password";
    private static final String ENCODED_PASSWORD = "encoded_new_password";

    @BeforeEach
    void setUp() {
        // Rendre ces stubs "lenient" car ils peuvent être surchargés ou non utilisés dans certains tests
        lenient().when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        lenient().when(membersClient.isEmailNotConfirmed(anyString())).thenReturn(false);
        // Par défaut, memberRepository.save() retourne l'objet passé en paramètre
        lenient().when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));
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
            passwordService.setInitialPassword(EMAIL, NEW_PASSWORD, UUID.randomUUID());

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
            passwordService.setInitialPassword(EMAIL, NEW_PASSWORD, UUID.randomUUID());

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
            assertThatThrownBy(() -> passwordService.setInitialPassword(EMAIL, NEW_PASSWORD, UUID.randomUUID()))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("Impossible de traiter votre demande");
            verify(memberRepository, never()).save(any());
        }

        @Test
        @DisplayName("Devrait lever InvalidRequestException si le mot de passe est déjà défini")
        void shouldThrowInvalidRequestException_IfAlreadyEnabled() {
            // Arrange
            Member existingMember = Member.builder().email(EMAIL).enabled(true).password("old_encoded").role(Role.MEMBER).build();
            when(memberRepository.getByEmail(EMAIL)).thenReturn(Optional.of(existingMember));

            // Act & Assert
            assertThatThrownBy(() -> passwordService.setInitialPassword(EMAIL, NEW_PASSWORD, UUID.randomUUID()))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("Impossible de traiter votre demande");
            verify(memberRepository, never()).save(any());
        }

        @Test
        @DisplayName("Devrait normaliser l'email (toLowerCase + trim) lors de la création")
        void shouldNormalizeEmail_WhenCreatingNewMember() {
            // Arrange
            String emailWithSpaces = "  USER@TEST.COM  ";
            when(memberRepository.getByEmail(emailWithSpaces)).thenReturn(Optional.empty());
            when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(membersClient.isEmailNotConfirmed(emailWithSpaces)).thenReturn(false);

            // Act
            passwordService.setInitialPassword(emailWithSpaces, NEW_PASSWORD, UUID.randomUUID());

            // Assert
            ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
            verify(memberRepository).save(memberCaptor.capture());
            Member savedMember = memberCaptor.getValue();

            assertThat(savedMember.getEmail()).isEqualTo("user@test.com"); // normalisé
        }

        @Test
        @DisplayName("Devrait créer membre avec email déjà en minuscule et sans espaces")
        void shouldCreateMember_WithAlreadyNormalizedEmail() {
            // Arrange
            String normalizedEmail = "user@test.com";
            when(memberRepository.getByEmail(normalizedEmail)).thenReturn(Optional.empty());

            // Act
            passwordService.setInitialPassword(normalizedEmail, NEW_PASSWORD, UUID.randomUUID());

            // Assert
            ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
            verify(memberRepository).save(memberCaptor.capture());
            assertThat(memberCaptor.getValue().getEmail()).isEqualTo(normalizedEmail);
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
        @DisplayName("Devrait lever RuntimeException si le membre n'est pas trouvé")
        void shouldThrowRuntimeException_IfMemberNotFound() {
            // Arrange
            // Surcharge du stub par défaut du setup pour simuler l'absence du membre
            when(memberRepository.getByEmail(EMAIL)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> passwordService.changePassword(EMAIL, CURRENT_PASSWORD, NEW_PASSWORD))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Membre non trouvé avec l'email: " + EMAIL);
        }

        @Test
        @DisplayName("Devrait lever RuntimeException si l'ancien mot de passe est null ou vide")
        void shouldThrowRuntimeException_IfPasswordIsNull() {
            // Arrange
            Member memberWithoutPassword = enabledMember.toBuilder().password(null).build();
            when(memberRepository.getByEmail(EMAIL)).thenReturn(Optional.of(memberWithoutPassword));

            // Act & Assert
            assertThatThrownBy(() -> passwordService.changePassword(EMAIL, CURRENT_PASSWORD, NEW_PASSWORD))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("Authentification échouée");
        }

        @Test
        @DisplayName("Devrait lever RuntimeException si le mot de passe actuel est incorrect")
        void shouldThrowRuntimeException_IfCurrentPasswordIsIncorrect() {
            // Arrange
            when(passwordEncoder.matches(CURRENT_PASSWORD, ENCODED_CURRENT_PASSWORD)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> passwordService.changePassword(EMAIL, CURRENT_PASSWORD, NEW_PASSWORD))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("Authentification échouée");
            verify(memberRepository, never()).save(any());
        }

        @Test
        @DisplayName("Devrait lever RuntimeException si le mot de passe est une chaîne vide")
        void shouldThrowRuntimeException_IfPasswordIsEmpty() {
            // Arrange
            Member memberWithEmptyPassword = enabledMember.toBuilder().password("").build();
            when(memberRepository.getByEmail(EMAIL)).thenReturn(Optional.of(memberWithEmptyPassword));

            // Act & Assert
            assertThatThrownBy(() -> passwordService.changePassword(EMAIL, CURRENT_PASSWORD, NEW_PASSWORD))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("Authentification échouée");
        }

        @Test
        @DisplayName("Devrait mettre à jour updatedAt lors du changement de mot de passe")
        void shouldUpdateUpdatedAt_WhenChangingPassword() {
            // Arrange
            LocalDateTime oldTimestamp = LocalDateTime.now().minusDays(1);
            Member memberWithOldTimestamp = enabledMember.toBuilder().updatedAt(oldTimestamp).build();
            when(memberRepository.getByEmail(EMAIL)).thenReturn(Optional.of(memberWithOldTimestamp));
            when(passwordEncoder.matches(CURRENT_PASSWORD, ENCODED_CURRENT_PASSWORD)).thenReturn(true);

            // Act
            passwordService.changePassword(EMAIL, CURRENT_PASSWORD, NEW_PASSWORD);

            // Assert
            ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
            verify(memberRepository).save(memberCaptor.capture());
            Member savedMember = memberCaptor.getValue();

            assertThat(savedMember.getUpdatedAt()).isNotNull();
            assertThat(savedMember.getUpdatedAt()).isAfter(oldTimestamp);
        }

        @Test
        @DisplayName("Devrait lever InvalidRequestException si le mot de passe est null")
        void shouldThrowInvalidRequestException_IfPasswordIsNull() {
            // Arrange
            Member memberWithNullPassword = enabledMember.toBuilder().password(null).build();
            when(memberRepository.getByEmail(EMAIL)).thenReturn(Optional.of(memberWithNullPassword));

            // Act & Assert
            assertThatThrownBy(() -> passwordService.changePassword(EMAIL, CURRENT_PASSWORD, NEW_PASSWORD))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("Authentification échouée");
            verify(memberRepository, never()).save(any());
        }
    }

    // ====================================================================
    // Tests requestPasswordReset
    // ====================================================================

    @Nested
    @DisplayName("requestPasswordReset")
    class RequestPasswordResetTests {
        private Member existingMember;
        private UUID memberId;

        @BeforeEach
        void setup() {
            memberId = UUID.randomUUID();
            existingMember = Member.builder()
                    .email(EMAIL)
                    .memberId(memberId)
                    .password(ENCODED_PASSWORD)
                    .enabled(true)
                    .role(Role.MEMBER)
                    .build();
        }

        @Test
        @DisplayName("Devrait retourner le membre et publier événement si le membre existe")
        void shouldReturnMemberAndPublishEvent_WhenMemberExists() {
            // Arrange
            when(memberRepository.getByEmail(EMAIL)).thenReturn(Optional.of(existingMember));

            // Act
            Optional<Member> result = passwordService.requestPasswordReset(EMAIL);

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(existingMember);

            // Vérifier que l'événement a été publié
            ArgumentCaptor<PasswordResetRequestedEvent> eventCaptor = 
                    ArgumentCaptor.forClass(PasswordResetRequestedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            
            PasswordResetRequestedEvent publishedEvent = eventCaptor.getValue();
            assertThat(publishedEvent.getEmail()).isEqualTo(EMAIL);
            assertThat(publishedEvent.getMemberId()).isEqualTo(memberId);
        }

        @Test
        @DisplayName("Devrait retourner Optional.empty et ne pas publier d'événement si le membre n'existe pas")
        void shouldReturnEmptyAndNotPublishEvent_WhenMemberNotFound() {
            // Arrange
            when(memberRepository.getByEmail(EMAIL)).thenReturn(Optional.empty());

            // Act
            Optional<Member> result = passwordService.requestPasswordReset(EMAIL);

            // Assert
            assertThat(result).isEmpty();
            
            // Vérifier qu'aucun événement n'a été publié
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("Devrait fonctionner avec email normalisé")
        void shouldWorkWithNormalizedEmail() {
            // Arrange
            String emailWithSpaces = "  USER@TEST.COM  ";
            when(memberRepository.getByEmail(emailWithSpaces)).thenReturn(Optional.of(existingMember));

            // Act
            Optional<Member> result = passwordService.requestPasswordReset(emailWithSpaces);

            // Assert
            assertThat(result).isPresent();
            verify(memberRepository).getByEmail(emailWithSpaces);
        }

        @Test
        @DisplayName("Ne devrait pas modifier la base de données")
        void shouldNotModifyDatabase() {
            // Arrange
            when(memberRepository.getByEmail(EMAIL)).thenReturn(Optional.of(existingMember));

            // Act
            passwordService.requestPasswordReset(EMAIL);

            // Assert
            verify(memberRepository, never()).save(any());
        }
    }

    // ====================================================================
    // Tests resetPasswordWithToken
    // ====================================================================

    @Nested
    @DisplayName("resetPasswordWithToken")
    class ResetPasswordWithTokenTests {
        private Member enabledMember;

        @BeforeEach
        void setup() {
            enabledMember = Member.builder()
                    .email(EMAIL)
                    .password("old_encoded_password")
                    .enabled(true)
                    .role(Role.MEMBER)
                    .build();
            
            when(memberRepository.getByEmail(EMAIL)).thenReturn(Optional.of(enabledMember));
        }

        @Test
        @DisplayName("Devrait réinitialiser le mot de passe et publier l'événement")
        void shouldResetPasswordAndPublishEvent() {
            // Act
            Member result = passwordService.resetPasswordWithToken(EMAIL, NEW_PASSWORD);

            // Assert
            ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
            verify(memberRepository).save(memberCaptor.capture());
            Member savedMember = memberCaptor.getValue();

            assertThat(savedMember.getPassword()).isEqualTo(ENCODED_PASSWORD);
            assertThat(savedMember.getUpdatedAt()).isNotNull();
            assertThat(savedMember.getPasswordUpdatedAt()).isNotNull();

            // Vérifier que l'événement PasswordResetEvent a été publié
            ArgumentCaptor<com.ecclesiaflow.springsecurity.business.events.PasswordResetEvent> eventCaptor = 
                    ArgumentCaptor.forClass(com.ecclesiaflow.springsecurity.business.events.PasswordResetEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            
            com.ecclesiaflow.springsecurity.business.events.PasswordResetEvent publishedEvent = eventCaptor.getValue();
            assertThat(publishedEvent.getEmail()).isEqualTo(EMAIL);
        }

        @Test
        @DisplayName("Devrait lever RuntimeException si le membre n'existe pas")
        void shouldThrowRuntimeException_IfMemberNotFound() {
            // Arrange
            when(memberRepository.getByEmail(EMAIL)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> passwordService.resetPasswordWithToken(EMAIL, NEW_PASSWORD))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Membre non trouvé avec l'email: " + EMAIL);
            
            verify(memberRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("Devrait mettre à jour updatedAt et passwordUpdatedAt")
        void shouldUpdateTimestamps() {
            // Arrange
            LocalDateTime oldTimestamp = LocalDateTime.now().minusDays(7);
            Member memberWithOldTimestamp = enabledMember.toBuilder()
                    .updatedAt(oldTimestamp)
                    .passwordUpdatedAt(oldTimestamp)
                    .build();
            when(memberRepository.getByEmail(EMAIL)).thenReturn(Optional.of(memberWithOldTimestamp));

            // Act
            passwordService.resetPasswordWithToken(EMAIL, NEW_PASSWORD);

            // Assert
            ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
            verify(memberRepository).save(memberCaptor.capture());
            Member savedMember = memberCaptor.getValue();

            assertThat(savedMember.getUpdatedAt()).isAfter(oldTimestamp);
            assertThat(savedMember.getPasswordUpdatedAt()).isAfter(oldTimestamp);
        }
    }

    // ====================================================================
    // Tests changePassword (cas manquants pour couverture complète)
    // ====================================================================

    @Nested
    @DisplayName("changePassword - Cas supplémentaires")
    class AdditionalChangePasswordTests {
        private Member enabledMember;
        private static final String CURRENT_PASSWORD = "current_password";
        private static final String ENCODED_CURRENT_PASSWORD = "encoded_current";

        @BeforeEach
        void setup() {
            enabledMember = Member.builder()
                    .email(EMAIL)
                    .password(ENCODED_CURRENT_PASSWORD)
                    .enabled(true)
                    .role(Role.MEMBER)
                    .build();
            
            lenient().when(memberRepository.getByEmail(EMAIL)).thenReturn(Optional.of(enabledMember));
            lenient().when(passwordEncoder.matches(CURRENT_PASSWORD, ENCODED_CURRENT_PASSWORD)).thenReturn(true);
        }

        @Test
        @DisplayName("Devrait lever InvalidRequestException si nouveau password = ancien password")
        void shouldThrowInvalidRequestException_IfNewPasswordSameAsCurrent() {
            // Arrange
            when(memberRepository.getByEmail(EMAIL)).thenReturn(Optional.of(enabledMember));
            
            // Act & Assert
            assertThatThrownBy(() -> passwordService.changePassword(EMAIL, CURRENT_PASSWORD, CURRENT_PASSWORD))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("Le nouveau mot de passe n'est pas valide");
            
            verify(memberRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("Devrait publier PasswordChangedEvent après succès")
        void shouldPublishPasswordChangedEvent_OnSuccess() {
            // Act
            passwordService.changePassword(EMAIL, CURRENT_PASSWORD, NEW_PASSWORD);

            // Assert
            ArgumentCaptor<com.ecclesiaflow.springsecurity.business.events.PasswordChangedEvent> eventCaptor = 
                    ArgumentCaptor.forClass(com.ecclesiaflow.springsecurity.business.events.PasswordChangedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            
            com.ecclesiaflow.springsecurity.business.events.PasswordChangedEvent publishedEvent = eventCaptor.getValue();
            assertThat(publishedEvent.getEmail()).isEqualTo(EMAIL);
        }

        @Test
        @DisplayName("Devrait permettre currentPassword=null et ne pas vérifier l'égalité")
        void shouldAllowNullCurrentPassword_AndSkipEqualityCheck() {
            // Arrange
            when(memberRepository.getByEmail(EMAIL)).thenReturn(Optional.of(enabledMember));
            when(passwordEncoder.matches(null, ENCODED_CURRENT_PASSWORD)).thenReturn(false);
            
            // Act & Assert - currentPassword == null, donc la condition (currentPassword != null && ...) est false
            // et on ne vérifie pas si currentPassword.equals(newPassword)
            assertThatThrownBy(() -> passwordService.changePassword(EMAIL, null, NEW_PASSWORD))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("Authentification échouée");
            
            // La vérification "même password" est bypassée car currentPassword == null
            verify(memberRepository, never()).save(any());
        }
    }

    // ====================================================================
    // Tests setInitialPassword (cas manquants pour événements)
    // ====================================================================

    @Nested
    @DisplayName("setInitialPassword - Événements")
    class SetInitialPasswordEventsTests {

        @Test
        @DisplayName("Devrait publier PasswordSetEvent après définition initiale du mot de passe")
        void shouldPublishPasswordSetEvent_AfterSettingPassword() {
            // Arrange
            UUID memberId = UUID.randomUUID();
            when(memberRepository.getByEmail(EMAIL)).thenReturn(Optional.empty());

            // Act
            passwordService.setInitialPassword(EMAIL, NEW_PASSWORD, memberId);

            // Assert
            ArgumentCaptor<com.ecclesiaflow.springsecurity.business.events.PasswordSetEvent> eventCaptor = 
                    ArgumentCaptor.forClass(com.ecclesiaflow.springsecurity.business.events.PasswordSetEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            
            com.ecclesiaflow.springsecurity.business.events.PasswordSetEvent publishedEvent = eventCaptor.getValue();
            assertThat(publishedEvent.getEmail()).isEqualTo(EMAIL);
        }
    }
}