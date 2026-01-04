package com.ecclesiaflow.springsecurity.application.logging.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import static com.ecclesiaflow.springsecurity.application.logging.SecurityMaskingUtils.sanitizeInfra;


/**
 * Aspect AOP dédié au logging des appels gRPC sortants du module Auth.
 * <p>
 * Cette classe implémente un aspect spécialisé dans le logging des communications
 * gRPC entre le module Auth et le module Members. Elle capture les appels sortants,
 * les erreurs de communication et les métriques de performance.
 * </p>
 *
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Component
@ConditionalOnProperty(name = "grpc.enabled", havingValue = "true", matchIfMissing = false)
public class GrpcClientLoggingAspect {

    // ========================================================================
    // Pointcuts
    // ========================================================================

    @Pointcut("execution(* com.ecclesiaflow.springsecurity.application.config.GrpcClientConfig.shutdown())")
    public void grpcChannelShutdown() {}

    @Pointcut("execution(* com.ecclesiaflow.springsecurity.io.members.MembersGrpcClient.*(..))")
    public void grpcClientCalls() {}

    // ========================================================================
    // Advices - Shutdown
    // ========================================================================

    @Before("grpcChannelShutdown()")
    public void logBeforeChannelShutdown(JoinPoint joinPoint) {
        log.info("GRPC-CLIENT: Initiating graceful shutdown of gRPC channel to Members service...");
    }

    @AfterReturning("grpcChannelShutdown()")
    public void logAfterChannelShutdown(JoinPoint joinPoint) {
        log.info("GRPC-CLIENT: gRPC channel to Members service closed successfully");
    }

    @AfterThrowing(pointcut = "grpcChannelShutdown()", throwing = "exception")
    public void logChannelShutdownError(JoinPoint joinPoint, Exception exception) {
        log.error("GRPC-CLIENT: Error while closing gRPC channel - {}: {}",
                exception.getClass().getSimpleName(), 
                sanitizeInfra(exception.getMessage()));
    }

    // ========================================================================
    // Advices - Appels RPC sortants
    // ========================================================================

    @Before("grpcClientCalls()")
    public void logBeforeRpcCall(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        
        if ("isEmailNotConfirmed".equals(methodName)) {
            log.info("GRPC-CLIENT: Calling Members.{} via gRPC", methodName);
        } else {
            log.debug("GRPC-CLIENT: Calling Members.{} via gRPC", methodName);
        }
    }

    @AfterReturning(pointcut = "grpcClientCalls()", returning = "result")
    public void logAfterSuccessfulRpcCall(JoinPoint joinPoint, Object result) {
        String methodName = joinPoint.getSignature().getName();
        
        if ("isEmailNotConfirmed".equals(methodName)) {
            log.info("GRPC-CLIENT: Members.{} completed successfully", methodName);
        } else {
            log.debug("GRPC-CLIENT: Members.{} completed successfully", methodName);
        }
    }

    @AfterThrowing(pointcut = "grpcClientCalls()", throwing = "exception")
    public void logRpcCallError(JoinPoint joinPoint, Exception exception) {
        String methodName = joinPoint.getSignature().getName();
        String exceptionType = exception.getClass().getSimpleName();
        
        if (exceptionType.contains("Unavailable") || exceptionType.contains("UNAVAILABLE")) {
            log.error("GRPC-CLIENT: Members service UNAVAILABLE during {} - {}",
                    methodName, 
                    sanitizeInfra(exception.getMessage()));
        } else if (exceptionType.contains("Timeout") || exceptionType.contains("DEADLINE_EXCEEDED")) {
            log.warn("GRPC-CLIENT: Timeout during {} - {}",
                    methodName, 
                    sanitizeInfra(exception.getMessage()));
        } else if (exception instanceof IllegalArgumentException) {
            log.warn("GRPC-CLIENT: Invalid argument in {} - {}",
                    methodName, 
                    sanitizeInfra(exception.getMessage()));
        } else {
            log.error("GRPC-CLIENT: Error during {} - {}: {}",
                    methodName,
                    exceptionType, 
                    sanitizeInfra(exception.getMessage()));
        }
    }
}
