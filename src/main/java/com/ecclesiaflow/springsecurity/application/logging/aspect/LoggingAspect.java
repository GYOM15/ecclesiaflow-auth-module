package com.ecclesiaflow.springsecurity.application.logging.aspect;

import com.ecclesiaflow.springsecurity.io.annotation.LogExecution;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Aspect AOP responsable du logging automatique des opérations critiques EcclesiaFlow.
 * <p>
 * Cette classe implémente la programmation orientée aspect pour capturer automatiquement
 * les appels de méthodes dans les services et contrôleurs, et générer des logs détaillés
 * pour le monitoring, le debugging et l'audit des opérations système.
 * </p>
 * 
 * <p><strong>Rôle architectural :</strong> Aspect transversal - Logging automatique</p>
 * 
 * <p><strong>Dépendances critiques :</strong></p>
 * <ul>
 *   <li>Spring AOP - Framework de programmation orientée aspect</li>
 *   <li>SLF4J/Logback - Framework de logging</li>
 *   <li>Annotation {@link LogExecution} - Marquage des méthodes à logger</li>
 * </ul>
 * 
 * <p><strong>Responsabilités principales :</strong></p>
 * <ul>
 *   <li>Logging automatique des entrées/sorties de méthodes</li>
 *   <li>Capture des exceptions et erreurs système</li>
 *   <li>Mesure des temps d'exécution pour le monitoring</li>
 *   <li>Logging configurable via annotation @LogExecution</li>
 * </ul>
 * 
 * <p><strong>Cas d'utilisation typiques :</strong></p>
 * <ul>
 *   <li>Debugging des flux d'authentification</li>
 *   <li>Monitoring des performances des services</li>
 *   <li>Audit des opérations sensibles (inscription, connexion)</li>
 *   <li>Traçabilité des erreurs en production</li>
 * </ul>
 * 
 * <p><strong>Garanties :</strong> Thread-safe, impact minimal sur les performances, logging asynchrone.</p>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Component
public class LoggingAspect {

    /**
     * Pointcut pour toutes les méthodes des services
     */
    @Pointcut("execution(* com.ecclesiaflow.springsecurity.business.services..*(..))")
    public void serviceMethods() {}

    /**
     * Pointcut pour toutes les méthodes des contrôleurs
     */
    @Pointcut("execution(* com.ecclesiaflow.springsecurity.web.controller..*(..))")
    public void controllerMethods() {}

    /**
     * Pointcut pour les méthodes annotées avec @LogExecution
     */
    @Pointcut("@annotation(com.ecclesiaflow.springsecurity.io.annotation.LogExecution)")
    public void logExecutionAnnotatedMethods() {}

    /**
     * Log générique pour les méthodes de service avec gestion des performances
     */
    @Around("serviceMethods()")
    public Object logServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        return logMethodExecution(joinPoint, "SERVICE");
    }

    /**
     * Log pour les méthodes annotées avec @LogExecution (configuration flexible)
     */
    @Around("logExecutionAnnotatedMethods() && @annotation(logExecution)")
    public Object logAnnotatedMethods(ProceedingJoinPoint joinPoint, LogExecution logExecution) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        long startTime = System.currentTimeMillis();
        
        String message = logExecution.value().isEmpty() ? 
            String.format("%s.%s", className, methodName) : logExecution.value();
        
        // Log des paramètres si demandé
        if (logExecution.includeParams()) {
            Object[] args = joinPoint.getArgs();
            log.info("Début: {} - Paramètres: {}", message, Arrays.toString(args));
        } else {
            log.info("Début: {}", message);
        }
        
        try {
            Object result = joinPoint.proceed();
            
            if (logExecution.includeExecutionTime()) {
                long executionTime = System.currentTimeMillis() - startTime;
                log.info("Succès: {} ({}ms)", message, executionTime);
            } else {
                log.info("Succès: {}", message);
            }
            
            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("Échec: {} ({}ms) - {}", message, executionTime, e.getMessage());
            throw e;
        }
    }

    /**
     * Log des appels aux contrôleurs (niveau DEBUG pour éviter le spam)
     */
    @Before("controllerMethods()")
    public void logControllerAccess(JoinPoint joinPoint) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        log.debug("API: {}.{}", className, methodName);
    }

    /**
     * Méthode utilitaire pour le logging générique des méthodes
     */
    private Object logMethodExecution(ProceedingJoinPoint joinPoint, String layer) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        long startTime = System.currentTimeMillis();
        
        log.debug("{}: Début {}.{}", layer, className, methodName);
        
        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;
            
            if (executionTime > 1000) { // Log si > 1 seconde
                log.warn("{}: {}.{} - Exécution lente ({}ms)", layer, className, methodName, executionTime);
            } else {
                log.debug("{}: {}.{} - Succès ({}ms)", layer, className, methodName, executionTime);
            }
            
            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("{}: {}.{} - Échec ({}ms): {}", layer, className, methodName, executionTime, e.getMessage());
            throw e;
        }
    }

    /**
     * Log des exceptions non gérées dans les services et contrôleurs
     */
    @AfterThrowing(pointcut = "serviceMethods() || controllerMethods()", throwing = "exception")
    public void logUnhandledException(JoinPoint joinPoint, Throwable exception) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        
        log.error("Exception non gérée dans {}.{}: {} - {}", 
                className, methodName, exception.getClass().getSimpleName(), exception.getMessage());
    }
}
