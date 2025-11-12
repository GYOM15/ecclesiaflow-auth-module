package com.ecclesiaflow.springsecurity.application.logging.aspect;

import com.ecclesiaflow.springsecurity.business.events.PasswordChangedEvent;
import com.ecclesiaflow.springsecurity.business.events.PasswordResetEvent;
import com.ecclesiaflow.springsecurity.business.events.PasswordResetRequestedEvent;
import com.ecclesiaflow.springsecurity.business.events.PasswordSetEvent;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Aspect pour logger les événements d'envoi d'emails liés aux mots de passe.
 * <p>
 * Cet aspect intercepte les méthodes du {@link com.ecclesiaflow.springsecurity.application.handlers.PasswordEventHandler}
 * pour logger l'envoi des emails de manière centralisée et découplée.
 * </p>
 * 
 * <p><strong>Avantages :</strong></p>
 * <ul>
 *   <li>Séparation des préoccupations : le handler ne contient pas de code de logging</li>
 *   <li>Logging centralisé et cohérent</li>
 *   <li>Facile à activer/désactiver via configuration de logs</li>
 * </ul>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
@Aspect
@Component
@Slf4j
public class EmailEventLoggingAspect {

    /**
     * Log l'envoi d'un email de bienvenue.
     */
    @Around("execution(* com.ecclesiaflow.springsecurity.application.handlers.PasswordEventHandler.handlePasswordSet(..))")
    public Object logWelcomeEmail(ProceedingJoinPoint joinPoint) throws Throwable {
        PasswordSetEvent event = (PasswordSetEvent) joinPoint.getArgs()[0];
        log.info("📧 [EMAIL] Sending welcome email to: {}", event.getEmail());
        
        try {
            Object result = joinPoint.proceed();
            log.info("✅ [EMAIL] Welcome email sent successfully to: {}", event.getEmail());
            return result;
        } catch (Exception e) {
            log.error("❌ [EMAIL] Failed to send welcome email to: {}", event.getEmail(), e);
            throw e;
        }
    }

    /**
     * Log l'envoi d'une notification de changement de mot de passe.
     */
    @Around("execution(* com.ecclesiaflow.springsecurity.application.handlers.PasswordEventHandler.handlePasswordChanged(..))")
    public Object logPasswordChangedNotification(ProceedingJoinPoint joinPoint) throws Throwable {
        PasswordChangedEvent event = (PasswordChangedEvent) joinPoint.getArgs()[0];
        log.info("📧 [EMAIL] Sending password changed notification to: {}", event.getEmail());
        
        try {
            Object result = joinPoint.proceed();
            log.info("✅ [EMAIL] Password changed notification sent successfully to: {}", event.getEmail());
            return result;
        } catch (Exception e) {
            log.error("❌ [EMAIL] Failed to send password changed notification to: {}", event.getEmail(), e);
            throw e;
        }
    }

    /**
     * Log l'envoi d'un email de réinitialisation de mot de passe.
     */
    @Around("execution(* com.ecclesiaflow.springsecurity.application.handlers.PasswordEventHandler.handlePasswordResetRequested(..))")
    public Object logPasswordResetEmail(ProceedingJoinPoint joinPoint) throws Throwable {
        PasswordResetRequestedEvent event = (PasswordResetRequestedEvent) joinPoint.getArgs()[0];
        log.info("📧 [EMAIL] Sending password reset email to: {}", event.getEmail());
        
        try {
            Object result = joinPoint.proceed();
            log.info("✅ [EMAIL] Password reset email sent successfully to: {}", event.getEmail());
            return result;
        } catch (Exception e) {
            log.error("❌ [EMAIL] Failed to send password reset email to: {}", event.getEmail(), e);
            throw e;
        }
    }

    /**
     * Log l'envoi d'une notification après réinitialisation de mot de passe.
     */
    @Around("execution(* com.ecclesiaflow.springsecurity.application.handlers.PasswordEventHandler.handlePasswordReset(..))")
    public Object logPasswordResetNotification(ProceedingJoinPoint joinPoint) throws Throwable {
        PasswordResetEvent event = (PasswordResetEvent) joinPoint.getArgs()[0];
        log.info("📧 [EMAIL] Sending password reset confirmation to: {}", event.getEmail());
        
        try {
            Object result = joinPoint.proceed();
            log.info("✅ [EMAIL] Password reset confirmation sent successfully to: {}", event.getEmail());
            return result;
        } catch (Exception e) {
            log.error("❌ [EMAIL] Failed to send password reset confirmation to: {}", event.getEmail(), e);
            throw e;
        }
    }
}
