package com.ecclesiaflow.springsecurity.application.logging.aspect;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.ecclesiaflow.springsecurity.application.logging.annotation.LogExecution;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoggingAspectTest {

    LoggingAspect loggingAspect;
    ListAppender<ILoggingEvent> listAppender;
    Logger logger;

    @BeforeEach
    void setup() {
        loggingAspect = new LoggingAspect();

        logger = (Logger) LoggerFactory.getLogger(LoggingAspect.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        logger.setLevel(Level.DEBUG);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(listAppender);
        listAppender.stop();
    }

    // ====================================================================
    // Classes de test
    // ====================================================================

    static class TestService {
        @LogExecution(value = "TestService.testMethod", includeParams = true, includeExecutionTime = true)
        public String testMethod(String param1, int param2) throws InterruptedException {
            Thread.sleep(50);
            return "success";
        }

        @LogExecution(value = "", includeParams = false, includeExecutionTime = false)
        public String methodWithEmptyValue() {
            return "empty-value-result";
        }

        @LogExecution(value = "Méthode sans temps", includeParams = false, includeExecutionTime = false)
        public String methodWithoutExecutionTime() {
            return "no-time-result";
        }

        public String slowMethod() throws InterruptedException {
            Thread.sleep(1100); // > 1s pour warning
            return "slow";
        }

        public String fastMethod() {
            return "fast";
        }

        public String failMethod() {
            throw new RuntimeException("Erreur simulée");
        }
    }

    static class TestServiceWithAnnotation {
        @LogExecution(value = "Méthode lente", includeParams = false, includeExecutionTime = true)
        public String slowMethod() throws InterruptedException {
            Thread.sleep(1100);
            return "slow";
        }

        @LogExecution(value = "Méthode qui échoue", includeParams = false, includeExecutionTime = true)
        public void failMethod() {
            throw new RuntimeException("Erreur simulée");
        }
    }

    static class TestController {
        public void someMethod() {}
    }

    // ====================================================================
    // Tests @LogExecution
    // ====================================================================

    @Test
    @DisplayName("Devrait logger correctement une méthode annotée @LogExecution avec paramètres")
    void shouldLogAnnotatedMethodExecution() throws Throwable {
        TestService target = new TestService();
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(loggingAspect);
        TestService proxy = factory.getProxy();

        String result = proxy.testMethod("paramValue", 42);
        assertThat(result).isEqualTo("success");

        List<ILoggingEvent> logs = listAppender.list;

        assertThat(logs.getFirst().getLevel()).isEqualTo(Level.INFO);
        assertThat(logs.getFirst().getFormattedMessage()).contains("Début: TestService.testMethod");
        assertThat(logs.get(0).getFormattedMessage()).contains("paramValue");
        assertThat(logs.get(0).getFormattedMessage()).contains("42");

        assertThat(logs.get(1).getLevel()).isEqualTo(Level.INFO);
        assertThat(logs.get(1).getFormattedMessage()).contains("Succès: TestService.testMethod");
        assertThat(logs.get(1).getFormattedMessage()).contains("ms");
    }

    @Test
    @DisplayName("Devrait utiliser className.methodName quand value est vide")
    void shouldUseDefaultNameWhenValueIsEmpty() {
        TestService target = new TestService();
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(loggingAspect);
        TestService proxy = factory.getProxy();

        String result = proxy.methodWithEmptyValue();
        assertThat(result).isEqualTo("empty-value-result");

        List<ILoggingEvent> logs = listAppender.list;

        // Devrait contenir TestService.methodWithEmptyValue
        assertThat(logs.getFirst().getFormattedMessage())
                .contains("TestService")
                .contains("methodWithEmptyValue");
    }

    @Test
    @DisplayName("Devrait ne pas afficher le temps d'exécution quand includeExecutionTime=false")
    void shouldNotLogExecutionTimeWhenDisabled() {
        TestService target = new TestService();
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(loggingAspect);
        TestService proxy = factory.getProxy();

        String result = proxy.methodWithoutExecutionTime();
        assertThat(result).isEqualTo("no-time-result");

        List<ILoggingEvent> logs = listAppender.list;

        // Log de succès sans temps
        ILoggingEvent successLog = logs.get(1);
        assertThat(successLog.getFormattedMessage()).contains("Succès: Méthode sans temps");
        assertThat(successLog.getFormattedMessage()).doesNotContain("ms");
    }

    @Test
    @DisplayName("Devrait logger une exception dans méthode annotée")
    void shouldLogExceptionInAnnotatedMethod() {
        TestServiceWithAnnotation target = new TestServiceWithAnnotation();
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(loggingAspect);
        TestServiceWithAnnotation proxy = factory.getProxy();

        Throwable thrown = catchThrowable(proxy::failMethod);
        assertThat(thrown).isInstanceOf(RuntimeException.class).hasMessage("Erreur simulée");

        List<ILoggingEvent> logs = listAppender.list;

        assertThat(logs.get(0).getLevel()).isEqualTo(Level.INFO);
        assertThat(logs.get(0).getFormattedMessage()).contains("Début: Méthode qui échoue");

        assertThat(logs.get(1).getLevel()).isEqualTo(Level.ERROR);
        assertThat(logs.get(1).getFormattedMessage()).contains("Échec: Méthode qui échoue");
        assertThat(logs.get(1).getFormattedMessage()).contains("Erreur simulée");
    }

    // ====================================================================
    // Tests logServiceMethods (via logMethodExecution)
    // ====================================================================

    @Test
    @DisplayName("Devrait logger une méthode de service rapide (< 1s)")
    void shouldLogFastServiceMethod() throws Throwable {
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(signature.getName()).thenReturn("fastMethod");

        when(pjp.getTarget()).thenReturn(new TestService());
        when(pjp.getSignature()).thenReturn(signature);
        when(pjp.proceed()).thenReturn("fast");

        Object result = loggingAspect.logServiceMethods(pjp);

        assertThat(result).isEqualTo("fast");

        List<ILoggingEvent> logs = listAppender.list;

        // Debug début
        assertThat(logs.get(0).getLevel()).isEqualTo(Level.DEBUG);
        assertThat(logs.get(0).getFormattedMessage()).contains("SERVICE: Début TestService.fastMethod");

        // Debug succès (pas de warning car < 1s)
        assertThat(logs.get(1).getLevel()).isEqualTo(Level.DEBUG);
        assertThat(logs.get(1).getFormattedMessage()).contains("SERVICE: TestService.fastMethod - Succès");
    }

    @Test
    @DisplayName("Devrait logger un warning pour méthode de service lente (> 1s)")
    void shouldLogWarningForSlowServiceMethod() throws Throwable {
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(signature.getName()).thenReturn("slowMethod");

        when(pjp.getTarget()).thenReturn(new TestService());
        when(pjp.getSignature()).thenReturn(signature);
        when(pjp.proceed()).thenAnswer(invocation -> {
            Thread.sleep(1100); // Forcer > 1s
            return "slow";
        });

        Object result = loggingAspect.logServiceMethods(pjp);

        assertThat(result).isEqualTo("slow");

        List<ILoggingEvent> logs = listAppender.list;

        // Warning pour exécution lente
        ILoggingEvent warnLog = logs.stream()
                .filter(log -> log.getLevel() == Level.WARN)
                .findFirst()
                .orElseThrow();

        assertThat(warnLog.getFormattedMessage())
                .contains("SERVICE: TestService.slowMethod - Exécution lente");
    }

    @Test
    @DisplayName("Devrait logger une exception dans méthode de service")
    void shouldLogExceptionInServiceMethod() throws Throwable {
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(signature.getName()).thenReturn("failMethod");

        when(pjp.getTarget()).thenReturn(new TestService());
        when(pjp.getSignature()).thenReturn(signature);
        when(pjp.proceed()).thenThrow(new RuntimeException("Service error"));

        assertThatThrownBy(() -> loggingAspect.logServiceMethods(pjp))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Service error");

        List<ILoggingEvent> logs = listAppender.list;

        // Error log
        ILoggingEvent errorLog = logs.stream()
                .filter(log -> log.getLevel() == Level.ERROR)
                .filter(log -> log.getFormattedMessage().contains("SERVICE:"))
                .findFirst()
                .orElseThrow();

        assertThat(errorLog.getFormattedMessage())
                .contains("SERVICE: TestService.failMethod - Échec")
                .contains("Service error");
    }

    // ====================================================================
    // Tests logControllerAccess
    // ====================================================================

    @Test
    @DisplayName("Devrait logger accès contrôleur (niveau DEBUG)")
    void shouldLogControllerAccess() {
        JoinPoint joinPoint = Mockito.mock(JoinPoint.class);
        Signature signature = Mockito.mock(Signature.class);

        Mockito.when(joinPoint.getTarget()).thenReturn(new TestController());
        Mockito.when(joinPoint.getSignature()).thenReturn(signature);
        Mockito.when(signature.getName()).thenReturn("someMethod");

        loggingAspect.logControllerAccess(joinPoint);

        List<ILoggingEvent> logs = listAppender.list;

        assertThat(logs).isNotEmpty();
        assertThat(logs.getFirst().getLevel()).isEqualTo(Level.DEBUG);
        assertThat(logs.getFirst().getFormattedMessage()).contains("API: TestController.someMethod");
    }

    // ====================================================================
    // Tests logUnhandledException
    // ====================================================================

    @Test
    @DisplayName("Devrait logger une exception non gérée")
    void shouldLogUnhandledException() {
        JoinPoint joinPoint = mock(JoinPoint.class);
        Signature signature = mock(Signature.class);
        when(signature.getName()).thenReturn("problematicMethod");

        when(joinPoint.getTarget()).thenReturn(new TestService());
        when(joinPoint.getSignature()).thenReturn(signature);

        RuntimeException exception = new RuntimeException("Unhandled error");

        loggingAspect.logUnhandledException(joinPoint, exception);

        List<ILoggingEvent> logs = listAppender.list;

        ILoggingEvent errorLog = logs.getFirst();
        assertThat(errorLog.getLevel()).isEqualTo(Level.ERROR);
        assertThat(errorLog.getFormattedMessage())
                .contains("Exception non gérée dans TestService.problematicMethod")
                .contains("RuntimeException")
                .contains("Unhandled error");
    }

    @Test
    @DisplayName("Devrait logger différents types d'exceptions")
    void shouldLogDifferentExceptionTypes() {
        JoinPoint joinPoint = mock(JoinPoint.class);
        Signature signature = mock(Signature.class);
        when(signature.getName()).thenReturn("methodWithNPE");

        when(joinPoint.getTarget()).thenReturn(new TestService());
        when(joinPoint.getSignature()).thenReturn(signature);

        NullPointerException npe = new NullPointerException("Null value");

        loggingAspect.logUnhandledException(joinPoint, npe);

        List<ILoggingEvent> logs = listAppender.list;

        ILoggingEvent errorLog = logs.getFirst();
        assertThat(errorLog.getFormattedMessage())
                .contains("NullPointerException")
                .contains("Null value");
    }
}