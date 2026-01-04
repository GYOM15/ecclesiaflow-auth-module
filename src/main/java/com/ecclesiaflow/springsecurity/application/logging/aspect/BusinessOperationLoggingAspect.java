package com.ecclesiaflow.springsecurity.application.logging.aspect;

import com.ecclesiaflow.springsecurity.application.logging.SecurityMaskingUtils;
import com.ecclesiaflow.springsecurity.business.domain.member.Member;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

import java.util.Optional;

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

    /**
     * Pointcut pour le changement de mot de passe (utilisateur authentifié)
     */
    @Pointcut("execution(* com.ecclesiaflow.springsecurity.business.services.impl.PasswordServiceImpl.changePassword(..))")
    public void passwordChange() {}

    /**
     * Pointcut pour la demande de réinitialisation de mot de passe
     */
    @Pointcut("execution(* com.ecclesiaflow.springsecurity.business.services.impl.PasswordServiceImpl.requestPasswordReset(..))")
    public void passwordResetRequest() {}

    // === AUTHENTIFICATION ===
    
    @Before("memberAuthentication()")
    public void logBeforeAuthentication() {
        log.info("BUSINESS: auth | start");
    }

    @AfterReturning("memberAuthentication()")
    public void logAfterSuccessfulAuthentication() {
        log.info("BUSINESS: auth | success");
    }

    @AfterThrowing(pointcut = "memberAuthentication()", throwing = "exception")
    public void logFailedAuthentication(Throwable exception) {
        log.warn("BUSINESS: auth | failed | reason={}", exception.getMessage());
    }

    // === GESTION DES MOTS DE PASSE ===

    @Before("passwordChange()")
    public void logBeforePasswordChange(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        String maskedEmail = SecurityMaskingUtils.maskEmail(args != null && args.length > 0 ? String.valueOf(args[0]) : null);
        log.info("BUSINESS: password_change | start | email={}", maskedEmail);
    }

    @AfterReturning("passwordChange()")
    public void logAfterSuccessfulPasswordChange(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        String maskedEmail = SecurityMaskingUtils.maskEmail(args != null && args.length > 0 ? String.valueOf(args[0]) : null);
        log.info("BUSINESS: password_change | success | email={}", maskedEmail);
    }

    @AfterThrowing(pointcut = "passwordChange()", throwing = "exception")
    public void logFailedPasswordChange(JoinPoint joinPoint, Throwable exception) {
        Object[] args = joinPoint.getArgs();
        String maskedEmail = SecurityMaskingUtils.maskEmail(args != null && args.length > 0 ? String.valueOf(args[0]) : null);
        log.warn("BUSINESS: password_change | failed | email={} | reason={}", maskedEmail, exception.getMessage());
    }

    @Around("passwordResetRequest()")
    public Object logPasswordResetRequestDetailed(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        String rawEmail = (args != null && args.length > 0) ? String.valueOf(args[0]) : null;
        String maskedEmail = SecurityMaskingUtils.maskEmail(rawEmail);

        log.info("BUSINESS: password_reset_request | start | email={}", maskedEmail);

        try {
            Object result = joinPoint.proceed();

            log.info("BUSINESS: password_reset_request | processed | email={}", maskedEmail);

            if (log.isDebugEnabled() && result instanceof Optional<?> opt) {
                if (opt.isPresent() && opt.get() instanceof Member m) {
                    log.debug("BUSINESS: password_reset_request | member_state=present | email={} | memberId={}",
                            maskedEmail,
                            SecurityMaskingUtils.maskId(m.getMemberId()));
                } else {
                    log.debug("BUSINESS: password_reset_request | member_state=absent | email={}", maskedEmail);
                }
            } else if (log.isDebugEnabled()) {
                log.debug("BUSINESS: password_reset_request | returnType={} | email={}",
                        (result == null ? "null" : result.getClass().getSimpleName()),
                        maskedEmail);
            }

            return result;

        } catch (Throwable ex) {
            log.error("BUSINESS: password_reset_request | failed | email={} | reason={}",
                    maskedEmail,
                    SecurityMaskingUtils.rootMessage(ex),
                    ex);
            throw ex;
        }
    }
}
