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
import java.lang.reflect.Field;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;

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

    @Nested
    class AuthenticationErrorLoggingTests {
        @Test
        void shouldHandleInvalidArgumentsCount() {
            // Given - Moins de 3 arguments
            JoinPoint joinPoint = mock(JoinPoint.class);
            when(joinPoint.getArgs()).thenReturn(new Object[]{mock(HttpServletRequest.class), "not a response"});

            // When
            aspect.performAuthenticationErrorLogging(joinPoint);

            // Then - Devrait logger un message de débogage
            List<ILoggingEvent> logs = listAppender.list;
            assertThat(logs).hasSize(1);
            assertThat(logs.get(0).getFormattedMessage())
                .contains("logAuthenticationError ignoré en raison d'arguments invalides");
        }

        @Test
        void shouldHandleInvalidArgumentTypes() {
            // Given - Types d'arguments incorrects (mauvaise combinaison request/exception)
            JoinPoint joinPoint = mock(JoinPoint.class);
            when(joinPoint.getArgs()).thenReturn(new Object[]{
                "not a request",
                mock(HttpServletResponse.class),
                new RuntimeException("Not an AuthenticationException")
            });

            // When
            aspect.performAuthenticationErrorLogging(joinPoint);

            // Then - Devrait logger un message de débogage
            List<ILoggingEvent> logs = listAppender.list;
            assertThat(logs).hasSize(1);
            assertThat(logs.get(0).getFormattedMessage())
                .contains("logAuthenticationError ignoré en raison d'arguments invalides");
        }

        @Test
        void shouldHandleMissingAuthenticationExceptionArgument() {
            setupBasicMockRequest("/api/login", "127.0.0.1", "Agent", "Bearer token");

            JoinPoint joinPoint = mock(JoinPoint.class);
            when(joinPoint.getArgs()).thenReturn(new Object[]{mockRequest, mock(HttpServletResponse.class), null});

            aspect.performAuthenticationErrorLogging(joinPoint);

            List<ILoggingEvent> logs = listAppender.list;
            assertThat(logs).hasSize(1);
            assertThat(logs.get(0).getFormattedMessage())
                    .contains("logAuthenticationError ignoré en raison d'arguments invalides");
        }

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

        @Test
        void shouldHandleNullUserAgent() {
            setupBasicMockRequest("/api/login", "10.0.0.1", null, null);
            when(mockAuthException.getMessage()).thenReturn("Access denied");

            JoinPoint joinPoint = createMockJoinPoint(mockRequest, mockResponse, mockAuthException);
            aspect.logAuthenticationError(joinPoint);

            String logMsg = listAppender.list.get(0).getFormattedMessage();
            assertThat(logMsg).contains("User-Agent: N/A");
        }

        @Test
        void shouldHandleExceptionDuringLogging() {
            // Simuler une exception lors de l'accès aux propriétés de la requête
            HttpServletRequest faultyRequest = mock(HttpServletRequest.class);
            when(faultyRequest.getRequestURI()).thenThrow(new RuntimeException("Request error"));

            JoinPoint joinPoint = createMockJoinPoint(faultyRequest, mockResponse, mockAuthException);

            // Ne devrait pas lever d'exception
            assertThatCode(() -> aspect.logAuthenticationError(joinPoint))
                    .doesNotThrowAnyException();

            List<String> debugLogs = logCaptor.getDebugLogs();
            assertThat(debugLogs).anyMatch(log -> log.contains("Erreur lors du logging d'authentification"));
        }
        
        @Test
        void shouldHandleExceptionInGetClientIpAddress() {
            // Given - Requête qui lève une exception lors de l'extraction de l'IP
            HttpServletRequest faultyRequest = mock(HttpServletRequest.class);
            when(faultyRequest.getHeader("X-Forwarded-For")).thenThrow(new RuntimeException("Header error"));
            
            JoinPoint joinPoint = createMockJoinPoint(faultyRequest, mockResponse, mockAuthException);

            // When - Ne devrait pas lever d'exception
            assertThatCode(() -> aspect.performAuthenticationErrorLogging(joinPoint))
                    .doesNotThrowAnyException();

            // Then - Devrait avoir un log de débogage sur l'erreur
            List<String> debugLogs = logCaptor.getDebugLogs();
            assertThat(debugLogs).anyMatch(log -> log.contains("Erreur lors du logging d'authentification"));
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
        void shouldExtractIpFromXRealIP() {
            when(mockRequest.getHeader("X-Forwarded-For")).thenReturn(null);
            when(mockRequest.getHeader("X-Real-IP")).thenReturn("198.51.100.1");
            when(mockRequest.getRemoteAddr()).thenReturn("192.168.1.5");

            String clientIp = aspect.getClientIpAddress(mockRequest);
            assertThat(clientIp).isEqualTo("198.51.100.1");
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

        @Test
        void shouldHandleEmptyXForwardedFor() {
            when(mockRequest.getHeader("X-Forwarded-For")).thenReturn("");
            when(mockRequest.getHeader("X-Real-IP")).thenReturn(null);
            when(mockRequest.getRemoteAddr()).thenReturn("10.0.0.50");

            String clientIp = aspect.getClientIpAddress(mockRequest);
            assertThat(clientIp).isEqualTo("10.0.0.50");
        }

        @Test
        void shouldHandleUnknownXForwardedFor() {
            when(mockRequest.getHeader("X-Forwarded-For")).thenReturn("unknown");
            when(mockRequest.getHeader("X-Real-IP")).thenReturn(null);
            when(mockRequest.getRemoteAddr()).thenReturn("10.0.0.60");

            String clientIp = aspect.getClientIpAddress(mockRequest);
            assertThat(clientIp).isEqualTo("10.0.0.60");
        }

        @Test
        void shouldHandleEmptyXRealIP() {
            when(mockRequest.getHeader("X-Forwarded-For")).thenReturn(null);
            when(mockRequest.getHeader("X-Real-IP")).thenReturn("");
            when(mockRequest.getRemoteAddr()).thenReturn("10.0.0.70");

            String clientIp = aspect.getClientIpAddress(mockRequest);
            assertThat(clientIp).isEqualTo("10.0.0.70");
        }

        @Test
        void shouldHandleUnknownXRealIP() {
            when(mockRequest.getHeader("X-Forwarded-For")).thenReturn(null);
            when(mockRequest.getHeader("X-Real-IP")).thenReturn("unknown");
            when(mockRequest.getRemoteAddr()).thenReturn("10.0.0.80");

            String clientIp = aspect.getClientIpAddress(mockRequest);
            assertThat(clientIp).isEqualTo("10.0.0.80");
        }
    }

    @Nested
    class CriticalErrorLoggingTests {

        @Test
        void shouldLogCriticalExceptionWithStackTrace() {
            Exception ex = new RuntimeException("Critical failure");
            when(mockJoinPoint.getSignature()).thenReturn(mockSignature);
            when(mockSignature.getName()).thenReturn("commence");

            boolean result = aspect.performCriticalErrorLogging(mockJoinPoint, ex);

            assertThat(result).isTrue();

            ILoggingEvent log = listAppender.list.get(0);
            assertThat(log.getLevel()).isEqualTo(Level.ERROR);
            assertThat(log.getFormattedMessage()).contains("CRITICAL ERROR");
            assertThat(log.getThrowableProxy()).isNotNull();
            assertThat(log.getThrowableProxy().getMessage()).contains("Critical failure");
        }

        @Test
        void shouldHandleExceptionDuringCriticalErrorLogging() {
            Exception ex = new RuntimeException("Critical failure");
            JoinPoint faultyJoinPoint = mock(JoinPoint.class);
            Signature faultySignature = mock(Signature.class);

            when(faultyJoinPoint.getSignature()).thenReturn(faultySignature);
            when(faultySignature.getName()).thenThrow(new RuntimeException("Signature error"));

            // La méthode devrait retourner false en cas d'erreur
            boolean result = aspect.performCriticalErrorLogging(faultyJoinPoint, ex);
            assertThat(result).isFalse();
        }

        @Test
        void shouldHandleNullJoinPointDuringCriticalLogging() {
            Exception ex = new RuntimeException("Null join point");

            boolean result = aspect.performCriticalErrorLogging(null, ex);

            assertThat(result).isTrue();
            ILoggingEvent log = listAppender.list.get(0);
            assertThat(log.getFormattedMessage())
                    .contains("CRITICAL ERROR - Erreur critique dans CustomAuthenticationEntryPoint")
                    .contains("Null join point")
                    .contains("méthode inconnue");
        }

        @Test
        void shouldHandleNullExceptionDuringCriticalLogging() {
            when(mockJoinPoint.getSignature()).thenReturn(mockSignature);
            when(mockSignature.getName()).thenReturn("commence");

            boolean result = aspect.performCriticalErrorLogging(mockJoinPoint, null);

            assertThat(result).isTrue();
            ILoggingEvent log = listAppender.list.get(0);
            assertThat(log.getFormattedMessage())
                    .contains("CRITICAL ERROR - Erreur critique dans CustomAuthenticationEntryPoint")
                    .contains("Aucun message d'erreur disponible")
                    .contains("commence");
        }
    }

    @Nested
    class DefensiveProgrammingTests {

        @Test
        void shouldHandleTooFewArguments() {
            JoinPoint joinPoint = mock(JoinPoint.class);
            when(joinPoint.getArgs()).thenReturn(new Object[]{"onlyOneArg"});

            aspect.logAuthenticationError(joinPoint);

            List<String> debugLogs = logCaptor.getDebugLogs();
            assertThat(debugLogs).anyMatch(log -> log.contains("logAuthenticationError ignoré en raison d'arguments invalides"));
        }

        @Test
        void shouldHandleIncorrectArgumentTypes() {
            JoinPoint joinPoint = mock(JoinPoint.class);
            when(joinPoint.getArgs()).thenReturn(new Object[]{new Object(), new Object(), new Object()});

            aspect.logAuthenticationError(joinPoint);

            List<String> debugLogs = logCaptor.getDebugLogs();
            assertThat(debugLogs).anyMatch(log -> log.contains("logAuthenticationError ignoré en raison d'arguments invalides"));
        }

        @Test
        void shouldHandleNullArguments() {
            JoinPoint joinPoint = mock(JoinPoint.class);
            when(joinPoint.getArgs()).thenReturn(new Object[]{null, null, null});

            aspect.logAuthenticationError(joinPoint);

            List<String> debugLogs = logCaptor.getDebugLogs();
            assertThat(debugLogs).anyMatch(log -> log.contains("logAuthenticationError ignoré en raison d'arguments invalides"));
        }

        @Test
        void shouldHandleEmptyArguments() {
            JoinPoint joinPoint = mock(JoinPoint.class);
            when(joinPoint.getArgs()).thenReturn(new Object[]{});

            aspect.logAuthenticationError(joinPoint);

            List<String> debugLogs = logCaptor.getDebugLogs();
            assertThat(debugLogs).anyMatch(log -> log.contains("logAuthenticationError ignoré en raison d'arguments invalides"));
        }
    }

    @Nested
    class AuthenticationEntryPointExceptionTests {
        
        @Test
        void shouldLogCriticalErrorWhenExceptionThrownInEntryPoint() {
            // Given
            when(mockJoinPoint.getSignature()).thenReturn(mockSignature);
            when(mockSignature.getName()).thenReturn("someMethod");
            Exception testException = new RuntimeException("Test critical error");

            // When
            aspect.logAuthenticationEntryPointException(mockJoinPoint, testException);

            // Then - Vérifie qu'une erreur critique est bien loggée
            List<ILoggingEvent> logs = listAppender.list;
            assertThat(logs).hasSize(1);
            assertThat(logs.get(0).getFormattedMessage())
                .contains("CRITICAL ERROR - Erreur critique dans CustomAuthenticationEntryPoint")
                .contains("Test critical error")
                .contains("someMethod");
        }
        
        @Test
        void shouldHandleNullExceptionInLogAuthenticationEntryPoint() {
            // Given
            when(mockJoinPoint.getSignature()).thenReturn(mockSignature);
            when(mockSignature.getName()).thenReturn("testMethod");

            // When - Appel avec exception null
            aspect.logAuthenticationEntryPointException(mockJoinPoint, null);

            // Then - Vérifie que la méthode gère le cas null
            List<ILoggingEvent> logs = listAppender.list;
            assertThat(logs).hasSize(1);
            assertThat(logs.get(0).getFormattedMessage())
                .contains("CRITICAL ERROR - Erreur critique dans CustomAuthenticationEntryPoint")
                .contains("Exception nulle interceptée");
        }
    }
    
    @Nested
    class PerformCriticalErrorLoggingTests {
        
        @Test
        void shouldReturnTrueWhenLoggingSucceeds() {
            // Given
            when(mockJoinPoint.getSignature()).thenReturn(mockSignature);
            when(mockSignature.getName()).thenReturn("testMethod");
            Exception testException = new RuntimeException("Test error");

            // When
            boolean result = aspect.performCriticalErrorLogging(mockJoinPoint, testException);

            // Then
            assertThat(result).isTrue();
            List<ILoggingEvent> logs = listAppender.list;
            assertThat(logs).hasSize(1);
            assertThat(logs.get(0).getFormattedMessage())
                .contains("CRITICAL ERROR - Erreur critique dans CustomAuthenticationEntryPoint")
                .contains("Test error")
                .contains("testMethod");
        }
        
        @Test
        void shouldHandleNullJoinPoint() {
            // Given
            Exception testException = new RuntimeException("Test error");

            // When
            boolean result = aspect.performCriticalErrorLogging(null, testException);

            // Then - La méthode devrait gérer le cas null et retourner true car le logging réussit
            assertThat(result).isTrue();
            
            // Vérifie que le message d'erreur contient les informations attendues
            List<ILoggingEvent> logs = listAppender.list;
            assertThat(logs).hasSize(1);
            assertThat(logs.get(0).getFormattedMessage())
                .contains("CRITICAL ERROR - Erreur critique dans CustomAuthenticationEntryPoint")
                .contains("Test error")
                .contains("méthode inconnue");
        }
        
        @Test
        void shouldHandleNullSignature() {
            // Given
            when(mockJoinPoint.getSignature()).thenReturn(null);
            Exception testException = new RuntimeException("Test error");

            // When
            boolean result = aspect.performCriticalErrorLogging(mockJoinPoint, testException);

            // Then - Devrait gérer le cas null et retourner true car le logging a réussi
            assertThat(result).isTrue();
            List<ILoggingEvent> logs = listAppender.list;
            assertThat(logs).hasSize(1);
            assertThat(logs.get(0).getFormattedMessage())
                .contains("CRITICAL ERROR - Erreur critique dans CustomAuthenticationEntryPoint")
                .contains("Test error")
                .contains("méthode inconnue");
        }
    }
    
    @Nested
    class PointcutTests {
        
        @Test
        void shouldDefineAuthenticationEntryPointMethodsPointcut() {
            // Given - La méthode est un pointcut vide, on vérifie juste qu'elle existe et peut être appelée
            
            // When
            aspect.authenticationEntryPointMethods();
            
            // Then - Aucune exception ne devrait être levée
            assertThat(aspect).isNotNull();
        }
        
        @Test
        void shouldDefineAuthenticationEntryPointExecutionPointcut() {
            // Given - La méthode est un pointcut vide, on vérifie juste qu'elle existe et peut être appelée
            
            // When
            aspect.authenticationEntryPointExecution();
            
            // Then - Aucune exception ne devrait être levée
            assertThat(aspect).isNotNull();
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

    // Méthode utilitaire pour créer un mock de JoinPoint avec les arguments spécifiés
    private JoinPoint createMockJoinPoint(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) {
        JoinPoint joinPoint = mock(JoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(new Object[]{request, response, exception});
        return joinPoint;
    }
}