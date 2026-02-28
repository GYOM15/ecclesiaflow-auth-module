package com.ecclesiaflow.springsecurity.application.logging.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods requiring detailed execution logging.
 * <p>
 * This annotation triggers automatic method call logging via aspect-oriented
 * programming (AOP). Provides granular control over log level, parameter inclusion,
 * and execution time measurement.
 * </p>
 * 
 * <p><strong>Architectural role:</strong> AOP annotation - Cross-cutting logging</p>
 * 
 * <p><strong>Critical dependencies:</strong></p>
 * <ul>
 *   <li>Spring AOP - Logging aspect processing</li>
 *   <li>Logging framework (SLF4J/Logback) - Log output</li>
 * </ul>
 * 
 * <p><strong>Typical use cases:</strong></p>
 * <ul>
 *   <li>Debugging critical authentication methods</li>
 *   <li>Monitoring business service performance</li>
 *   <li>Auditing sensitive operations (registration, login)</li>
 * </ul>
 * 
 * <p><strong>Guarantees:</strong> Thread-safe, minimal performance impact.</p>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogExecution {
    
    /**
     * Custom log message.
     * <p>
     * If not specified, uses the method name by default.
     * </p>
     * 
     * @return the custom message or empty string to use the default
     */
    String value() default "";
    
    /**
     * Log level for this execution.
     * <p>
     * Determines the log message importance. Supported values
     * are: INFO, WARN, ERROR, DEBUG.
     * </p>
     * 
     * @return the log level, INFO by default
     */
    String level() default "INFO";
    
    /**
     * Whether method parameters should be included in the log.
     * <p>
     * Warning: may expose sensitive data such as passwords.
     * Use with caution on authentication methods.
     * </p>
     * 
     * @return true to include parameters, false by default
     */
    boolean includeParams() default false;
    
    /**
     * Whether execution time should be measured and logged.
     * <p>
     * Useful for performance monitoring and identifying
     * bottlenecks in the application.
     * </p>
     * 
     * @return true to measure duration, true by default
     */
    boolean includeExecutionTime() default true;
}
