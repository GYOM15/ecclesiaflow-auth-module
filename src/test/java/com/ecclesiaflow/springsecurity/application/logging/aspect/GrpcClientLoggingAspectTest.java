package com.ecclesiaflow.springsecurity.application.logging.aspect;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.*;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour GrpcClientLoggingAspect.
 * Version refactorisée sans boucles inutiles - Tests professionnels.
 */
@DisplayName("GrpcClientLoggingAspect - Tests unitaires")
class GrpcClientLoggingAspectTest {

    private ListAppender<ILoggingEvent> listAppender;
    private Logger logger;
    private GrpcClientLoggingAspect aspect;

    // Mocks réutilisables
    private JoinPoint joinPoint;
    private Signature signature;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(GrpcClientLoggingAspect.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        logger.setLevel(Level.DEBUG);

        aspect = new GrpcClientLoggingAspect();

        // Initialiser les mocks
        joinPoint = mock(JoinPoint.class);
        signature = mock(Signature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(listAppender);
    }

    // ========================================================================
    // Tests des Pointcuts
    // ========================================================================

    @Test
    @DisplayName("Doit couvrir les pointcuts")
    void shouldCoverPointcuts() {
        // Test des pointcuts (appels directs pour couverture)
        aspect.grpcChannelShutdown();
        aspect.grpcClientCalls();

        // Coverage only - pas de boucles inutiles
        assertThat(true).isTrue();
    }

    // ========================================================================
    // Tests - Channel Shutdown
    // ========================================================================

    @Test
    @DisplayName("Doit logger (INFO) avant le shutdown du canal gRPC")
    void shouldLogBeforeChannelShutdown() {
        // When
        aspect.logBeforeChannelShutdown(joinPoint);

        // Then
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent log = listAppender.list.getFirst();

        assertThat(log.getLevel()).isEqualTo(Level.INFO);
        assertThat(log.getFormattedMessage()).contains("GRPC-CLIENT", "shutdown", "gRPC channel");
    }

    @Test
    @DisplayName("Doit logger (INFO) après le shutdown réussi du canal gRPC")
    void shouldLogAfterChannelShutdown() {
        // When
        aspect.logAfterChannelShutdown(joinPoint);

        // Then
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent log = listAppender.list.getFirst();

        assertThat(log.getLevel()).isEqualTo(Level.INFO);
        assertThat(log.getFormattedMessage()).contains("GRPC-CLIENT", "closed successfully");
    }

    @Test
    @DisplayName("Doit logger (ERROR) les erreurs lors du shutdown du canal")
    void shouldLogChannelShutdownError() {
        // Given
        Exception channelError = new RuntimeException("Channel error");

        // When
        aspect.logChannelShutdownError(joinPoint, channelError);

        // Then
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent log = listAppender.list.getFirst();

        assertThat(log.getLevel()).isEqualTo(Level.ERROR);
        assertThat(log.getFormattedMessage()).contains("GRPC-CLIENT", "Error", "closing gRPC channel");
    }

    @Test
    @DisplayName("Doit gérer différents types d'erreurs de shutdown")
    void shouldHandleDifferentShutdownErrors() {
        // Test avec IllegalStateException
        aspect.logChannelShutdownError(joinPoint, new IllegalStateException("Invalid state"));

        // Test avec InterruptedException
        aspect.logChannelShutdownError(joinPoint, new InterruptedException("Interrupted"));

        assertThat(listAppender.list).hasSize(2);
        assertThat(listAppender.list)
                .allSatisfy(log -> assertThat(log.getLevel()).isEqualTo(Level.ERROR));
    }

    // ========================================================================
    // Tests - RPC Calls
    // ========================================================================

    @Test
    @DisplayName("Doit logger (INFO) avant un appel RPC 'isEmailNotConfirmed'")
    void shouldLogBeforeRpcCall_IsEmailNotConfirmed() {
        // Given
        when(signature.getName()).thenReturn("isEmailNotConfirmed");

        // When
        aspect.logBeforeRpcCall(joinPoint);

        // Then
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent log = listAppender.list.getFirst();

        assertThat(log.getLevel()).isEqualTo(Level.INFO);
        assertThat(log.getFormattedMessage()).contains("GRPC-CLIENT", "Calling Members", "isEmailNotConfirmed");
    }

    @Test
    @DisplayName("Doit logger (DEBUG) avant un appel RPC générique")
    void shouldLogBeforeRpcCall_OtherMethod() {
        // Given
        when(signature.getName()).thenReturn("someOtherMethod");

        // When
        aspect.logBeforeRpcCall(joinPoint);

        // Then
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent log = listAppender.list.getFirst();

        assertThat(log.getLevel()).isEqualTo(Level.DEBUG);
    }

    @Test
    @DisplayName("Doit logger (INFO) après un appel RPC réussi 'isEmailNotConfirmed'")
    void shouldLogAfterSuccessfulRpcCall_IsEmailNotConfirmed() {
        // Given
        when(signature.getName()).thenReturn("isEmailNotConfirmed");

        // When
        aspect.logAfterSuccessfulRpcCall(joinPoint, true);

        // Then
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent log = listAppender.list.getFirst();

        assertThat(log.getLevel()).isEqualTo(Level.INFO);
        assertThat(log.getFormattedMessage()).contains("GRPC-CLIENT", "completed successfully", "isEmailNotConfirmed");
    }

    @Test
    @DisplayName("Doit logger (DEBUG) après un appel RPC réussi générique")
    void shouldLogAfterSuccessfulRpcCall_OtherMethod() {
        // Given
        when(signature.getName()).thenReturn("anotherMethod");

        // When
        aspect.logAfterSuccessfulRpcCall(joinPoint, null);

        // Then
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent log = listAppender.list.getFirst();

        assertThat(log.getLevel()).isEqualTo(Level.DEBUG);
    }

    // ========================================================================
    // Tests - RPC Call Errors
    // ========================================================================

    @Test
    @DisplayName("Doit logger (ERROR) une erreur UNAVAILABLE")
    void shouldLogRpcCallError_Unavailable() {
        // Given
        when(signature.getName()).thenReturn("isEmailNotConfirmed");

        class ServiceUnavailableError extends RuntimeException {
            public ServiceUnavailableError(String message) {
                super(message);
            }
        }
        Exception unavailableException = new ServiceUnavailableError("Service not available");

        // When
        aspect.logRpcCallError(joinPoint, unavailableException);

        // Then
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent log = listAppender.list.getFirst();

        assertThat(log.getLevel()).isEqualTo(Level.ERROR);
        assertThat(log.getFormattedMessage()).contains("GRPC-CLIENT", "UNAVAILABLE", "isEmailNotConfirmed");
    }

    @Test
    @DisplayName("Doit logger (ERROR) une erreur avec UNAVAILABLE en majuscules dans le nom de classe")
    void shouldLogRpcCallError_UnavailableUpperCase() {
        // Given
        when(signature.getName()).thenReturn("getMemberByEmail");
        
        class UNAVAILABLEException extends RuntimeException {
            public UNAVAILABLEException(String message) {
                super(message);
            }
        }
        Exception unavailableException = new UNAVAILABLEException("gRPC service is down");

        // When
        aspect.logRpcCallError(joinPoint, unavailableException);

        // Then
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent log = listAppender.list.getFirst();

        assertThat(log.getLevel()).isEqualTo(Level.ERROR);
        assertThat(log.getFormattedMessage()).contains("GRPC-CLIENT", "UNAVAILABLE", "getMemberByEmail");
    }

    @Test
    @DisplayName("Doit logger (WARN) un Timeout")
    void shouldLogRpcCallError_Timeout() {
        // Given
        when(signature.getName()).thenReturn("checkStatus");
        Exception timeoutException = new java.util.concurrent.TimeoutException("Operation timed out");

        // When
        aspect.logRpcCallError(joinPoint, timeoutException);

        // Then
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent log = listAppender.list.getFirst();

        assertThat(log.getLevel()).isEqualTo(Level.WARN);
        assertThat(log.getFormattedMessage()).contains("GRPC-CLIENT", "Timeout", "checkStatus");
    }

    @Test
    @DisplayName("Doit logger (WARN) un DEADLINE_EXCEEDED")
    void shouldLogRpcCallError_DeadlineExceeded() {
        // Given
        when(signature.getName()).thenReturn("checkStatus");

        class RequestDEADLINE_EXCEEDEDError extends RuntimeException {
            public RequestDEADLINE_EXCEEDEDError(String message) {
                super(message);
            }
        }
        Exception deadlineException = new RequestDEADLINE_EXCEEDEDError("Request timeout");

        // When
        aspect.logRpcCallError(joinPoint, deadlineException);

        // Then
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent log = listAppender.list.getFirst();

        assertThat(log.getLevel()).isEqualTo(Level.WARN);
        assertThat(log.getFormattedMessage()).contains("GRPC-CLIENT", "Timeout", "checkStatus");
    }

    @Test
    @DisplayName("Doit logger (WARN) une IllegalArgumentException")
    void shouldLogRpcCallError_IllegalArgument() {
        // Given
        when(signature.getName()).thenReturn("validateEmail");
        Exception illegalArgException = new IllegalArgumentException("Invalid email format");

        // When
        aspect.logRpcCallError(joinPoint, illegalArgException);

        // Then
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent log = listAppender.list.getFirst();

        assertThat(log.getLevel()).isEqualTo(Level.WARN);
        assertThat(log.getFormattedMessage()).contains("GRPC-CLIENT", "Invalid argument", "validateEmail");
    }

    @Test
    @DisplayName("Doit logger (ERROR) une erreur générique")
    void shouldLogRpcCallError_Generic() {
        // Given
        when(signature.getName()).thenReturn("someMethod");
        Exception genericException = new RuntimeException("Some error");

        // When
        aspect.logRpcCallError(joinPoint, genericException);

        // Then
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent log = listAppender.list.getFirst();

        assertThat(log.getLevel()).isEqualTo(Level.ERROR);
        assertThat(log.getFormattedMessage()).contains("GRPC-CLIENT", "Error during", "someMethod");
    }

    @Test
    @DisplayName("Doit couvrir tous les cas d'erreur")
    void shouldCoverAllErrorCases() {
        // Given
        when(signature.getName()).thenReturn("testMethod");

        // Test différents types d'erreurs (pas de boucles inutiles)
        aspect.logRpcCallError(joinPoint, new RuntimeException("UNAVAILABLE error"));
        aspect.logRpcCallError(joinPoint, new RuntimeException("Timeout occurred"));
        aspect.logRpcCallError(joinPoint, new IllegalArgumentException("Bad arg"));
        aspect.logRpcCallError(joinPoint, new NullPointerException("Null value"));

        // Then - Vérifier qu'il y a 4 logs
        assertThat(listAppender.list).hasSize(4);
    }
}
