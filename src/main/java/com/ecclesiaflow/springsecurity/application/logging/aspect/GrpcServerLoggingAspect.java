package com.ecclesiaflow.springsecurity.application.logging.aspect;

import com.ecclesiaflow.springsecurity.application.logging.SecurityMaskingUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Aspect AOP dédié au logging des opérations gRPC du module d'authentification.
 * <p>
 * Cette classe implémente un aspect spécialisé dans le logging des communications
 * gRPC inter-modules. Elle capture les appels entrants sur le serveur gRPC,
 * les erreurs de traitement et les métriques de performance.
 * </p>
 *
 * <p><strong>Rôle architectural :</strong> Aspect infrastructure - Audit des communications gRPC</p>
 *
 * <p><strong>Dépendances critiques :</strong></p>
 * <ul>
 *   <li>Spring AOP - Framework de programmation orientée aspect</li>
 *   <li>SLF4J/Logback - Framework de logging</li>
 *   <li>GrpcServerConfig - Configuration du serveur gRPC</li>
 *   <li>JwtGrpcServiceImpl - Implémentation des services gRPC JWT</li>
 * </ul>
 *
 * <p><strong>Responsabilités principales :</strong></p>
 * <ul>
 *   <li>Logging des démarrages/arrêts du serveur gRPC</li>
 *   <li>Audit des appels RPC entrants (GenerateTemporaryToken)</li>
 *   <li>Capture des erreurs de traitement gRPC</li>
 *   <li>Traçabilité inter-modules (Members → Auth)</li>
 *   <li>Sanitization des données sensibles (emails, tokens)</li>
 * </ul>
 *
 * <p><strong>Cas d'utilisation typiques :</strong></p>
 * <ul>
 *   <li>Audit de sécurité des communications inter-services</li>
 *   <li>Monitoring des performances gRPC (latence, throughput)</li>
 *   <li>Debugging des problèmes de communication</li>
 *   <li>Analyse des patterns d'appels entre modules</li>
 * </ul>
 *
 * <p><strong>Sécurité :</strong></p>
 * <ul>
 *   <li>Masquage partiel des emails (GDPR compliance)</li>
 *   <li>Pas de logging des tokens complets</li>
 *   <li>Sanitization automatique des données sensibles</li>
 * </ul>
 *
 * <p><strong>Garanties :</strong> Thread-safe, logging asynchrone, impact minimal sur performances.</p>
 *
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Component
@ConditionalOnProperty(name = "grpc.enabled", havingValue = "true", matchIfMissing = false)
public class GrpcServerLoggingAspect {

    // ========================================================================
    // Pointcuts - Définition des points d'interception
    // ========================================================================

    /**
     * Pointcut pour le démarrage du serveur gRPC.
     * <p>
     * Intercepte la méthode {@code start()} pour auditer les démarrages du serveur.
     * </p>
     */
    @Pointcut("execution(* com.ecclesiaflow.springsecurity.application.config.GrpcServerConfig.start())")
    public void grpcServerStart() {}

    /**
     * Pointcut pour l'arrêt du serveur gRPC.
     * <p>
     * Intercepte la méthode {@code stop()} pour auditer les arrêts graceful.
     * </p>
     */
    @Pointcut("execution(* com.ecclesiaflow.springsecurity.application.config.GrpcServerConfig.stop())")
    public void grpcServerStop() {}

    /**
     * Pointcut pour tous les appels RPC du service JWT.
     * <p>
     * Intercepte toutes les méthodes publiques de {@code JwtGrpcServiceImpl}
     * pour auditer les communications gRPC entrantes.
     * </p>
     */
    @Pointcut("execution(* com.ecclesiaflow.springsecurity.io.grpc.server.JwtGrpcServiceImpl.*(..))")
    public void grpcServiceCalls() {}

    // ========================================================================
    // Advices - Démarrage/Arrêt du serveur gRPC
    // ========================================================================

    /**
     * Log avant le démarrage du serveur gRPC.
     *
     * @param joinPoint point de jonction contenant les détails de l'appel
     */
    @Before("grpcServerStart()")
    public void logBeforeServerStart(JoinPoint joinPoint) {
        log.info("🚀 GRPC: Initializing gRPC server for Auth module...");
    }

    /**
     * Log après démarrage réussi du serveur gRPC.
     *
     * @param joinPoint point de jonction
     */
    @AfterReturning("grpcServerStart()")
    public void logAfterServerStart(JoinPoint joinPoint) {
        log.info("✅ GRPC: gRPC server started successfully and ready to accept connections");
    }

    /**
     * Log des erreurs lors du démarrage du serveur gRPC.
     *
     * @param joinPoint point de jonction
     * @param exception exception levée lors du démarrage
     */
    @AfterThrowing(pointcut = "grpcServerStart()", throwing = "exception")
    public void logServerStartError(JoinPoint joinPoint, Exception exception) {
        log.error("❌ GRPC: Failed to start gRPC server - {}: {}", 
                exception.getClass().getSimpleName(), 
                SecurityMaskingUtils.sanitizeInfra(exception.getMessage()),
                exception);
    }

    /**
     * Log avant l'arrêt du serveur gRPC.
     *
     * @param joinPoint point de jonction
     */
    @Before("grpcServerStop()")
    public void logBeforeServerStop(JoinPoint joinPoint) {
        log.info("GRPC: Initiating graceful shutdown of gRPC server...");
    }

    /**
     * Log après arrêt réussi du serveur gRPC.
     *
     * @param joinPoint point de jonction
     */
    @AfterReturning("grpcServerStop()")
    public void logAfterServerStop(JoinPoint joinPoint) {
        log.info("GRPC: gRPC server stopped successfully");
    }

    // ========================================================================
    // Advices - Appels RPC (GenerateTemporaryToken)
    // ========================================================================

    /**
     * Log avant chaque appel RPC entrant.
     * <p>
     * Capture les informations de base de l'appel pour traçabilité inter-modules.
     * Masque les données sensibles (emails, tokens).
     * </p>
     *
     * @param joinPoint point de jonction contenant le nom de la méthode et les arguments
     */
    @Before("grpcServiceCalls()")
    public void logBeforeRpcCall(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        
        // Log selon le type de RPC
        if ("generateTemporaryToken".equals(methodName)) {
            log.info("GRPC-RPC: Received {} request from Members module", methodName);
        } else if ("validateToken".equals(methodName)) {
            log.debug("[GRPC-RPC]: Received {} request", methodName);
        } else {
            log.debug("[GRPC-RPC]: Received {} request", methodName);
        }
    }

    /**
     * Log après succès d'un appel RPC.
     * <p>
     * Confirme le traitement réussi de l'appel RPC.
     * </p>
     *
     * @param joinPoint point de jonction
     */
    @AfterReturning("grpcServiceCalls()")
    public void logAfterSuccessfulRpcCall(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        
        if ("generateTemporaryToken".equals(methodName)) {
            log.info("GRPC-RPC: {} completed successfully", methodName);
        } else {
            log.debug("GRPC-RPC: {} completed successfully", methodName);
        }
    }

    /**
     * Log des erreurs lors du traitement RPC.
     * <p>
     * Capture les exceptions levées pendant le traitement des appels gRPC.
     * Différencie les erreurs métier (IllegalArgumentException) des erreurs techniques.
     * </p>
     *
     * @param joinPoint point de jonction
     * @param exception exception levée
     */
    @AfterThrowing(pointcut = "grpcServiceCalls()", throwing = "exception")
    public void logRpcCallError(JoinPoint joinPoint, Exception exception) {
        String methodName = joinPoint.getSignature().getName();
        
        // Erreurs de validation (members error)
        if (exception instanceof IllegalArgumentException) {
            log.warn("GRPC-RPC: Invalid argument in {} - {}",
                    methodName, 
                    SecurityMaskingUtils.sanitizeInfra(exception.getMessage()),
                    exception);
        } 
        // Erreurs internes (server error)
        else {
            log.error("GRPC-RPC: Error in {} - {}: {}",
                    methodName,
                    exception.getClass().getSimpleName(), 
                    SecurityMaskingUtils.sanitizeInfra(exception.getMessage()),
                    exception);
        }
    }

}
