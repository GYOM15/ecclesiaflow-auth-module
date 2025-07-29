package com.ecclesiaflow.springsecurity.aspect;

import com.ecclesiaflow.springsecurity.annotation.LogExecution;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    // Pointcut pour les services d'authentification
    @Pointcut("execution(* com.ecclesiaflow.springsecurity.services.impl.AuthenticationServiceImpl.*(..))")
    public void authenticationServiceMethods() {}

    // Pointcut pour les contrôleurs
    @Pointcut("execution(* com.ecclesiaflow.springsecurity.controller.*.*(..))")
    public void controllerMethods() {}

    // Pointcut pour les méthodes annotées avec @LogExecution
    @Pointcut("@annotation(com.ecclesiaflow.springsecurity.annotation.LogExecution)")
    public void logExecutionAnnotatedMethods() {}

    // Pointcut pour le GlobalExceptionHandler
    @Pointcut("execution(* com.ecclesiaflow.springsecurity.exception.GlobalExceptionHandler.*(..))")
    public void exceptionHandlerMethods() {}

    // Pointcut pour les opérations critiques (enregistrement et authentification)
    @Pointcut("execution(* com.ecclesiaflow.springsecurity.services.impl.AuthenticationServiceImpl.registerMember(..)) || " +
              "execution(* com.ecclesiaflow.springsecurity.services.impl.AuthenticationServiceImpl.getAuthenticatedMember(..))")
    public void criticalOperations() {}

    // Log avant l'exécution des méthodes critiques
    @Before("criticalOperations()")
    public void logBeforeCriticalOperation(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        
        if ("registerMember".equals(methodName)) {
            log.info("Tentative d'enregistrement d'un nouveau membre");
        } else if ("getAuthenticatedMember".equals(methodName)) {
            log.info("Tentative d'authentification");
        }
    }

    // Log autour des méthodes d'authentification avec gestion des exceptions
    @Around("authenticationServiceMethods()")
    public Object logAroundAuthenticationService(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;
            
            log.info("Méthode {} exécutée avec succès en {}ms", methodName, executionTime);
            
            // Log spécifique pour les opérations importantes
            if ("registerMember".equals(methodName)) {
                log.info("Nouveau membre enregistré avec succès");
            } else if ("getAuthenticatedMember".equals(methodName)) {
                log.info("Authentification réussie");
            } else if ("refreshToken".equals(methodName)) {
                log.info("Token rafraîchi avec succès");
            }
            
            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            
            log.warn("Échec de la méthode {} après {}ms - Erreur: {}", 
                    methodName, executionTime, e.getMessage());
            
            // Log spécifique pour les échecs d'opérations critiques
            if ("registerMember".equals(methodName)) {
                log.warn("Échec de l'enregistrement du membre: {}", e.getMessage());
            } else if ("getAuthenticatedMember".equals(methodName)) {
                log.warn("Échec de l'authentification: {}", e.getMessage());
            } else if ("refreshToken".equals(methodName)) {
                log.warn("Échec du rafraîchissement du token: {}", e.getMessage());
            }
            
            throw e;
        }
    }

    // Log pour les méthodes annotées avec @LogExecution
    @Around("logExecutionAnnotatedMethods() && @annotation(logExecution)")
    public Object logAnnotatedMethods(ProceedingJoinPoint joinPoint, LogExecution logExecution) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        long startTime = System.currentTimeMillis();
        
        String message = logExecution.value().isEmpty() ? 
            String.format("Exécution de %s.%s", className, methodName) : logExecution.value();
        
        if (logExecution.includeParams()) {
            Object[] args = joinPoint.getArgs();
            log.info("{} - Paramètres: {}", message, Arrays.toString(args));
        } else {
            log.info(message);
        }
        
        try {
            Object result = joinPoint.proceed();
            
            if (logExecution.includeExecutionTime()) {
                long executionTime = System.currentTimeMillis() - startTime;
                log.info("{} - Terminée avec succès en {}ms", message, executionTime);
            }
            
            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("{} - Échec après {}ms: {}", message, executionTime, e.getMessage());
            throw e;
        }
    }

    // Log des appels aux contrôleurs
    @Before("controllerMethods()")
    public void logControllerAccess(JoinPoint joinPoint) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        
        log.debug("Appel du contrôleur: {}.{}", className, methodName);
    }

    // Log spécifique pour les gestionnaires d'exceptions
    @Before("exceptionHandlerMethods()")
    public void logExceptionHandler(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        
        if (args.length > 0 && args[0] instanceof Exception) {
            Exception ex = (Exception) args[0];
            log.warn("Gestion d'exception par {} - Type: {}, Message: {}", 
                    methodName, ex.getClass().getSimpleName(), ex.getMessage());
        }
    }

    // Log des exceptions non gérées
    @AfterThrowing(pointcut = "authenticationServiceMethods() || controllerMethods()", throwing = "exception")
    public void logException(JoinPoint joinPoint, Throwable exception) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        
        log.error("Exception dans {}.{}: {} - {}", 
                className, methodName, exception.getClass().getSimpleName(), exception.getMessage());
    }
}
