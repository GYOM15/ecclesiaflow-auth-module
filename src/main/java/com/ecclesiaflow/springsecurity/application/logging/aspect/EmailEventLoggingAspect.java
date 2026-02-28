package com.ecclesiaflow.springsecurity.application.logging.aspect;

import com.ecclesiaflow.springsecurity.application.logging.SecurityMaskingUtils;
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
        String maskedEmail = SecurityMaskingUtils.maskEmail(event.getEmail());

        log.info("[EMAIL] welcome | start | email={}", maskedEmail);

        try {
            Object result = joinPoint.proceed();
            log.info("[EMAIL] welcome | success | email={}", maskedEmail);
            return result;
        } catch (Exception e) {
            log.error("[EMAIL] welcome | failed | email={} | reason={}", maskedEmail, SecurityMaskingUtils.rootMessage(e), e);
            throw e;
        }
    }
}
