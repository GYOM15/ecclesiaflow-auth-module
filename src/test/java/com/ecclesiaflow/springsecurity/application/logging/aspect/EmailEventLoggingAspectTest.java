package com.ecclesiaflow.springsecurity.application.logging.aspect;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.ecclesiaflow.springsecurity.business.events.PasswordSetEvent;
import org.aspectj.lang.ProceedingJoinPoint;
import com.ecclesiaflow.springsecurity.application.logging.SecurityMaskingUtils;
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
        String masked = SecurityMaskingUtils.maskEmail(EMAIL);
        assertThat(logsList.get(0).getFormattedMessage())
                .contains("[EMAIL] welcome | start | email=" + masked);
        assertThat(logsList.get(1).getFormattedMessage())
                .contains("[EMAIL] welcome | success | email=" + masked);
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

        String masked = SecurityMaskingUtils.maskEmail(EMAIL);
        // Vérifier le premier log (avant l'envoi)
        assertThat(logsList.get(0).getFormattedMessage())
                .contains("[EMAIL] welcome | start | email=" + masked);

        // Vérifier le dernier log (erreur)
        assertThat(logsList.getLast().getFormattedMessage())
                .contains("[EMAIL] welcome | failed | email=" + masked)
                .contains("reason=Email service unavailable");
    }

    @Test
    @DisplayName("Devrait masquer [UNKNOWN] quand l'email est null (welcome)")
    void shouldMaskUnknownWhenEmailNull_Welcome() throws Throwable {
        PasswordSetEvent event = new PasswordSetEvent(this, null);
        when(joinPoint.getArgs()).thenReturn(new Object[]{event});
        when(joinPoint.proceed()).thenReturn(null);

        loggingAspect.logWelcomeEmail(joinPoint);

        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList).hasSize(2);
        assertThat(logsList.get(0).getFormattedMessage())
                .contains("[EMAIL] welcome | start | email=[UNKNOWN]");
        assertThat(logsList.get(1).getFormattedMessage())
                .contains("[EMAIL] welcome | success | email=[UNKNOWN]");
    }

}
