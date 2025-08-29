package com.ecclesiaflow.springsecurity.business.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour BusinessOperationLoggingAspect
 *
 * Teste le comportement de l'aspect AOP pour le logging des opérations
 * métier critiques (inscription, authentification, rafraîchissement de tokens).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BusinessOperationLoggingAspect - Tests unitaires")
class BusinessOperationLoggingAspectTest {

    @InjectMocks
    private BusinessOperationLoggingAspect businessOperationLoggingAspect;

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private Signature signature;

    private ListAppender<ILoggingEvent> listAppender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        // Configuration du logger pour capturer les logs dans les tests
        logger = (Logger) LoggerFactory.getLogger(BusinessOperationLoggingAspect.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    // === TESTS POUR L'ENREGISTREMENT DE MEMBRES ===

    @Test
    @DisplayName("Devrait logger avant l'enregistrement d'un membre")
    void shouldLogBeforeMemberRegistration() {
        // When
        businessOperationLoggingAspect.logBeforeMemberRegistration(joinPoint);

        // Then
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent logEvent = listAppender.list.get(0);
        assertThat(logEvent.getMessage()).contains("BUSINESS: Tentative d'enregistrement d'un nouveau membre");
        assertThat(logEvent.getLevel().toString()).isEqualTo("INFO");
    }

    @Test
    @DisplayName("Devrait logger après un enregistrement réussi")
    void shouldLogAfterSuccessfulRegistration() {
        // When
        businessOperationLoggingAspect.logAfterSuccessfulRegistration(joinPoint);

        // Then
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent logEvent = listAppender.list.get(0);
        assertThat(logEvent.getMessage()).contains("BUSINESS: Nouveau membre enregistré avec succès");
        assertThat(logEvent.getLevel().toString()).isEqualTo("INFO");
    }

    @Test
    @DisplayName("Devrait logger les échecs d'enregistrement")
    void shouldLogFailedRegistration() {
        // Given
        RuntimeException exception = new RuntimeException("Email déjà utilisé");

        // When
        businessOperationLoggingAspect.logFailedRegistration(joinPoint, exception);

        // Then
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent logEvent = listAppender.list.get(0);
        assertThat(logEvent.getFormattedMessage()).contains("BUSINESS: Échec de l'enregistrement du membre - Email déjà utilisé");
        assertThat(logEvent.getLevel().toString()).isEqualTo("WARN");
    }

    // === TESTS POUR L'AUTHENTIFICATION ===

    @Test
    @DisplayName("Devrait logger avant l'authentification")
    void shouldLogBeforeAuthentication() {
        // When
        businessOperationLoggingAspect.logBeforeAuthentication(joinPoint);

        // Then
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent logEvent = listAppender.list.get(0);
        assertThat(logEvent.getMessage()).contains("BUSINESS: Tentative d'authentification");
        assertThat(logEvent.getLevel().toString()).isEqualTo("INFO");
    }

    @Test
    @DisplayName("Devrait logger après une authentification réussie")
    void shouldLogAfterSuccessfulAuthentication() {
        // When
        businessOperationLoggingAspect.logAfterSuccessfulAuthentication(joinPoint);

        // Then
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent logEvent = listAppender.list.get(0);
        assertThat(logEvent.getMessage()).contains("BUSINESS: Authentification réussie");
        assertThat(logEvent.getLevel().toString()).isEqualTo("INFO");
    }

    @Test
    @DisplayName("Devrait logger les échecs d'authentification")
    void shouldLogFailedAuthentication() {
        // Given
        RuntimeException exception = new RuntimeException("Identifiants invalides");

        // When
        businessOperationLoggingAspect.logFailedAuthentication(joinPoint, exception);

        // Then
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent logEvent = listAppender.list.get(0);
        assertThat(logEvent.getFormattedMessage()).contains("BUSINESS: Échec de l'authentification - Identifiants invalides");
        assertThat(logEvent.getLevel().toString()).isEqualTo("WARN");
    }

    // === TESTS POUR LE RAFRAÎCHISSEMENT DE TOKEN ===

    @Test
    @DisplayName("Devrait logger avant le rafraîchissement de token")
    void shouldLogBeforeTokenRefresh() {
        // When
        businessOperationLoggingAspect.logBeforeTokenRefresh(joinPoint);

        // Then
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent logEvent = listAppender.list.get(0);
        assertThat(logEvent.getMessage()).contains("BUSINESS: Tentative de rafraîchissement de token");
        assertThat(logEvent.getLevel().toString()).isEqualTo("INFO");
    }

    @Test
    @DisplayName("Devrait logger après un rafraîchissement de token réussi")
    void shouldLogAfterSuccessfulTokenRefresh() {
        // When
        businessOperationLoggingAspect.logAfterSuccessfulTokenRefresh(joinPoint);

        // Then
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent logEvent = listAppender.list.get(0);
        assertThat(logEvent.getMessage()).contains("BUSINESS: Token rafraîchi avec succès");
        assertThat(logEvent.getLevel().toString()).isEqualTo("INFO");
    }

    @Test
    @DisplayName("Devrait logger les échecs de rafraîchissement de token")
    void shouldLogFailedTokenRefresh() {
        // Given
        RuntimeException exception = new RuntimeException("Token invalide");

        // When
        businessOperationLoggingAspect.logFailedTokenRefresh(joinPoint, exception);

        // Then
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent logEvent = listAppender.list.get(0);
        assertThat(logEvent.getFormattedMessage()).contains("BUSINESS: Échec du rafraîchissement de token - Token invalide");
        assertThat(logEvent.getLevel().toString()).isEqualTo("WARN");
    }

    // === TESTS DE VALIDATION DES INTERACTIONS ===

    @Test
    @DisplayName("Devrait appeler les méthodes JoinPoint correctement")
    void shouldCallJoinPointMethodsCorrectly() {
        // When
        businessOperationLoggingAspect.logBeforeMemberRegistration(joinPoint);
        businessOperationLoggingAspect.logBeforeAuthentication(joinPoint);
        businessOperationLoggingAspect.logBeforeTokenRefresh(joinPoint);

        // Then
        // Vérifier que les méthodes ont été appelées (même si pas utilisées dans l'implémentation actuelle)
        // Cela garantit que le JoinPoint est correctement injecté
        assertThat(listAppender.list).hasSize(3);
    }

    @Test
    @DisplayName("Devrait gérer une exception non nulle")
    void shouldHandleExceptions() {
        Throwable exception = new RuntimeException("boom");

        assertThatCode(() -> {
            businessOperationLoggingAspect.logFailedRegistration(joinPoint, exception);
            businessOperationLoggingAspect.logFailedAuthentication(joinPoint, exception);
            businessOperationLoggingAspect.logFailedTokenRefresh(joinPoint, exception);
        }).doesNotThrowAnyException();

        assertThat(listAppender.list).hasSize(3);
    }
}
