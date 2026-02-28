package com.ecclesiaflow.springsecurity.application.logging.aspect;

import com.ecclesiaflow.springsecurity.application.logging.annotation.LogExecution;
import com.ecclesiaflow.springsecurity.application.logging.SecurityMaskingUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

/**
 * AOP aspect responsible for automatic logging of critical EcclesiaFlow operations.
 * <p>
 * This class implements aspect-oriented programming to automatically capture
 * method calls in services and controllers, and generate detailed logs
 * for monitoring, debugging, and auditing system operations.
 * </p>
 * 
 *
 * <p><strong>Critical dependencies:</strong></p>
 * <ul>
 *   <li>Spring AOP - Aspect-oriented programming framework</li>
 *   <li>SLF4J/Logback - Logging framework</li>
 *   <li>Annotation {@link LogExecution} - Method logging marker</li>
 * </ul>
 * 
 * <p><strong>Main responsibilities:</strong></p>
 * <ul>
 *   <li>Automatic logging of method entry/exit</li>
 *   <li>Exception and system error capture</li>
 *   <li>Execution time measurement for monitoring</li>
 *   <li>Configurable logging via @LogExecution annotation</li>
 * </ul>
 * 
 * <p><strong>Typical use cases:</strong></p>
 * <ul>
 *   <li>Debugging authentication flows</li>
 *   <li>Monitoring service performance</li>
 *   <li>Auditing sensitive operations (registration, login)</li>
 *   <li>Error traceability in production</li>
 * </ul>
 * 
 * <p><strong>Guarantees:</strong> Thread-safe, minimal performance impact, asynchronous logging.</p>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Component
public class LoggingAspect {

    /**
     * Pointcut for all service methods.
     */
    @Pointcut("execution(* com.ecclesiaflow.springsecurity.business.services..*(..))")
    public void serviceMethods() {}

    /**
     * Pointcut for all controller methods.
     */
    @Pointcut("execution(* com.ecclesiaflow.springsecurity.web.controller..*(..))")
    public void controllerMethods() {}

    /**
     * Pointcut for methods annotated with @LogExecution.
     */
    @Pointcut("@annotation(com.ecclesiaflow.springsecurity.application.logging.annotation.LogExecution)")
    public void logExecutionAnnotatedMethods() {}

    /**
     * Generic logging for service methods with performance tracking.
     */
    @Around("serviceMethods()")
    public Object logServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        return logMethodExecution(joinPoint);
    }

    /**
     * Logging for methods annotated with @LogExecution (flexible configuration).
     */
    @Around("logExecutionAnnotatedMethods() && @annotation(logExecution)")
    public Object logAnnotatedMethods(ProceedingJoinPoint joinPoint, LogExecution logExecution) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        long startTime = System.currentTimeMillis();
        
        String message = logExecution.value().isEmpty() ? 
            String.format("%s.%s", className, methodName) : logExecution.value();
        
        // Log parameters if requested
        if (logExecution.includeParams()) {
            Object[] args = joinPoint.getArgs();
            log.info("Start: {} - Params: {}", message, SecurityMaskingUtils.maskArgs(args));
        } else {
            log.info("Start: {}", message);
        }
        
        try {
            Object result = joinPoint.proceed();
            
            if (logExecution.includeExecutionTime()) {
                long executionTime = System.currentTimeMillis() - startTime;
                log.info("Success: {} ({}ms)", message, executionTime);
            } else {
                log.info("Success: {}", message);
            }
            
            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("Failure: {} ({}ms) - {}", message, executionTime, e.getMessage());
            throw e;
        }
    }

    /**
     * Log controller calls (DEBUG level to avoid spam).
     */
    @Before("controllerMethods()")
    public void logControllerAccess(JoinPoint joinPoint) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        log.debug("API: {}.{}", className, methodName);
    }

    /**
     * Utility method for generic method logging.
     */
    private Object logMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        long startTime = System.currentTimeMillis();
        
        log.debug("{}: Start {}.{}", "SERVICE", className, methodName);
        
        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;
            
            if (executionTime > 1000) { // Log if > 1 second
                log.warn("{}: {}.{} - Slow execution ({}ms)", "SERVICE", className, methodName, executionTime);
            } else {
                log.debug("{}: {}.{} - Success ({}ms)", "SERVICE", className, methodName, executionTime);
            }
            
            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("{}: {}.{} - Failure ({}ms): {}", "SERVICE", className, methodName, executionTime, e.getMessage());
            throw e;
        }
    }

    /**
     * Log unhandled exceptions in services and controllers.
     */
    @AfterThrowing(pointcut = "serviceMethods() || controllerMethods()", throwing = "exception")
    public void logUnhandledException(JoinPoint joinPoint, Throwable exception) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        
        log.error("Unhandled exception in {}.{}: {} - {}", 
                className, methodName, exception.getClass().getSimpleName(), exception.getMessage());
    }
}
