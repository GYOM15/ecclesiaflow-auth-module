package com.ecclesiaflow.springsecurity.business.aspect;

import com.ecclesiaflow.springsecurity.io.annotation.LogExecution;
import org.aspectj.lang.ProceedingJoinPoint;
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
 * Tests unitaires pour LoggingAspect
 * 
 * Teste le comportement de l'aspect AOP pour le logging automatique
 * des méthodes de services et contrôleurs.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LoggingAspect - Tests unitaires")
class LoggingAspectTest {

    @InjectMocks
    private LoggingAspect loggingAspect;

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private Signature signature;

    @Mock
    private LogExecution logExecution;

    private ListAppender<ILoggingEvent> listAppender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        // Configuration du logger pour capturer les logs dans les tests
        logger = (Logger) LoggerFactory.getLogger(LoggingAspect.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @Test
    @DisplayName("Devrait logger les méthodes de service avec succès")
    void shouldLogServiceMethodsSuccessfully() throws Throwable {
        // Given
        Object mockTarget = new Object();
        when(proceedingJoinPoint.getTarget()).thenReturn(mockTarget);
        when(proceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("testMethod");
        when(proceedingJoinPoint.proceed()).thenReturn("result");

        // When
        Object result = loggingAspect.logServiceMethods(proceedingJoinPoint);

        // Then
        assertThat(result).isEqualTo("result");
        verify(proceedingJoinPoint).proceed();
        verify(proceedingJoinPoint, atLeastOnce()).getTarget();
        verify(proceedingJoinPoint, atLeastOnce()).getSignature();
        verify(signature, atLeastOnce()).getName();
    }

    @Test
    @DisplayName("Devrait logger les exceptions dans les méthodes de service")
    void shouldLogServiceMethodExceptions() throws Throwable {
        // Given
        Object mockTarget = new Object();
        RuntimeException exception = new RuntimeException("Test exception");
        when(proceedingJoinPoint.getTarget()).thenReturn(mockTarget);
        when(proceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("testMethod");
        when(proceedingJoinPoint.proceed()).thenThrow(exception);

        // When & Then
        assertThatThrownBy(() -> loggingAspect.logServiceMethods(proceedingJoinPoint))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Test exception");

        verify(proceedingJoinPoint).proceed();
    }

    @Test
    @DisplayName("Devrait logger les méthodes annotées avec @LogExecution")
    void shouldLogAnnotatedMethods() throws Throwable {
        // Given
        Object mockTarget = new Object();
        when(proceedingJoinPoint.getTarget()).thenReturn(mockTarget);
        when(proceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("annotatedMethod");
        when(proceedingJoinPoint.proceed()).thenReturn("annotated result");
        when(logExecution.value()).thenReturn("Custom message");
        when(logExecution.includeParams()).thenReturn(false);
        when(logExecution.includeExecutionTime()).thenReturn(true);

        // When
        Object result = loggingAspect.logAnnotatedMethods(proceedingJoinPoint, logExecution);

        // Then
        assertThat(result).isEqualTo("annotated result");
        verify(proceedingJoinPoint).proceed();
        verify(logExecution, atLeastOnce()).value();
        verify(logExecution).includeParams();
        verify(logExecution).includeExecutionTime();
    }

    @Test
    @DisplayName("Devrait inclure les paramètres quand demandé dans @LogExecution")
    void shouldIncludeParametersWhenRequested() throws Throwable {
        // Given
        Object mockTarget = new Object();
        Object[] args = {"param1", "param2"};
        when(proceedingJoinPoint.getTarget()).thenReturn(mockTarget);
        when(proceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("methodWithParams");
        when(proceedingJoinPoint.getArgs()).thenReturn(args);
        when(proceedingJoinPoint.proceed()).thenReturn("result with params");
        when(logExecution.value()).thenReturn("");
        when(logExecution.includeParams()).thenReturn(true);
        when(logExecution.includeExecutionTime()).thenReturn(false);

        // When
        Object result = loggingAspect.logAnnotatedMethods(proceedingJoinPoint, logExecution);

        // Then
        assertThat(result).isEqualTo("result with params");
        verify(proceedingJoinPoint).getArgs();
        verify(logExecution).includeParams();
    }

    @Test
    @DisplayName("Devrait logger les exceptions dans les méthodes annotées")
    void shouldLogAnnotatedMethodExceptions() throws Throwable {
        // Given
        Object mockTarget = new Object();
        RuntimeException exception = new RuntimeException("Annotated exception");
        when(proceedingJoinPoint.getTarget()).thenReturn(mockTarget);
        when(proceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("failingMethod");
        when(proceedingJoinPoint.proceed()).thenThrow(exception);
        when(logExecution.value()).thenReturn("Failing operation");
        when(logExecution.includeParams()).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> loggingAspect.logAnnotatedMethods(proceedingJoinPoint, logExecution))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Annotated exception");

        verify(proceedingJoinPoint).proceed();
    }

    @Test
    @DisplayName("Devrait logger l'accès aux contrôleurs")
    void shouldLogControllerAccess() {
        // Given
        Object mockTarget = new Object();
        when(joinPoint.getTarget()).thenReturn(mockTarget);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("controllerMethod");

        // When
        loggingAspect.logControllerAccess(joinPoint);

        // Then
        verify(joinPoint).getTarget();
        verify(joinPoint).getSignature();
        verify(signature).getName();
    }

    @Test
    @DisplayName("Devrait logger les exceptions non gérées")
    void shouldLogUnhandledExceptions() {
        // Given
        Object mockTarget = new Object();
        RuntimeException exception = new RuntimeException("Unhandled exception");
        when(joinPoint.getTarget()).thenReturn(mockTarget);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("problematicMethod");

        // When
        loggingAspect.logUnhandledException(joinPoint, exception);

        // Then
        verify(joinPoint).getTarget();
        verify(joinPoint).getSignature();
        verify(signature).getName();
    }

    @Test
    @DisplayName("Devrait détecter les méthodes lentes (> 1 seconde)")
    void shouldDetectSlowMethods() throws Throwable {
        // Given
        Object mockTarget = new Object();
        when(proceedingJoinPoint.getTarget()).thenReturn(mockTarget);
        when(proceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("slowMethod");
        when(proceedingJoinPoint.proceed()).thenReturn("slow result");

        // When
        Object result = loggingAspect.logServiceMethods(proceedingJoinPoint);

        // Then
        assertThat(result).isEqualTo("slow result");
        verify(proceedingJoinPoint).proceed();
    }

    @Test
    @DisplayName("Devrait utiliser le nom de méthode par défaut quand @LogExecution.value() est vide")
    void shouldUseDefaultMethodNameWhenLogExecutionValueIsEmpty() throws Throwable {
        // Given
        Object mockTarget = new Object();
        when(proceedingJoinPoint.getTarget()).thenReturn(mockTarget);
        when(proceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("defaultNameMethod");
        when(proceedingJoinPoint.proceed()).thenReturn("default result");
        when(logExecution.value()).thenReturn("");
        when(logExecution.includeParams()).thenReturn(false);
        when(logExecution.includeExecutionTime()).thenReturn(true);

        // When
        Object result = loggingAspect.logAnnotatedMethods(proceedingJoinPoint, logExecution);

        // Then
        assertThat(result).isEqualTo("default result");
        verify(logExecution).value();
        verify(proceedingJoinPoint).getTarget();
        verify(signature).getName();
    }
}
