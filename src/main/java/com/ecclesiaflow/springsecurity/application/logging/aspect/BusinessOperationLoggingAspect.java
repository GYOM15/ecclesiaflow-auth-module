package com.ecclesiaflow.springsecurity.application.logging.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * Aspect AOP spécialisé dans le logging des opérations métier critiques EcclesiaFlow.
 * <p>
 * Cette classe implémente un aspect dédié au logging des opérations métier sensibles
 * comme l'inscription, l'authentification et le rafraîchissement de tokens.
 * Séparé du logging technique général pour respecter le principe de responsabilité unique.
 * </p>
 * 
 * <p><strong>Rôle architectural :</strong> Aspect métier - Audit des opérations critiques</p>
 * 
 * <p><strong>Dépendances critiques :</strong></p>
 * <ul>
 *   <li>Spring AOP - Framework de programmation orientée aspect</li>
 *   <li>SLF4J/Logback - Framework de logging métier</li>
 *   <li>Services d'authentification - Cibles des pointcuts</li>
 * </ul>
 * 
 * <p><strong>Responsabilités principales :</strong></p>
 * <ul>
 *   <li>Audit des inscriptions de nouveaux membres</li>
 *   <li>Traçabilité des tentatives d'authentification</li>
 *   <li>Logging des opérations de rafraîchissement de tokens</li>
 *   <li>Capture des échecs d'opérations métier</li>
 * </ul>
 * 
 * <p><strong>Cas d'utilisation typiques :</strong></p>
 * <ul>
 *   <li>Audit de sécurité des comptes utilisateurs</li>
 *   <li>Traçabilité des connexions pour conformité</li>
 *   <li>Détection des tentatives d'intrusion</li>
 *   <li>Analyse des patterns d'utilisation</li>
 * </ul>
 * 
 * <p><strong>Garanties :</strong> Thread-safe, logging asynchrone, séparation métier/technique.</p>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Component
public class BusinessOperationLoggingAspect {

    /**
     * Pointcut pour l'authentification
     */
    @Pointcut("execution(* com.ecclesiaflow.springsecurity.business.services.impl.AuthenticationServiceImpl.getAuthenticatedMember(..))")
    public void memberAuthentication() {}

    // === AUTHENTIFICATION ===
    
    @Before("memberAuthentication()")
    public void logBeforeAuthentication(JoinPoint joinPoint) {
        log.info("BUSINESS: Tentative d'authentification");
    }

    @AfterReturning("memberAuthentication()")
    public void logAfterSuccessfulAuthentication(JoinPoint joinPoint) {
        log.info("BUSINESS: Authentification réussie");
    }

    @AfterThrowing(pointcut = "memberAuthentication()", throwing = "exception")
    public void logFailedAuthentication(JoinPoint joinPoint, Throwable exception) {
        log.warn("BUSINESS: Échec de l'authentification - {}", exception.getMessage());
    }
}
