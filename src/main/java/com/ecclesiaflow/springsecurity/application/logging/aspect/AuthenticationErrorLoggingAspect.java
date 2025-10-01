package com.ecclesiaflow.springsecurity.application.logging.aspect;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

/**
 * Aspect AOP spécialisé dans le logging des erreurs d'authentification EcclesiaFlow.
 * <p>
 * Cette classe implémente un aspect dédié au logging des erreurs d'authentification
 * interceptées par le CustomAuthenticationEntryPoint. Séparé du logging technique général
 * pour respecter le principe de responsabilité unique et faciliter l'audit de sécurité.
 * </p>
 *
 * <p><strong>Rôle architectural :</strong> Aspect sécurité - Audit des erreurs d'authentification</p>
 *
 * <p><strong>Dépendances critiques :</strong></p>
 * <ul>
 *   <li>Spring AOP - Framework de programmation orientée aspect</li>
 *   <li>SLF4J/Logback - Framework de logging sécurité</li>
 *   <li>CustomAuthenticationEntryPoint - Cible du pointcut</li>
 * </ul>
 *
 * <p><strong>Responsabilités principales :</strong></p>
 * <ul>
 *   <li>Audit des tentatives d'authentification échouées</li>
 *   <li>Traçabilité des erreurs JWT pour sécurité</li>
 *   <li>Logging des détails de requêtes problématiques</li>
 *   <li>Capture des exceptions critiques d'authentification</li>
 * </ul>
 *
 * <p><strong>Cas d'utilisation typiques :</strong></p>
 * <ul>
 *   <li>Détection d'attaques par force brute</li>
 *   <li>Audit de sécurité des tokens JWT</li>
 *   <li>Monitoring des tentatives d'accès non autorisées</li>
 *   <li>Traçabilité pour conformité RGPD/sécurité</li>
 * </ul>
 *
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Component
public class AuthenticationErrorLoggingAspect {

    /**
     * Pointcut pour intercepter les appels à CustomAuthenticationEntryPoint.commence().
     */
    @Pointcut("execution(* com.ecclesiaflow.springsecurity.web.security.CustomAuthenticationEntryPoint.commence(..))")
    public void authenticationEntryPointExecution() {}

    /**
     * Pointcut pour intercepter toutes les méthodes du CustomAuthenticationEntryPoint.
     */
    @Pointcut("execution(* com.ecclesiaflow.springsecurity.web.security.CustomAuthenticationEntryPoint.*(..))")
    public void authenticationEntryPointMethods() {}

    /**
     * Intercepte l'exécution de la méthode commence() pour logger les erreurs d'authentification.
     *
     * @param joinPoint Point de jointure contenant les détails de l'exécution
     */
    @Before("authenticationEntryPointExecution()")
    public void logAuthenticationError(JoinPoint joinPoint) {
        performAuthenticationErrorLogging(joinPoint);
    }

    /**
     * Effectue le logging des erreurs d'authentification et retourne le résultat.
     * Cette méthode est extraite pour faciliter les tests unitaires.
     *
     * @param joinPoint Point de jointure contenant les détails de l'exécution
     */
    public void performAuthenticationErrorLogging(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();

        if (args.length >= 3
                && args[0] instanceof HttpServletRequest request
                && args[2] instanceof AuthenticationException authException) {

            try {
                String clientIp = getClientIpAddress(request);
                String userAgent = request.getHeader("User-Agent");
                String authHeader = request.getHeader("Authorization");

                log.warn("SECURITY ALERT - Erreur d'authentification: {} | URI: {} | IP: {} | User-Agent: {} | Auth Header: {}",
                        authException.getMessage(),
                        request.getRequestURI(),
                        clientIp,
                        userAgent != null ? userAgent.substring(0, Math.min(userAgent.length(), 100)) : "N/A",
                        authHeader != null ? "Bearer ***" : "Absent");

                // Log supplémentaire pour audit de sécurité
                log.info("Authentication attempt failed for IP: {} on endpoint: {}", clientIp, request.getRequestURI());
            } catch (Exception e) {
                log.debug("Erreur lors du logging d'authentification: {}", e.getMessage());
            }
        } else {
            log.debug("logAuthenticationError ignoré en raison d'arguments invalides: {}", args);
        }
    }

    /**
     * Intercepte les exceptions non gérées dans le CustomAuthenticationEntryPoint
     * pour logger les erreurs critiques.
     *
     * @param joinPoint Point de jointure contenant les détails de l'exécution
     * @param exception Exception levée
     */
    @AfterThrowing(pointcut = "authenticationEntryPointMethods()", throwing = "exception")
    public void logAuthenticationEntryPointException(JoinPoint joinPoint, Exception exception) {
        performCriticalErrorLogging(joinPoint, exception);
    }

    /**
     * Effectue le logging des erreurs critiques et retourne le résultat.
     * Cette méthode est extraite pour faciliter les tests unitaires.
     *
     * @param joinPoint Point de jointure contenant les détails de l'exécution
     * @param exception Exception à logger
     * @return true si le logging a été effectué avec succès, false sinon
     */
    public boolean performCriticalErrorLogging(JoinPoint joinPoint, Exception exception) {
        try {
            log.error("CRITICAL ERROR - Erreur critique dans CustomAuthenticationEntryPoint: {} | Méthode: {}",
                    exception.getMessage(),
                    joinPoint.getSignature().getName(),
                    exception);
            return true;
        } catch (Exception e) {
            // En cas d'erreur de logging, on ne peut pas faire grand-chose
            // mais on retourne false pour indiquer l'échec
            return false;
        }
    }

    /**
     * Extrait l'adresse IP réelle du client en tenant compte des proxies et load balancers.
     *
     * @param request Requête HTTP
     * @return Adresse IP du client
     */
    String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
