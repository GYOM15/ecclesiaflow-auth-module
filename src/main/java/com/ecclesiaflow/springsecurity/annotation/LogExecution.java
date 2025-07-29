package com.ecclesiaflow.springsecurity.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation pour marquer les méthodes qui nécessitent un logging détaillé
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogExecution {
    
    /**
     * Message personnalisé pour le log
     */
    String value() default "";
    
    /**
     * Niveau de log (INFO, WARN, ERROR, DEBUG)
     */
    String level() default "INFO";
    
    /**
     * Inclure les paramètres dans le log
     */
    boolean includeParams() default false;
    
    /**
     * Inclure le temps d'exécution
     */
    boolean includeExecutionTime() default true;
}
