package com.ecclesiaflow.springsecurity.application.logging.aspect;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for GrpcServerLoggingAspect.
 * Refactored version without unnecessary loops and with parameterized tests.
 */
@DisplayName("GrpcServerLoggingAspect - Unit tests")
class GrpcServerLoggingAspectTest {

    private ListAppender<ILoggingEvent> listAppender;
    private Logger logger;
    private GrpcServerLoggingAspect aspect;

    // Reusable mocks
    private JoinPoint joinPoint;
    private Signature signature;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(GrpcServerLoggingAspect.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        logger.setLevel(Level.DEBUG); // Capture all levels

        aspect = new GrpcServerLoggingAspect();

        // Initialize mocks
        joinPoint = mock(JoinPoint.class);
        signature = mock(Signature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{}); // Default behavior
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(listAppender);
    }

    // ========================================================================
    // Tests - Server Start/Stop
    // ========================================================================

    @Test
    @DisplayName("Should log (INFO) before startup of the gRPC server")
    void shouldLogBeforeServerStart() {
        // When
        aspect.logBeforeServerStart(joinPoint);

        // Then
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent log = listAppender.list.getFirst();

        assertThat(log.getLevel()).isEqualTo(Level.INFO);
        assertThat(log.getFormattedMessage()).contains("GRPC", "Initializing gRPC server");
    }

    @Test
    @DisplayName("Should log (INFO) after successful server startup")
    void shouldLogAfterServerStart() {
        // When
        aspect.logAfterServerStart(joinPoint);

        // Then
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent log = listAppender.list.getFirst();

        assertThat(log.getLevel()).isEqualTo(Level.INFO);
        assertThat(log.getFormattedMessage()).contains("GRPC", "server started successfully");
    }

    @Test
    @DisplayName("Should log (ERROR) server startup errors")
    void shouldLogServerStartError() {
        // Given
        Exception ex = new RuntimeException("bind to https://server:7443 and server.local:7443 failed");

        // When
        aspect.logServerStartError(joinPoint, ex);

        // Then
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent log = listAppender.list.getFirst();

        assertThat(log.getLevel()).isEqualTo(Level.ERROR);
        assertThat(log.getFormattedMessage()).contains("GRPC", "Failed to start gRPC server");
        // Tests that the exception is properly attached to the log
        assertThat(log.getThrowableProxy()).isNotNull();
        assertThat(((ThrowableProxy) log.getThrowableProxy()).getThrowable()).isSameAs(ex);
        // Sanitization assertions
        assertThat(log.getFormattedMessage()).doesNotContain("https://server:7443");
        assertThat(log.getFormattedMessage()).doesNotContain("server.local:7443");
        assertThat(log.getFormattedMessage()).contains("[URL]").contains("[HOST:PORT]");
    }

    @Test
    @DisplayName("Should log (INFO) before gRPC server shutdown")
    void shouldLogBeforeServerStop() {
        // When
        aspect.logBeforeServerStop(joinPoint);

        // Then
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent log = listAppender.list.getFirst();

        assertThat(log.getLevel()).isEqualTo(Level.INFO);
        assertThat(log.getFormattedMessage()).contains("GRPC", "graceful shutdown");
    }

    @Test
    @DisplayName("Should log (INFO) after successful server shutdown")
    void shouldLogAfterServerStop() {
        // When
        aspect.logAfterServerStop(joinPoint);

        // Then
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent log = listAppender.list.getFirst();

        assertThat(log.getLevel()).isEqualTo(Level.INFO);
        assertThat(log.getFormattedMessage()).contains("GRPC", "server stopped successfully");
    }

    // ========================================================================
    // Tests - RPC Service Calls
    // ========================================================================

    @Test
    @DisplayName("Should log (INFO) before an RPC call 'generateTemporaryToken'")
    void shouldLogBeforeRpcCall_GenerateTemporaryToken() {
        // Given
        when(signature.getName()).thenReturn("generateTemporaryToken");

        // When
        aspect.logBeforeRpcCall(joinPoint);

        // Then
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent log = listAppender.list.getFirst();

        assertThat(log.getLevel()).isEqualTo(Level.INFO);
        assertThat(log.getFormattedMessage()).contains("GRPC-RPC", "Received", "generateTemporaryToken", "Members module");
    }

    @Test
    @DisplayName("Should log (DEBUG) before call for generic methods")
    void shouldLogBeforeRpcCall_GenericMethod() {
        // Given
        when(signature.getName()).thenReturn("someMethodWithEmail");
        when(joinPoint.getArgs()).thenReturn(new Object[]{"ID-123", "johndoe.user@example.com", "other-data"});

        // When
        aspect.logBeforeRpcCall(joinPoint);

        // Then
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent log = listAppender.list.getFirst();
        
        assertThat(log.getLevel()).isEqualTo(Level.DEBUG);
        assertThat(log.getFormattedMessage()).contains("GRPC-RPC", "Received", "someMethodWithEmail");
    }

    @Test
    @DisplayName("Should log (DEBUG) before an RPC call 'validateToken'")
    void shouldLogBeforeRpcCall_ValidateToken() {
        // Given
        when(signature.getName()).thenReturn("validateToken");

        // When
        aspect.logBeforeRpcCall(joinPoint);

        // Then
        assertThat(listAppender.list).hasSize(1);
        assertThat(listAppender.list.getFirst().getLevel()).isEqualTo(Level.DEBUG);
    }

    @Test
    @DisplayName("Should log (INFO) after successful 'generateTemporaryToken' RPC call")
    void shouldLogAfterSuccessfulRpcCall_GenerateTemporaryToken() {
        // Given
        when(signature.getName()).thenReturn("generateTemporaryToken");

        // When
        aspect.logAfterSuccessfulRpcCall(joinPoint);

        // Then
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent log = listAppender.list.getFirst();

        assertThat(log.getLevel()).isEqualTo(Level.INFO);
        assertThat(log.getFormattedMessage()).contains("GRPC-RPC", "completed successfully", "generateTemporaryToken");
    }

    @Test
    @DisplayName("Should log (DEBUG) after successful 'validateToken' RPC call")
    void shouldLogAfterSuccessfulRpcCall_OtherMethod() {
        // Given
        when(signature.getName()).thenReturn("validateToken");

        // When
        aspect.logAfterSuccessfulRpcCall(joinPoint);

        // Then
        assertThat(listAppender.list).hasSize(1);
        assertThat(listAppender.list.getFirst().getLevel()).isEqualTo(Level.DEBUG);
    }

    // ========================================================================
    // Tests - RPC Call Errors
    // ========================================================================

    @Test
    @DisplayName("Should log (WARN) an IllegalArgumentException")
    void shouldLogRpcCallError_IllegalArgument() {
        // Given
        when(signature.getName()).thenReturn("generateTemporaryToken");
        Exception illegalArgException = new IllegalArgumentException("invalid host example.com:1234");

        // When
        aspect.logRpcCallError(joinPoint, illegalArgException);

        // Then
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent log = listAppender.list.getFirst();

        assertThat(log.getLevel()).isEqualTo(Level.WARN);
        assertThat(log.getFormattedMessage()).contains("GRPC-RPC", "Invalid argument", "generateTemporaryToken");
        // Sanitization assertions
        assertThat(log.getFormattedMessage()).doesNotContain("example.com:1234");
        assertThat(log.getFormattedMessage()).contains("[HOST:PORT]");
        assertThat(log.getThrowableProxy()).isNotNull();
    }

    @Test
    @DisplayName("Should log (ERROR) an internal error (RuntimeException)")
    void shouldLogRpcCallError_InternalError() {
        // Given
        when(signature.getName()).thenReturn("generateTemporaryToken");
        Exception internalException = new RuntimeException("timeout at https://x:9999");

        // When
        aspect.logRpcCallError(joinPoint, internalException);

        // Then
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent log = listAppender.list.getFirst()
                ;

        assertThat(log.getLevel()).isEqualTo(Level.ERROR);
        assertThat(log.getFormattedMessage()).contains("GRPC-RPC", "Error in", "generateTemporaryToken");
        assertThat(log.getThrowableProxy()).isNotNull();
        // Sanitization assertions
        assertThat(log.getFormattedMessage()).doesNotContain("https://x:9999");
        assertThat(log.getFormattedMessage()).contains("[URL]");
    }

    // ========================================================================
    // Tests - Private utility methods (via Reflection)
    // ========================================================================

    // ========================================================================
    // Tests - Pointcuts Coverage
    // ========================================================================

    @Test
    @DisplayName("Should cover the grpcServerStart pointcut")
    void shouldCoverGrpcServerStartPointcut() {
        // Given / When
        aspect.grpcServerStart();

        // Then - No assertion needed, just cover the pointcut code
        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("Should cover the grpcServerStop pointcut")
    void shouldCoverGrpcServerStopPointcut() {
        // Given / When
        aspect.grpcServerStop();

        // Then - No assertion needed, just cover the pointcut code
        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("Should cover the grpcServiceCalls pointcut")
    void shouldCoverGrpcServiceCallsPointcut() {
        // Given / When
        aspect.grpcServiceCalls();

        // Then - No assertion needed, just cover the pointcut code
        assertThat(true).isTrue();
    }
}