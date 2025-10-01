package com.ecclesiaflow.springsecurity.application.logging.aspect;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.ecclesiaflow.springsecurity.application.logging.annotation.LogExecution;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class LoggingAspectTest {

    LoggingAspect loggingAspect;
    ListAppender<ILoggingEvent> listAppender;
    Logger logger;

    @BeforeEach
    void setup() {
        loggingAspect = new LoggingAspect();

        // Préparer un logger et y attacher ListAppender pour capter les logs
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

    /**
     * Classe de test servant à simuler une méthode annotée @LogExecution
     */
    static class TestService {
        @LogExecution(value = "TestService.testMethod", includeParams = true, includeExecutionTime = true)
        public String testMethod(String param1, int param2) throws InterruptedException {
            Thread.sleep(50); // simuler un traitement
            return "success";
        }

        public String slowMethod() throws InterruptedException {
            Thread.sleep(1100); // plus d'1s pour déclencher warning
            return "slow";
        }

        public String failMethod() {
            throw new RuntimeException("Erreur simulée");
        }
    }

    /**
     * Classe de test avec annotations @LogExecution pour tester différents scénarios
     */
    static class TestServiceWithAnnotation {
        @LogExecution(value = "Méthode lente", includeParams = false, includeExecutionTime = true)
        public String slowMethod() throws InterruptedException {
            Thread.sleep(1100); // plus d'1s pour déclencher warning
            return "slow";
        }

        @LogExecution(value = "Méthode qui échoue", includeParams = false, includeExecutionTime = true)
        public void failMethod() {
            throw new RuntimeException("Erreur simulée");
        }
    }

    @Test
    @DisplayName("Devrait logger correctement une méthode annotée @LogExecution")
    void shouldLogAnnotatedMethodExecution() throws Throwable {
        TestService target = new TestService();

        // Création proxy AOP avec l'aspect LoggingAspect
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(loggingAspect);
        TestService proxy = factory.getProxy();

        String result = proxy.testMethod("paramValue", 42);
        assertThat(result).isEqualTo("success");

        List<ILoggingEvent> logs = listAppender.list;

        // Vérification début + paramètres
        assertThat(logs.getFirst().getLevel()).isEqualTo(Level.INFO);
        assertThat(logs.getFirst().getFormattedMessage()).contains("Début: TestService.testMethod");
        assertThat(logs.get(0).getFormattedMessage()).contains("paramValue");
        assertThat(logs.get(0).getFormattedMessage()).contains("42");

        // Vérification succès + temps d'exécution
        assertThat(logs.get(1).getLevel()).isEqualTo(Level.INFO);
        assertThat(logs.get(1).getFormattedMessage()).contains("Succès: TestService.testMethod");
        assertThat(logs.get(1).getFormattedMessage()).contains("ms");
    }

    @Test
    @DisplayName("Devrait logger une méthode de service normale (DEBUG, succès)")
    void shouldLogServiceMethodSuccess() throws Throwable {
        TestServiceWithAnnotation target = new TestServiceWithAnnotation();

        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(loggingAspect);
        TestServiceWithAnnotation proxy = factory.getProxy();

        String result = proxy.slowMethod();
        assertThat(result).isEqualTo("slow");

        List<ILoggingEvent> logs = listAppender.list;

        // Vérifier qu'il y a au moins des logs
        assertThat(logs).isNotEmpty();

        // Le premier log : début
        assertThat(logs.get(0).getLevel()).isEqualTo(Level.INFO);
        assertThat(logs.get(0).getFormattedMessage()).contains("Début: Méthode lente");

        // Le deuxième log : succès avec temps d'exécution
        assertThat(logs.get(1).getLevel()).isEqualTo(Level.INFO);
        assertThat(logs.get(1).getFormattedMessage()).contains("Succès: Méthode lente");
        assertThat(logs.get(1).getFormattedMessage()).contains("ms");
    }

    @Test
    @DisplayName("Devrait logger une exception dans méthode de service")
    void shouldLogExceptionInServiceMethod() {
        TestServiceWithAnnotation target = new TestServiceWithAnnotation();

        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(loggingAspect);
        TestServiceWithAnnotation proxy = factory.getProxy();

        Throwable thrown = catchThrowable(proxy::failMethod);
        assertThat(thrown).isInstanceOf(RuntimeException.class).hasMessage("Erreur simulée");

        List<ILoggingEvent> logs = listAppender.list;

        // Vérifier qu'il y a des logs
        assertThat(logs).isNotEmpty();

        // Le premier log : début
        assertThat(logs.get(0).getLevel()).isEqualTo(Level.INFO);
        assertThat(logs.get(0).getFormattedMessage()).contains("Début: Méthode qui échoue");

        // Le deuxième log : erreur
        assertThat(logs.get(1).getLevel()).isEqualTo(Level.ERROR);
        assertThat(logs.get(1).getFormattedMessage()).contains("Échec: Méthode qui échoue");
        assertThat(logs.get(1).getFormattedMessage()).contains("Erreur simulée");
    }


    @Test
    @DisplayName("Devrait logger accès contrôleur (niveau DEBUG)")
    void shouldLogControllerAccess() {
        LoggingAspect aspect = new LoggingAspect();

        // Mock du JoinPoint
        JoinPoint joinPoint = Mockito.mock(JoinPoint.class);
        Signature signature = Mockito.mock(Signature.class);

        // Stub des méthodes nécessaires
        Mockito.when(joinPoint.getTarget()).thenReturn(new TestController());
        Mockito.when(joinPoint.getSignature()).thenReturn(signature);
        Mockito.when(signature.getName()).thenReturn("someMethod");
        Mockito.when(signature.getDeclaringType()).thenReturn(TestController.class);

        aspect.logControllerAccess(joinPoint);

        List<ILoggingEvent> logs = listAppender.list;

        assertThat(logs).isNotEmpty();
        assertThat(logs.getFirst().getLevel()).isEqualTo(Level.DEBUG);
        assertThat(logs.getFirst().getFormattedMessage()).contains("API: TestController.someMethod");
    }

    // Classe test pour simuler contrôleur
    static class TestController {
        public void someMethod() {}
    }
}
