package com.ecclesiaflow.springsecurity.application.logging.aspect;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import nl.altindag.log.LogCaptor;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("AuthenticationErrorLoggingAspect - Tests unitaires")
class AuthenticationErrorLoggingAspectTest {

    private AuthenticationErrorLoggingAspect aspect;

    @Mock private HttpServletRequest mockRequest;
    @Mock private HttpServletResponse mockResponse;
    @Mock private AuthenticationException mockAuthException;
    @Mock private JoinPoint mockJoinPoint;
    @Mock private Signature mockSignature;

    private ListAppender<ILoggingEvent> listAppender;
    private Logger logger;
    private AutoCloseable mocks;
    private LogCaptor logCaptor;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        aspect = new AuthenticationErrorLoggingAspect();

        // Initialize LogCaptor for defensive programming tests
        logCaptor = LogCaptor.forClass(AuthenticationErrorLoggingAspect.class);

        logger = (Logger) LoggerFactory.getLogger(AuthenticationErrorLoggingAspect.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        logger.setLevel(Level.DEBUG);
    }

    @AfterEach
    void tearDown() throws Exception {
        logger.detachAppender(listAppender);
        listAppender.stop();
        mocks.close();
        logCaptor.close();
    }


    // ===========================================
    // TESTS PRINCIPAUX
    // ===========================================

    @Nested
    class AuthenticationErrorLoggingTests {

        @Test
        void shouldLogAuthenticationErrorsWithFullDetails() {
            setupBasicMockRequest("/api/secure", "192.168.1.100", "TestAgent", "Bearer mytoken");
            when(mockAuthException.getMessage()).thenReturn("Erreur JWT");

            JoinPoint joinPoint = createMockJoinPoint(mockRequest, mockResponse, mockAuthException);

            aspect.logAuthenticationError(joinPoint);

            List<ILoggingEvent> logs = listAppender.list;
            assertThat(logs).hasSize(2);

            ILoggingEvent securityLog = logs.getFirst();
            assertThat(securityLog.getLevel()).isEqualTo(Level.WARN);
            assertThat(securityLog.getFormattedMessage()).contains("SECURITY ALERT");

            ILoggingEvent auditLog = logs.get(1);
            assertThat(auditLog.getLevel()).isEqualTo(Level.INFO);
            assertThat(auditLog.getFormattedMessage()).contains("Authentication attempt failed");
        }

        @Test
        void shouldMaskAuthorizationHeader() {
            setupBasicMockRequest("/api/protected", "127.0.0.1", "SomeAgent", "Bearer sensitiveToken");
            when(mockAuthException.getMessage()).thenReturn("Token expiré");

            JoinPoint joinPoint = createMockJoinPoint(mockRequest, mockResponse, mockAuthException);
            aspect.logAuthenticationError(joinPoint);

            String logMsg = listAppender.list.get(0).getFormattedMessage();
            assertThat(logMsg).contains("Auth Header: Bearer ***");
            assertThat(logMsg).doesNotContain("sensitiveToken");
        }

        @Test
        void shouldHandleMissingAuthorizationHeader() {
            setupBasicMockRequest("/api/login", "10.0.0.1", "Agent", null);
            when(mockAuthException.getMessage()).thenReturn("Access denied");

            JoinPoint joinPoint = createMockJoinPoint(mockRequest, mockResponse, mockAuthException);
            aspect.logAuthenticationError(joinPoint);

            String logMsg = listAppender.list.get(0).getFormattedMessage();
            assertThat(logMsg).contains("Auth Header: Absent");
        }

        @Test
        void shouldTruncateLongUserAgent() {
            String longUserAgent = "A".repeat(150);
            setupBasicMockRequest("/api/data", "8.8.8.8", longUserAgent, null);
            when(mockAuthException.getMessage()).thenReturn("UA test");

            JoinPoint joinPoint = createMockJoinPoint(mockRequest, mockResponse, mockAuthException);
            aspect.logAuthenticationError(joinPoint);

            String logMsg = listAppender.list.get(0).getFormattedMessage();
            assertThat(logMsg).contains("User-Agent: " + longUserAgent.substring(0, 100));
        }
    }

    @Nested
    class IPExtractionTests {

        @Test
        void shouldExtractIpFromXForwardedFor() {
            when(mockRequest.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1, 10.0.0.1");
            when(mockRequest.getHeader("X-Real-IP")).thenReturn(null);
            when(mockRequest.getRemoteAddr()).thenReturn("192.168.1.5");
            when(mockRequest.getRequestURI()).thenReturn("/secure");
            when(mockRequest.getHeader("Authorization")).thenReturn(null);
            when(mockAuthException.getMessage()).thenReturn("IP Test");

            JoinPoint joinPoint = createMockJoinPoint(mockRequest, mockResponse, mockAuthException);
            aspect.logAuthenticationError(joinPoint);

            String logMsg = listAppender.list.get(0).getFormattedMessage();
            assertThat(logMsg).contains("IP: 203.0.113.1");
        }

        @Test
        void shouldFallbackToRemoteAddrIfNoHeaders() {
            when(mockRequest.getHeader("X-Forwarded-For")).thenReturn(null);
            when(mockRequest.getHeader("X-Real-IP")).thenReturn(null);
            when(mockRequest.getRemoteAddr()).thenReturn("10.0.0.42");
            when(mockRequest.getRequestURI()).thenReturn("/login");
            when(mockRequest.getHeader("Authorization")).thenReturn(null);
            when(mockAuthException.getMessage()).thenReturn("Fallback IP");

            JoinPoint joinPoint = createMockJoinPoint(mockRequest, mockResponse, mockAuthException);
            aspect.logAuthenticationError(joinPoint);

            String logMsg = listAppender.list.get(0).getFormattedMessage();
            assertThat(logMsg).contains("IP: 10.0.0.42");
        }
    }

    @Nested
    class CriticalErrorLoggingTests {

        @Test
        void shouldLogCriticalExceptionWithStackTrace() {
            Exception ex = new RuntimeException("Critical failure");
            when(mockJoinPoint.getSignature()).thenReturn(mockSignature);
            when(mockSignature.getName()).thenReturn("commence");

            // Tester la méthode qui retourne un booléen au lieu de lever une exception
            boolean result = aspect.performCriticalErrorLogging(mockJoinPoint, ex);

            // Vérifier que l'opération a réussi
            assertThat(result).isTrue();

            ILoggingEvent log = listAppender.list.get(0);
            assertThat(log.getLevel()).isEqualTo(Level.ERROR);
            assertThat(log.getFormattedMessage()).contains("CRITICAL ERROR");
            assertThat(log.getThrowableProxy()).isNotNull();
            assertThat(log.getThrowableProxy().getMessage()).contains("Critical failure");
        }
    }

    @Nested
    class DefensiveProgrammingTests {

        @Test
        void shouldHandleTooFewArguments() {
            // Simule un JoinPoint avec trop peu d'arguments
            JoinPoint joinPoint = mock(JoinPoint.class);
            when(joinPoint.getArgs()).thenReturn(new Object[]{"onlyOneArg"});

            aspect.logAuthenticationError(joinPoint);

            // Vérifie que le message d'ignoré est loggé en DEBUG
            List<String> debugLogs = logCaptor.getDebugLogs();
            assertThat(debugLogs).anyMatch(log -> log.contains("logAuthenticationError ignoré en raison d'arguments invalides"));
        }

        @Test
        void shouldHandleIncorrectArgumentTypes() {
            // Simule un JoinPoint avec des types d'arguments incorrects
            JoinPoint joinPoint = mock(JoinPoint.class);
            when(joinPoint.getArgs()).thenReturn(new Object[]{new Object(), new Object(), new Object()});

            aspect.logAuthenticationError(joinPoint);

            List<String> debugLogs = logCaptor.getDebugLogs();
            assertThat(debugLogs).anyMatch(log -> log.contains("logAuthenticationError ignoré en raison d'arguments invalides"));
        }

        @Test
        void shouldHandleNullArguments() {
            // Simule un JoinPoint avec des arguments null
            JoinPoint joinPoint = mock(JoinPoint.class);
            when(joinPoint.getArgs()).thenReturn(new Object[]{null, null, null});

            aspect.logAuthenticationError(joinPoint);

            List<String> debugLogs = logCaptor.getDebugLogs();
            assertThat(debugLogs).anyMatch(log -> log.contains("logAuthenticationError ignoré en raison d'arguments invalides"));
        }
    }

    @Nested
    class ConcurrencyTests {

        @Test
        void shouldBeThreadSafe() throws InterruptedException {
            int threadCount = 5;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch finishLatch = new CountDownLatch(threadCount);

            setupBasicMockRequest("/api/concurrent", "10.0.0.1", "ConcurrentAgent", "Bearer token123");
            when(mockAuthException.getMessage()).thenReturn("Multi-threaded error");
            JoinPoint joinPoint = createMockJoinPoint(mockRequest, mockResponse, mockAuthException);

            for (int i = 0; i < threadCount; i++) {
                new Thread(() -> {
                    try {
                        startLatch.await();
                        aspect.logAuthenticationError(joinPoint);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        finishLatch.countDown();
                    }
                }).start();
            }

            startLatch.countDown();
            assertThat(finishLatch.await(5, TimeUnit.SECONDS)).isTrue();

            // Expecting 2 logs per thread (WARN + INFO)
            List<ILoggingEvent> logs = listAppender.list;
            assertThat(logs).hasSize(threadCount * 2);

            long warnCount = logs.stream().filter(log -> log.getLevel() == Level.WARN).count();
            long infoCount = logs.stream().filter(log -> log.getLevel() == Level.INFO).count();

            assertThat(warnCount).isEqualTo(threadCount);
            assertThat(infoCount).isEqualTo(threadCount);
        }
    }

    // ===========================================
    // MÉTHODES UTILITAIRES
    // ===========================================

    private void setupBasicMockRequest(String uri, String ip, String userAgent, String authHeader) {
        when(mockRequest.getRequestURI()).thenReturn(uri);
        when(mockRequest.getRemoteAddr()).thenReturn(ip);
        when(mockRequest.getHeader("User-Agent")).thenReturn(userAgent);
        when(mockRequest.getHeader("Authorization")).thenReturn(authHeader);
        when(mockRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(mockRequest.getHeader("X-Real-IP")).thenReturn(null);
    }

    private JoinPoint createMockJoinPoint(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) {
        JoinPoint joinPoint = mock(JoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(new Object[]{request, response, exception});
        return joinPoint;
    }
}


