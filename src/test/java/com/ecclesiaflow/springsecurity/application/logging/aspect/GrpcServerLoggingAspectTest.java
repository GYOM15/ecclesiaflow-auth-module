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
 * Tests unitaires pour GrpcServerLoggingAspect.
 * Version refactorisée sans boucles inutiles et avec tests paramétrés.
 */
@DisplayName("GrpcServerLoggingAspect - Tests unitaires")
class GrpcServerLoggingAspectTest {

    private ListAppender<ILoggingEvent> listAppender;
    private Logger logger;
    private GrpcServerLoggingAspect aspect;

    // Mocks réutilisables
    private JoinPoint joinPoint;
    private Signature signature;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(GrpcServerLoggingAspect.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        logger.setLevel(Level.DEBUG); // Capturer tous les niveaux

        aspect = new GrpcServerLoggingAspect();

        // Initialiser les mocks
        joinPoint = mock(JoinPoint.class);
        signature = mock(Signature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{}); // Comportement par défaut
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(listAppender);
    }

    // ========================================================================
    // Tests - Server Start/Stop
    // ========================================================================

    @Test
    @DisplayName("Doit logger (INFO) avant le démarrage du serveur gRPC")
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
    @DisplayName("Doit logger (INFO) après le démarrage réussi du serveur")
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
    @DisplayName("Doit logger (ERROR) les erreurs de démarrage du serveur")
    void shouldLogServerStartError() {
        // Given
        Exception ex = new RuntimeException("Port already in use");

        // When
        aspect.logServerStartError(joinPoint, ex);

        // Then
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent log = listAppender.list.getFirst();

        assertThat(log.getLevel()).isEqualTo(Level.ERROR);
        assertThat(log.getFormattedMessage()).contains("GRPC", "Failed to start gRPC server");
        // Vérifier que l'exception est bien attachée au log
        assertThat(log.getThrowableProxy()).isNotNull();
        assertThat(((ThrowableProxy) log.getThrowableProxy()).getThrowable()).isSameAs(ex);
    }

    @Test
    @DisplayName("Doit logger (INFO) avant l'arrêt du serveur gRPC")
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
    @DisplayName("Doit logger (INFO) après l'arrêt réussi du serveur")
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
    @DisplayName("Doit logger (INFO) avant un appel RPC 'generateTemporaryToken'")
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
    @DisplayName("Doit logger (DEBUG) avant l'appel pour les méthodes génériques")
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
    @DisplayName("Doit logger (DEBUG) avant un appel RPC 'validateToken'")
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
    @DisplayName("Doit logger (INFO) après un appel RPC 'generateTemporaryToken' réussi")
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
    @DisplayName("Doit logger (DEBUG) après un appel RPC 'validateToken' réussi")
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
    @DisplayName("Doit logger (WARN) une IllegalArgumentException")
    void shouldLogRpcCallError_IllegalArgument() {
        // Given
        when(signature.getName()).thenReturn("generateTemporaryToken");
        Exception illegalArgException = new IllegalArgumentException("Invalid UUID format");

        // When
        aspect.logRpcCallError(joinPoint, illegalArgException);

        // Then
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent log = listAppender.list.getFirst();

        assertThat(log.getLevel()).isEqualTo(Level.WARN);
        assertThat(log.getFormattedMessage()).contains("GRPC-RPC", "Invalid argument", "generateTemporaryToken");
        assertThat(log.getThrowableProxy()).isNotNull();
    }

    @Test
    @DisplayName("Doit logger (ERROR) une erreur interne (RuntimeException)")
    void shouldLogRpcCallError_InternalError() {
        // Given
        when(signature.getName()).thenReturn("generateTemporaryToken");
        Exception internalException = new RuntimeException("Database connection failed");

        // When
        aspect.logRpcCallError(joinPoint, internalException);

        // Then
        assertThat(listAppender.list).hasSize(1);
        ILoggingEvent log = listAppender.list.getFirst()
                ;

        assertThat(log.getLevel()).isEqualTo(Level.ERROR);
        assertThat(log.getFormattedMessage()).contains("GRPC-RPC", "Error in", "generateTemporaryToken");
        assertThat(log.getThrowableProxy()).isNotNull();
    }

    // ========================================================================
    // Tests - Méthodes privées utilitaires (via Réflexion)
    // ========================================================================

    @DisplayName("Doit sanitizer les emails")
    @ParameterizedTest(name = "Input: \"{0}\" -> Output: \"{1}\"")
    @CsvSource({
            "john.doe@example.com,   jo***e@example.com",
            "test@example.com,     te***t@example.com",
            "abc@test.com,         ***@test.com",
            "ab@test.com,          ***@test.com",
            "a@b.com,              ***@b.com"
    })
    void shouldSanitizeValidEmails(String input, String expected) throws Exception {
        // Given
        Method sanitizeEmailMethod = GrpcServerLoggingAspect.class.getDeclaredMethod("sanitizeEmail", String.class);
        sanitizeEmailMethod.setAccessible(true);

        // When
        String result = (String) sanitizeEmailMethod.invoke(aspect, input);

        // Then
        assertThat(result).isEqualTo(expected);
    }

    @DisplayName("Doit retourner '***' pour les emails invalides ou null")
    @ParameterizedTest
    @ValueSource(strings = {"invalidemail", ""})
    void shouldSanitizeInvalidOrNullEmails(String input) throws Exception {
        // Given
        Method sanitizeEmailMethod = GrpcServerLoggingAspect.class.getDeclaredMethod("sanitizeEmail", String.class);
        sanitizeEmailMethod.setAccessible(true);

        // When
        String result = (String) sanitizeEmailMethod.invoke(aspect, input);

        // Then
        assertThat(result).isEqualTo("***");
    }

    @Test
    @DisplayName("Doit sanitizer un email null")
    void shouldSanitizeNullEmail() throws Exception {
        // Given
        Method sanitizeEmailMethod = GrpcServerLoggingAspect.class.getDeclaredMethod("sanitizeEmail", String.class);
        sanitizeEmailMethod.setAccessible(true);

        // When
        String result = (String) sanitizeEmailMethod.invoke(aspect, (String) null);

        // Then
        assertThat(result).isEqualTo("***");
    }


    @DisplayName("Doit sanitizer les tokens")
    @ParameterizedTest(name = "Input: \"{0}\" -> Output: \"{1}\"")
    @CsvSource({
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0,   eyJhbG...***"
            // Ajoutez d'autres exemples de tokens longs si nécessaire
    })
    void shouldSanitizeValidTokens(String input, String expected) throws Exception {
        // Given
        Method sanitizeTokenMethod = GrpcServerLoggingAspect.class.getDeclaredMethod("sanitizeToken", String.class);
        sanitizeTokenMethod.setAccessible(true);

        // When
        String result = (String) sanitizeTokenMethod.invoke(aspect, input);

        // Then
        assertThat(result).isEqualTo(expected);
    }

    @DisplayName("Doit retourner '***' pour les tokens courts ou null")
    @ParameterizedTest
    @ValueSource(strings = {"abc", "1234567", "short", ""})
    void shouldSanitizeShortOrNullTokens(String input) throws Exception {
        // Given
        Method sanitizeTokenMethod = GrpcServerLoggingAspect.class.getDeclaredMethod("sanitizeToken", String.class);
        sanitizeTokenMethod.setAccessible(true);

        // When
        String result = (String) sanitizeTokenMethod.invoke(aspect, input);

        // Then
        assertThat(result).isEqualTo("***");
    }

    @Test
    @DisplayName("Doit sanitizer un token null")
    void shouldSanitizeNullToken() throws Exception {
        // Given
        Method sanitizeTokenMethod = GrpcServerLoggingAspect.class.getDeclaredMethod("sanitizeToken", String.class);
        sanitizeTokenMethod.setAccessible(true);

        // When
        String result = (String) sanitizeTokenMethod.invoke(aspect, (String) null);

        // Then
        assertThat(result).isEqualTo("***");
    }

    // ========================================================================
    // Tests - Pointcuts Coverage
    // ========================================================================

    @Test
    @DisplayName("Doit couvrir le pointcut grpcServerStart")
    void shouldCoverGrpcServerStartPointcut() {
        // Given / When
        aspect.grpcServerStart();

        // Then - Pas d'assertion nécessaire, juste couvrir le code du pointcut
        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("Doit couvrir le pointcut grpcServerStop")
    void shouldCoverGrpcServerStopPointcut() {
        // Given / When
        aspect.grpcServerStop();

        // Then - Pas d'assertion nécessaire, juste couvrir le code du pointcut
        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("Doit couvrir le pointcut grpcServiceCalls")
    void shouldCoverGrpcServiceCallsPointcut() {
        // Given / When
        aspect.grpcServiceCalls();

        // Then - Pas d'assertion nécessaire, juste couvrir le code du pointcut
        assertThat(true).isTrue();
    }
}