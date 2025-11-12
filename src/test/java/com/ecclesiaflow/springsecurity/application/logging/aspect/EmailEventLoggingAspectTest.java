package com.ecclesiaflow.springsecurity.application.logging.aspect;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.ecclesiaflow.springsecurity.business.events.PasswordChangedEvent;
import com.ecclesiaflow.springsecurity.business.events.PasswordResetEvent;
import com.ecclesiaflow.springsecurity.business.events.PasswordResetRequestedEvent;
import com.ecclesiaflow.springsecurity.business.events.PasswordSetEvent;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailEventLoggingAspect - Tests Unitaires")
class EmailEventLoggingAspectTest {

    @Mock
    private ProceedingJoinPoint joinPoint;

    @InjectMocks
    private EmailEventLoggingAspect loggingAspect;

    private ListAppender<ILoggingEvent> listAppender;
    private Logger logger;

    private static final String EMAIL = "user@test.com";
    private static final UUID MEMBER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        // Configurer le logger pour capturer les logs
        logger = (Logger) LoggerFactory.getLogger(EmailEventLoggingAspect.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        // Nettoyer après chaque test
        logger.detachAppender(listAppender);
    }

    // ====================================================================
    // Tests logWelcomeEmail
    // ====================================================================

    @Test
    @DisplayName("Devrait logger l'envoi d'email de bienvenue avant et après succès")
    void shouldLogWelcomeEmail_WhenSuccessful() throws Throwable {
        // Arrange
        PasswordSetEvent event = new PasswordSetEvent(this, EMAIL);
        when(joinPoint.getArgs()).thenReturn(new Object[]{event});
        when(joinPoint.proceed()).thenReturn(null);

        // Act
        loggingAspect.logWelcomeEmail(joinPoint);

        // Assert
        verify(joinPoint).proceed();
        
        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList).hasSize(2);
        assertThat(logsList.get(0).getFormattedMessage())
                .contains("📧", "[EMAIL]", "Sending welcome email to", EMAIL);
        assertThat(logsList.get(1).getFormattedMessage())
                .contains("✅", "[EMAIL]", "Welcome email sent successfully to", EMAIL);
    }

    @Test
    @DisplayName("Devrait logger l'erreur si envoi échoue et propager l'exception")
    void shouldLogError_WhenWelcomeEmailFails() throws Throwable {
        // Arrange
        PasswordSetEvent event = new PasswordSetEvent(this, EMAIL);
        RuntimeException exception = new RuntimeException("Email service unavailable");
        when(joinPoint.getArgs()).thenReturn(new Object[]{event});
        when(joinPoint.proceed()).thenThrow(exception);

        // Act & Assert
        assertThatThrownBy(() -> loggingAspect.logWelcomeEmail(joinPoint))
                .isEqualTo(exception);

        verify(joinPoint).proceed();

        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList).hasSizeGreaterThanOrEqualTo(2);

        // Vérifier le premier log (avant l'envoi)
        assertThat(logsList.get(0).getFormattedMessage())
                .contains("📧", "[EMAIL]", "Sending welcome email to", EMAIL);

        // Vérifier le dernier log (erreur)
        assertThat(logsList.getLast().getFormattedMessage())
                .contains("❌", "[EMAIL]", "Failed to send welcome email to", EMAIL);
    }


    // ====================================================================
    // Tests logPasswordChangedNotification
    // ====================================================================

    @Test
    @DisplayName("Devrait logger notification de changement de mot de passe")
    void shouldLogPasswordChangedNotification_WhenSuccessful() throws Throwable {
        // Arrange
        PasswordChangedEvent event = new PasswordChangedEvent(this, EMAIL);
        when(joinPoint.getArgs()).thenReturn(new Object[]{event});
        when(joinPoint.proceed()).thenReturn(null);

        // Act
        loggingAspect.logPasswordChangedNotification(joinPoint);

        // Assert
        verify(joinPoint).proceed();
        
        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList).hasSize(2);
        assertThat(logsList.get(0).getFormattedMessage())
                .contains("Sending password changed notification to", EMAIL);
        assertThat(logsList.get(1).getFormattedMessage())
                .contains("Password changed notification sent successfully to", EMAIL);
    }

    @Test
    @DisplayName("Devrait logger l'erreur si notification échoue")
    void shouldLogError_WhenPasswordChangedNotificationFails() throws Throwable {
        // Arrange
        PasswordChangedEvent event = new PasswordChangedEvent(this, EMAIL);
        RuntimeException exception = new RuntimeException("Email service error");
        when(joinPoint.getArgs()).thenReturn(new Object[]{event});
        when(joinPoint.proceed()).thenThrow(exception);

        // Act & Assert
        assertThatThrownBy(() -> loggingAspect.logPasswordChangedNotification(joinPoint))
                .isEqualTo(exception);

        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList.getLast().getFormattedMessage())
                .contains("Failed to send password changed notification to", EMAIL);
    }

    // ====================================================================
    // Tests logPasswordResetEmail
    // ====================================================================

    @Test
    @DisplayName("Devrait logger l'envoi d'email de reset")
    void shouldLogPasswordResetEmail_WhenSuccessful() throws Throwable {
        // Arrange
        PasswordResetRequestedEvent event = new PasswordResetRequestedEvent(this, EMAIL, MEMBER_ID);
        when(joinPoint.getArgs()).thenReturn(new Object[]{event});
        when(joinPoint.proceed()).thenReturn(null);

        // Act
        loggingAspect.logPasswordResetEmail(joinPoint);

        // Assert
        verify(joinPoint).proceed();
        
        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList).hasSize(2);
        assertThat(logsList.get(0).getFormattedMessage())
                .contains("Sending password reset email to", EMAIL);
        assertThat(logsList.get(1).getFormattedMessage())
                .contains("Password reset email sent successfully to", EMAIL);
    }

    @Test
    @DisplayName("Devrait logger l'erreur si envoi de reset échoue")
    void shouldLogError_WhenPasswordResetEmailFails() throws Throwable {
        // Arrange
        PasswordResetRequestedEvent event = new PasswordResetRequestedEvent(this, EMAIL, MEMBER_ID);
        RuntimeException exception = new RuntimeException("Service unavailable");
        when(joinPoint.getArgs()).thenReturn(new Object[]{event});
        when(joinPoint.proceed()).thenThrow(exception);

        // Act & Assert
        assertThatThrownBy(() -> loggingAspect.logPasswordResetEmail(joinPoint))
                .isEqualTo(exception);

        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList.getLast().getFormattedMessage())
                .contains("Failed to send password reset email to", EMAIL);
    }

    // ====================================================================
    // Tests logPasswordResetNotification
    // ====================================================================

    @Test
    @DisplayName("Devrait logger confirmation de reset")
    void shouldLogPasswordResetNotification_WhenSuccessful() throws Throwable {
        // Arrange
        PasswordResetEvent event = new PasswordResetEvent(this, EMAIL);
        when(joinPoint.getArgs()).thenReturn(new Object[]{event});
        when(joinPoint.proceed()).thenReturn(null);

        // Act
        loggingAspect.logPasswordResetNotification(joinPoint);

        // Assert
        verify(joinPoint).proceed();
        
        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList).hasSize(2);
        assertThat(logsList.get(0).getFormattedMessage())
                .contains("Sending password reset confirmation to", EMAIL);
        assertThat(logsList.get(1).getFormattedMessage())
                .contains("Password reset confirmation sent successfully to", EMAIL);
    }

    @Test
    @DisplayName("Devrait logger l'erreur si confirmation de reset échoue")
    void shouldLogError_WhenPasswordResetNotificationFails() throws Throwable {
        // Arrange
        PasswordResetEvent event = new PasswordResetEvent(this, EMAIL);
        RuntimeException exception = new RuntimeException("Email error");
        when(joinPoint.getArgs()).thenReturn(new Object[]{event});
        when(joinPoint.proceed()).thenThrow(exception);

        // Act & Assert
        assertThatThrownBy(() -> loggingAspect.logPasswordResetNotification(joinPoint))
                .isEqualTo(exception);

        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList.getLast().getFormattedMessage())
                .contains("Failed to send password reset confirmation to", EMAIL);
    }
}
