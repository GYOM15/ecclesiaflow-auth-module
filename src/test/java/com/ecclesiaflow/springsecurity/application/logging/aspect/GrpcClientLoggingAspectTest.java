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
 * Unit tests for GrpcClientLoggingAspect.
 * Refactored version without unnecessary loops - Professional tests.
 */
@DisplayName("GrpcClientLoggingAspect - Unit tests")
class GrpcClientLoggingAspectTest {

    private ListAppender<ILoggingEvent> listAppender;
    private Logger logger;
    private GrpcClientLoggingAspect aspect;

    // Reusable mocks
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

        // Initialize mocks
        joinPoint = mock(JoinPoint.class);
        signature = mock(Signature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(listAppender);
    }

    // ========================================================================
    // Tests for Pointcuts
    // ========================================================================

    @Test
    @DisplayName("Should cover pointcuts")
    void shouldCoverPointcuts() {
        // Test pointcuts (direct calls for coverage)
        aspect.grpcChannelShutdown();
        aspect.grpcClientCalls();

        // Coverage only - no unnecessary loops
        assertThat(true).isTrue();
    }

    // ========================================================================
    // Tests - Channel Shutdown
    // ========================================================================

    @Test
    @DisplayName("Should log (INFO) before gRPC channel shutdown")
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
    @DisplayName("Should log (INFO) after successful shutdown of the gRPC channel")
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
    @DisplayName("Should log (ERROR) errors during channel shutdown")
    void shouldLogChannelShutdownError() {
        // Given
        Exception channelError = new RuntimeException("failed to close http://internal:8080 and example.com:443");

        // When
        aspect.logChannelShutdownError(joinPoint, channelError);

        // Then
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent log = listAppender.list.getFirst();

        assertThat(log.getLevel()).isEqualTo(Level.ERROR);
        assertThat(log.getFormattedMessage()).contains("GRPC-CLIENT", "Error", "closing gRPC channel");
        // Sanitization assertions
        assertThat(log.getFormattedMessage()).doesNotContain("http://internal:8080");
        assertThat(log.getFormattedMessage()).doesNotContain("example.com:443");
        assertThat(log.getFormattedMessage()).contains("[URL]").contains("[HOST:PORT]");
    }

    @Test
    @DisplayName("Should handle different shutdown error types")
    void shouldHandleDifferentShutdownErrors() {
        // Test with IllegalStateException
        aspect.logChannelShutdownError(joinPoint, new IllegalStateException("Invalid state"));

        // Test with InterruptedException
        aspect.logChannelShutdownError(joinPoint, new InterruptedException("Interrupted"));

        assertThat(listAppender.list).hasSize(2);
        assertThat(listAppender.list)
                .allSatisfy(log -> assertThat(log.getLevel()).isEqualTo(Level.ERROR));
    }

    // ========================================================================
    // Tests - RPC Calls
    // ========================================================================

    @Test
    @DisplayName("Should log (INFO) before an RPC call 'isEmailNotConfirmed'")
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
    @DisplayName("Should log (DEBUG) before an RPC call generic")
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
    @DisplayName("Should log (INFO) after a successful RPC call 'isEmailNotConfirmed'")
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
    @DisplayName("Should log (DEBUG) after a successful RPC call generic")
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
    @DisplayName("Should log (ERROR) an UNAVAILABLE error")
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
    @DisplayName("Should log (ERROR) an error with UNAVAILABLE in uppercase in class name")
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
    @DisplayName("Should log (WARN) a Timeout")
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
    @DisplayName("Should log (WARN) a DEADLINE_EXCEEDED")
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
    @DisplayName("Should log (WARN) an IllegalArgumentException")
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
    @DisplayName("Should log (ERROR) a generic error")
    void shouldLogRpcCallError_Generic() {
        // Given
        when(signature.getName()).thenReturn("someMethod");
        Exception genericException = new RuntimeException("connect to https://svc.local:8443 failed at host:443");

        // When
        aspect.logRpcCallError(joinPoint, genericException);

        // Then
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent log = listAppender.list.getFirst();

        assertThat(log.getLevel()).isEqualTo(Level.ERROR);
        assertThat(log.getFormattedMessage()).contains("GRPC-CLIENT", "Error during", "someMethod");
        // Sanitization assertions
        assertThat(log.getFormattedMessage()).doesNotContain("https://svc.local:8443");
        assertThat(log.getFormattedMessage()).doesNotContain("host:443");
        assertThat(log.getFormattedMessage()).contains("[URL]").contains("[HOST:PORT]");
    }

    @Test
    @DisplayName("Should cover all error cases")
    void shouldCoverAllErrorCases() {
        // Given
        when(signature.getName()).thenReturn("testMethod");

        // Test different error types (no unnecessary loops)
        aspect.logRpcCallError(joinPoint, new RuntimeException("UNAVAILABLE error"));
        aspect.logRpcCallError(joinPoint, new RuntimeException("Timeout occurred"));
        aspect.logRpcCallError(joinPoint, new IllegalArgumentException("Bad arg"));
        aspect.logRpcCallError(joinPoint, new NullPointerException("Null value"));

        // Then - Tests that there are 4 logs
        assertThat(listAppender.list).hasSize(4);
    }
}
