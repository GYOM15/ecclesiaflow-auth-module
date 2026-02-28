package com.ecclesiaflow.springsecurity.application.logging.aspect;

import com.ecclesiaflow.springsecurity.application.logging.SecurityMaskingUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * AOP aspect dedicated to logging gRPC operations in the authentication module.
 * <p>
 * This class implements a specialized aspect for logging inter-module gRPC communications.
 * It captures incoming gRPC server calls, processing errors, and performance metrics.
 * </p>
 *
 *
 * <p><strong>Critical dependencies:</strong></p>
 * <ul>
 *   <li>Spring AOP - Aspect-oriented programming framework</li>
 *   <li>SLF4J/Logback - Logging framework</li>
 *   <li>GrpcServerConfig - gRPC server configuration</li>
 *   <li>AuthGrpcServiceImpl - gRPC JWT service implementation</li>
 * </ul>
 *
 * <p><strong>Main responsibilities:</strong></p>
 * <ul>
 *   <li>Logging gRPC server start/stop events</li>
 *   <li>Auditing incoming RPC calls (GenerateTemporaryToken)</li>
 *   <li>Capturing gRPC processing errors</li>
 *   <li>Inter-module traceability (Members → Auth)</li>
 *   <li>Sensitive data sanitization (emails, tokens)</li>
 * </ul>
 *
 * <p><strong>Typical use cases:</strong></p>
 * <ul>
 *   <li>Security audit of inter-service communications</li>
 *   <li>gRPC performance monitoring (latency, throughput)</li>
 *   <li>Debugging communication issues</li>
 *   <li>Analyzing call patterns between modules</li>
 * </ul>
 *
 * <p><strong>Security:</strong></p>
 * <ul>
 *   <li>Partial email masking (GDPR compliance)</li>
 *   <li>No logging of complete tokens</li>
 *   <li>Automatic sensitive data sanitization</li>
 * </ul>
 *
 * <p><strong>Guarantees:</strong> Thread-safe, asynchronous logging, minimal performance impact.</p>
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
    // Pointcuts - Interception point definitions
    // ========================================================================

    /**
     * Pointcut for gRPC server startup.
     * <p>
     * Intercepts the {@code start()} method to audit server startups.
     * </p>
     */
    @Pointcut("execution(* com.ecclesiaflow.springsecurity.application.config.GrpcServerConfig.start())")
    public void grpcServerStart() {}

    /**
     * Pointcut for gRPC server shutdown.
     * <p>
     * Intercepts the {@code stop()} method to audit graceful shutdowns.
     * </p>
     */
    @Pointcut("execution(* com.ecclesiaflow.springsecurity.application.config.GrpcServerConfig.stop())")
    public void grpcServerStop() {}

    /**
     * Pointcut for all JWT service RPC calls.
     * <p>
     * Intercepts all public methods of {@code AuthGrpcServiceImpl}
     * to audit incoming gRPC communications.
     * </p>
     */
    @Pointcut("execution(* com.ecclesiaflow.springsecurity.io.grpc.server.AuthGrpcServiceImpl.*(..))")
    public void grpcServiceCalls() {}

    // ========================================================================
    // Advices - gRPC server start/stop
    // ========================================================================

    /**
     * Log before gRPC server startup.
     *
     * @param joinPoint join point containing call details
     */
    @Before("grpcServerStart()")
    public void logBeforeServerStart(JoinPoint joinPoint) {
        log.info("🚀 GRPC: Initializing gRPC server for Auth module...");
    }

    /**
     * Log after successful gRPC server startup.
     *
     * @param joinPoint join point
     */
    @AfterReturning("grpcServerStart()")
    public void logAfterServerStart(JoinPoint joinPoint) {
        log.info("✅ GRPC: gRPC server started successfully and ready to accept connections");
    }

    /**
     * Log errors during gRPC server startup.
     *
     * @param joinPoint join point
     * @param exception exception thrown during startup
     */
    @AfterThrowing(pointcut = "grpcServerStart()", throwing = "exception")
    public void logServerStartError(JoinPoint joinPoint, Exception exception) {
        log.error("❌ GRPC: Failed to start gRPC server - {}: {}", 
                exception.getClass().getSimpleName(), 
                SecurityMaskingUtils.sanitizeInfra(exception.getMessage()),
                exception);
    }

    /**
     * Log before gRPC server shutdown.
     *
     * @param joinPoint join point
     */
    @Before("grpcServerStop()")
    public void logBeforeServerStop(JoinPoint joinPoint) {
        log.info("GRPC: Initiating graceful shutdown of gRPC server...");
    }

    /**
     * Log after successful gRPC server shutdown.
     *
     * @param joinPoint join point
     */
    @AfterReturning("grpcServerStop()")
    public void logAfterServerStop(JoinPoint joinPoint) {
        log.info("GRPC: gRPC server stopped successfully");
    }

    // ========================================================================
    // Advices - RPC calls (GenerateTemporaryToken)
    // ========================================================================

    /**
     * Log before each incoming RPC call.
     * <p>
     * Captures basic call information for inter-module traceability.
     * Masks sensitive data (emails, tokens).
     * </p>
     *
     * @param joinPoint join point containing method name and arguments
     */
    @Before("grpcServiceCalls()")
    public void logBeforeRpcCall(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        
        // Log based on RPC type
        if ("generateTemporaryToken".equals(methodName)) {
            log.info("GRPC-RPC: Received {} request from Members module", methodName);
        } else if ("validateToken".equals(methodName)) {
            log.debug("[GRPC-RPC]: Received {} request", methodName);
        } else {
            log.debug("[GRPC-RPC]: Received {} request", methodName);
        }
    }

    /**
     * Log after successful RPC call.
     * <p>
     * Confirms successful processing of the RPC call.
     * </p>
     *
     * @param joinPoint join point
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
     * Log errors during RPC processing.
     * <p>
     * Captures exceptions thrown during gRPC call processing.
     * Differentiates business errors (IllegalArgumentException) from technical errors.
     * </p>
     *
     * @param joinPoint join point
     * @param exception thrown exception
     */
    @AfterThrowing(pointcut = "grpcServiceCalls()", throwing = "exception")
    public void logRpcCallError(JoinPoint joinPoint, Exception exception) {
        String methodName = joinPoint.getSignature().getName();
        
        // Validation errors (members error)
        if (exception instanceof IllegalArgumentException) {
            log.warn("GRPC-RPC: Invalid argument in {} - {}",
                    methodName, 
                    SecurityMaskingUtils.sanitizeInfra(exception.getMessage()),
                    exception);
        } 
        // Internal errors (server error)
        else {
            log.error("GRPC-RPC: Error in {} - {}: {}",
                    methodName,
                    exception.getClass().getSimpleName(), 
                    SecurityMaskingUtils.sanitizeInfra(exception.getMessage()),
                    exception);
        }
    }

}
