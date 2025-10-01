package com.ecclesiaflow.springsecurity.application.logging.aspect;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.junit.jupiter.api.*;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitaires pour BusinessOperationLoggingAspect.
 */
@SpringBootTest(classes = BusinessOperationLoggingAspectTest.TestConfig.class)
@DisplayName("BusinessOperationLoggingAspect - Tests unitaires")
class BusinessOperationLoggingAspectTest {

    @Autowired
    private TestAuthenticationServiceImpl testAuthService;

    private ListAppender<ILoggingEvent> listAppender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(TestBusinessOperationAspect.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        logger.setLevel(Level.DEBUG);
    }

    @AfterEach
    void tearDown() {
        if (logger != null) {
            logger.detachAppender(listAppender);
        }
        if (listAppender != null) {
            listAppender.stop();
        }
    }

    @Nested
    @DisplayName("Tests d'authentification")
    class AuthenticationTests {

        @Test
        @DisplayName("Devrait logger l'authentification réussie")
        void shouldLogSuccessfulAuthentication() {
            String result = testAuthService.getAuthenticatedMember();

            assertThat(result).isEqualTo("authenticated-member-123");

            List<ILoggingEvent> logs = listAppender.list;
            assertThat(logs)
                    .hasSize(2)
                    .satisfies(logList -> {
                        ILoggingEvent startLog = logList.getFirst();
                        assertThat(startLog.getLevel()).isEqualTo(Level.INFO);
                        assertThat(startLog.getFormattedMessage())
                                .contains("BUSINESS: Tentative d'authentification");

                        ILoggingEvent successLog = logList.get(1);
                        assertThat(successLog.getLevel()).isEqualTo(Level.INFO);
                        assertThat(successLog.getFormattedMessage())
                                .contains("BUSINESS: Authentification réussie");
                    });
        }

        @Test
        @DisplayName("Devrait logger l'échec d'authentification avec message d'erreur")
        void shouldLogFailedAuthentication() {
            assertThatThrownBy(() -> testAuthService.getAuthenticatedMemberWithError())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Token invalide ou expiré");

            List<ILoggingEvent> logs = listAppender.list;
            assertThat(logs)
                    .hasSize(2)
                    .satisfies(logList -> {
                        ILoggingEvent startLog = logList.getFirst();
                        assertThat(startLog.getLevel()).isEqualTo(Level.INFO);
                        assertThat(startLog.getFormattedMessage())
                                .contains("BUSINESS: Tentative d'authentification");

                        ILoggingEvent errorLog = logList.get(1);
                        assertThat(errorLog.getLevel()).isEqualTo(Level.WARN);
                        assertThat(errorLog.getFormattedMessage())
                                .contains("BUSINESS: Échec de l'authentification")
                                .contains("Token invalide ou expiré");
                    });
        }

        @Test
        @DisplayName("Devrait gérer les SecurityException")
        void shouldHandleSecurityExceptions() {
            assertThatThrownBy(() -> testAuthService.getAuthenticatedMemberWithSecurityError())
                    .isInstanceOf(SecurityException.class)
                    .hasMessage("Accès interdit");

            List<ILoggingEvent> logs = listAppender.list;
            assertThat(logs)
                    .hasSize(2);

            ILoggingEvent errorLog = logs.get(1);
            assertThat(errorLog.getLevel()).isEqualTo(Level.WARN);
            assertThat(errorLog.getFormattedMessage())
                    .contains("BUSINESS: Échec de l'authentification")
                    .contains("Accès interdit");
        }

        @Test
        @DisplayName("Devrait gérer les IllegalStateException")
        void shouldHandleIllegalStateExceptions() {
            assertThatThrownBy(() -> testAuthService.getAuthenticatedMemberWithStateError())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("État du système invalide");

            List<ILoggingEvent> logs = listAppender.list;
            ILoggingEvent errorLog = logs.get(1);
            assertThat(errorLog.getFormattedMessage())
                    .contains("BUSINESS: Échec de l'authentification")
                    .contains("État du système invalide");
        }

        @Test
        @DisplayName("Devrait logger avec token null")
        void shouldHandleNullToken() {
            assertThatThrownBy(() -> testAuthService.getAuthenticatedMemberWithNullPointer())
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("Token est null");

            List<ILoggingEvent> logs = listAppender.list;
            ILoggingEvent errorLog = logs.get(1);
            assertThat(errorLog.getFormattedMessage())
                    .contains("BUSINESS: Échec de l'authentification")
                    .contains("Token est null");
        }
    }

    @Nested
    @DisplayName("Tests de robustesse")
    class RobustnessTests {

        @Test
        @DisplayName("Devrait gérer les appels concurrents")
        void shouldHandleConcurrentCalls() throws InterruptedException {
            int threadCount = 5;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch finishLatch = new CountDownLatch(threadCount);

            for (int i = 0; i < threadCount; i++) {
                new Thread(() -> {
                    try {
                        startLatch.await();
                        testAuthService.getAuthenticatedMember();
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
            assertThat(logs.size()).isEqualTo(threadCount * 2);
        }

        @Test
        @DisplayName("Devrait maintenir les performances")
        void shouldMaintainPerformance() {
            int iterations = 100;
            long startTime = System.nanoTime();

            for (int i = 0; i < iterations; i++) {
                testAuthService.getAuthenticatedMember();
            }

            long endTime = System.nanoTime();
            long totalTimeMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
            assertThat(totalTimeMs).isLessThan(1000);

            List<ILoggingEvent> logs = listAppender.list;
            assertThat(logs.size()).isEqualTo(iterations * 2);
        }

        @Test
        @DisplayName("Devrait traiter les exceptions en chaîne")
        void shouldHandleChainedExceptions() {
            assertThatThrownBy(() -> testAuthService.getAuthenticatedMemberWithChainedException())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Erreur d'authentification")
                    .hasCauseInstanceOf(IllegalArgumentException.class);

            List<ILoggingEvent> logs = listAppender.list;
            ILoggingEvent errorLog = logs.get(1);
            assertThat(errorLog.getFormattedMessage())
                    .contains("BUSINESS: Échec de l'authentification")
                    .contains("Erreur d'authentification");
        }
    }

    @Nested
    @DisplayName("Tests de validation des logs")
    class LogValidationTests {

        @Test
        @DisplayName("Devrait logger avec le bon format de timestamp")
        void shouldLogWithCorrectTimestamp() {
            testAuthService.getAuthenticatedMember();

            List<ILoggingEvent> logs = listAppender.list;
            assertThat(logs)
                    .allSatisfy(log -> {
                        assertThat(log.getTimeStamp()).isPositive();
                        assertThat(log.getTimeStamp()).isCloseTo(System.currentTimeMillis(), within(5000L));
                    });
        }

        @Test
        @DisplayName("Devrait logger avec le bon thread")
        void shouldLogWithCorrectThread() {
            testAuthService.getAuthenticatedMember();

            List<ILoggingEvent> logs = listAppender.list;
            String currentThreadName = Thread.currentThread().getName();
            assertThat(logs)
                    .allSatisfy(log -> assertThat(log.getThreadName()).isEqualTo(currentThreadName));
        }

        @Test
        @DisplayName("Devrait logger sans données sensibles")
        void shouldLogWithoutSensitiveData() {
            testAuthService.getAuthenticatedMemberWithSensitiveData();

            List<ILoggingEvent> logs = listAppender.list;
            assertThat(logs)
                    .allSatisfy(log -> {
                        String msg = log.getFormattedMessage();
                        assertThat(msg).doesNotContain("password");
                        assertThat(msg).doesNotContain("secret");
                        assertThat(msg).doesNotContain("token123");
                    });
        }
    }

    // ===========================
    // CONFIGURATION DE TEST
    // ===========================

    @Configuration
    @EnableAspectJAutoProxy
    static class TestConfig {

        @Bean
        public TestBusinessOperationAspect testBusinessOperationAspect() {
            return new TestBusinessOperationAspect();
        }

        @Bean
        public TestAuthenticationServiceImpl testAuthenticationServiceImpl() {
            return new TestAuthenticationServiceImpl();
        }
    }

    // ===========================
    // ASPECT DE TEST
    // ===========================

    @Slf4j
    @Aspect
    @Component
    static class TestBusinessOperationAspect {

        @Pointcut("execution(* com.ecclesiaflow.springsecurity.application.logging.aspect.BusinessOperationLoggingAspectTest.TestAuthenticationServiceImpl.*(..))")
        public void testServiceMethods() {}

        @Before("testServiceMethods()")
        public void logBeforeAuthentication(JoinPoint joinPoint) {
            log.info("BUSINESS: Tentative d'authentification");
        }

        @AfterReturning("testServiceMethods()")
        public void logAfterSuccessfulAuthentication(JoinPoint joinPoint) {
            log.info("BUSINESS: Authentification réussie");
        }

        @AfterThrowing(pointcut = "testServiceMethods()", throwing = "exception")
        public void logFailedAuthentication(JoinPoint joinPoint, Throwable exception) {
            log.warn("BUSINESS: Échec de l'authentification - {}", exception.getMessage());
        }
    }

    // ===========================
    // SERVICE DE TEST
    // ===========================

    static class TestAuthenticationServiceImpl {

        public String getAuthenticatedMember() {
            return "authenticated-member-123";
        }

        public void getAuthenticatedMemberWithError() {
            throw new RuntimeException("Token invalide ou expiré");
        }

        public void getAuthenticatedMemberWithSecurityError() {
            throw new SecurityException("Accès interdit");
        }

        public void getAuthenticatedMemberWithStateError() {
            throw new IllegalStateException("État du système invalide");
        }

        public void getAuthenticatedMemberWithNullPointer() {
            throw new NullPointerException("Token est null");
        }

        public void getAuthenticatedMemberWithChainedException() {
            IllegalArgumentException cause = new IllegalArgumentException("Paramètre invalide");
            throw new RuntimeException("Erreur d'authentification", cause);
        }

        public void getAuthenticatedMemberWithSensitiveData() {
            // Simule des données sensibles non loggées
            String password = "secret123";
            String token = "token123";
        }
    }
}
