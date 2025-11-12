package com.ecclesiaflow.springsecurity.application.handlers;

import com.ecclesiaflow.springsecurity.business.domain.email.EmailClient;
import com.ecclesiaflow.springsecurity.business.events.PasswordChangedEvent;
import com.ecclesiaflow.springsecurity.business.events.PasswordResetEvent;
import com.ecclesiaflow.springsecurity.business.events.PasswordResetRequestedEvent;
import com.ecclesiaflow.springsecurity.business.events.PasswordSetEvent;
import com.ecclesiaflow.springsecurity.web.security.Jwt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordEventHandler - Tests Unitaires")
class PasswordEventHandlerTest {

    @Mock
    private EmailClient emailClient;
    @Mock
    private Jwt jwt;

    @InjectMocks
    private PasswordEventHandler eventHandler;

    private static final String EMAIL = "user@test.com";
    private static final UUID MEMBER_ID = UUID.randomUUID();
    private static final String TOKEN = "temporary-jwt-token";
    private static final String RESET_LINK = "http://localhost:3000/reset-password?token=" + TOKEN;
    private static final String FRONTEND_BASE_URL = "http://localhost:3000";

    @BeforeEach
    void setUp() {
        // Injecter la valeur frontendBaseUrl via réflexion
        ReflectionTestUtils.setField(eventHandler, "frontendBaseUrl", FRONTEND_BASE_URL);
    }

    // ====================================================================
    // Tests handlePasswordSet
    // ====================================================================

    @Nested
    @DisplayName("handlePasswordSet")
    class HandlePasswordSetTests {

        @Test
        @DisplayName("Devrait envoyer email de bienvenue lorsque événement PasswordSet est reçu")
        void shouldSendWelcomeEmail_WhenPasswordSetEventReceived() {
            // Arrange
            PasswordSetEvent event = new PasswordSetEvent(this, EMAIL);

            // Act
            eventHandler.handlePasswordSet(event);

            // Assert
            verify(emailClient).sendWelcomeEmail(EMAIL);
        }

        @Test
        @DisplayName("Ne devrait pas propager l'exception si envoi email échoue")
        void shouldNotPropagateException_WhenEmailSendingFails() {
            // Arrange
            PasswordSetEvent event = new PasswordSetEvent(this, EMAIL);
            doThrow(new RuntimeException("Email service unavailable"))
                    .when(emailClient).sendWelcomeEmail(EMAIL);

            // Act & Assert
            assertThatCode(() -> eventHandler.handlePasswordSet(event))
                    .doesNotThrowAnyException();
            
            verify(emailClient).sendWelcomeEmail(EMAIL);
        }
    }

    // ====================================================================
    // Tests handlePasswordChanged
    // ====================================================================

    @Nested
    @DisplayName("handlePasswordChanged")
    class HandlePasswordChangedTests {

        @Test
        @DisplayName("Devrait envoyer notification de changement de mot de passe")
        void shouldSendPasswordChangedNotification_WhenPasswordChangedEventReceived() {
            // Arrange
            PasswordChangedEvent event = new PasswordChangedEvent(this, EMAIL);

            // Act
            eventHandler.handlePasswordChanged(event);

            // Assert
            verify(emailClient).sendPasswordChangedNotification(EMAIL);
        }

        @Test
        @DisplayName("Ne devrait pas propager l'exception si envoi échoue")
        void shouldNotPropagateException_WhenEmailSendingFails() {
            // Arrange
            PasswordChangedEvent event = new PasswordChangedEvent(this, EMAIL);
            doThrow(new RuntimeException("Email service error"))
                    .when(emailClient).sendPasswordChangedNotification(EMAIL);

            // Act & Assert
            assertThatCode(() -> eventHandler.handlePasswordChanged(event))
                    .doesNotThrowAnyException();
        }
    }

    // ====================================================================
    // Tests handlePasswordResetRequested
    // ====================================================================

    @Nested
    @DisplayName("handlePasswordResetRequested")
    class HandlePasswordResetRequestedTests {

        @Test
        @DisplayName("Devrait générer token JWT et envoyer email de reset avec lien")
        void shouldGenerateTokenAndSendResetEmail_WhenPasswordResetRequestedEventReceived() {
            // Arrange
            PasswordResetRequestedEvent event = new PasswordResetRequestedEvent(this, EMAIL, MEMBER_ID);
            when(jwt.generateTemporaryToken(EMAIL, MEMBER_ID, "password_reset"))
                    .thenReturn(TOKEN);

            // Act
            eventHandler.handlePasswordResetRequested(event);

            // Assert
            verify(jwt).generateTemporaryToken(EMAIL, MEMBER_ID, "password_reset");
            
            ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
            verify(emailClient).sendPasswordResetEmail(eq(EMAIL), linkCaptor.capture());
            
            String capturedLink = linkCaptor.getValue();
            assertThat(capturedLink).isEqualTo(RESET_LINK);
            assertThat(capturedLink).startsWith(FRONTEND_BASE_URL);
            assertThat(capturedLink).contains(TOKEN);
        }

        @Test
        @DisplayName("Devrait construire le lien correctement avec le bon format")
        void shouldBuildResetLinkCorrectly() {
            // Arrange
            PasswordResetRequestedEvent event = new PasswordResetRequestedEvent(this, EMAIL, MEMBER_ID);
            when(jwt.generateTemporaryToken(EMAIL, MEMBER_ID, "password_reset"))
                    .thenReturn(TOKEN);

            // Act
            eventHandler.handlePasswordResetRequested(event);

            // Assert
            ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
            verify(emailClient).sendPasswordResetEmail(eq(EMAIL), linkCaptor.capture());
            
            assertThat(linkCaptor.getValue())
                    .matches("http://localhost:3000/reset-password\\?token=.+");
        }

        @Test
        @DisplayName("Ne devrait pas propager l'exception si génération token échoue")
        void shouldNotPropagateException_WhenTokenGenerationFails() {
            // Arrange
            PasswordResetRequestedEvent event = new PasswordResetRequestedEvent(this, EMAIL, MEMBER_ID);
            when(jwt.generateTemporaryToken(EMAIL, MEMBER_ID, "password_reset"))
                    .thenThrow(new RuntimeException("JWT generation error"));

            // Act & Assert
            assertThatCode(() -> eventHandler.handlePasswordResetRequested(event))
                    .doesNotThrowAnyException();
            
            verify(emailClient, never()).sendPasswordResetEmail(anyString(), anyString());
        }

        @Test
        @DisplayName("Ne devrait pas propager l'exception si envoi email échoue")
        void shouldNotPropagateException_WhenEmailSendingFails() {
            // Arrange
            PasswordResetRequestedEvent event = new PasswordResetRequestedEvent(this, EMAIL, MEMBER_ID);
            when(jwt.generateTemporaryToken(EMAIL, MEMBER_ID, "password_reset"))
                    .thenReturn(TOKEN);
            doThrow(new RuntimeException("Email service error"))
                    .when(emailClient).sendPasswordResetEmail(anyString(), anyString());

            // Act & Assert
            assertThatCode(() -> eventHandler.handlePasswordResetRequested(event))
                    .doesNotThrowAnyException();
        }
    }

    // ====================================================================
    // Tests handlePasswordReset
    // ====================================================================

    @Nested
    @DisplayName("handlePasswordReset")
    class HandlePasswordResetTests {

        @Test
        @DisplayName("Devrait envoyer notification de confirmation de reset")
        void shouldSendResetConfirmation_WhenPasswordResetEventReceived() {
            // Arrange
            PasswordResetEvent event = new PasswordResetEvent(this, EMAIL);

            // Act
            eventHandler.handlePasswordReset(event);

            // Assert
            verify(emailClient).sendPasswordChangedNotification(EMAIL);
        }

        @Test
        @DisplayName("Ne devrait pas propager l'exception si envoi échoue")
        void shouldNotPropagateException_WhenEmailSendingFails() {
            // Arrange
            PasswordResetEvent event = new PasswordResetEvent(this, EMAIL);
            doThrow(new RuntimeException("Email service unavailable"))
                    .when(emailClient).sendPasswordChangedNotification(EMAIL);

            // Act & Assert
            assertThatCode(() -> eventHandler.handlePasswordReset(event))
                    .doesNotThrowAnyException();
        }
    }
}
