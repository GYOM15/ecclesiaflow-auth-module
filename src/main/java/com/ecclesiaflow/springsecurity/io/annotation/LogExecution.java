package com.ecclesiaflow.springsecurity.io.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation pour marquer les méthodes nécessitant un logging détaillé d'exécution.
 * <p>
 * Cette annotation permet de déclencher automatiquement un logging des appels de méthodes
 * via la programmation orientée aspect (AOP). Offre un contrôle granulaire sur le niveau
 * de log, l'inclusion des paramètres et la durée d'exécution.
 * </p>
 * 
 * <p><strong>Rôle architectural :</strong> Annotation AOP - Logging transversal</p>
 * 
 * <p><strong>Dépendances critiques :</strong></p>
 * <ul>
 *   <li>Spring AOP - Traitement des aspects de logging</li>
 *   <li>Framework de logging (SLF4J/Logback) - Sortie des logs</li>
 * </ul>
 * 
 * <p><strong>Cas d'utilisation typiques :</strong></p>
 * <ul>
 *   <li>Debugging des méthodes critiques d'authentification</li>
 *   <li>Monitoring des performances des services métier</li>
 *   <li>Audit des opérations sensibles (inscription, connexion)</li>
 * </ul>
 * 
 * <p><strong>Garanties :</strong> Thread-safe, impact minimal sur les performances.</p>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogExecution {
    
    /**
     * Message personnalisé pour le log.
     * <p>
     * Si non spécifié, utilise le nom de la méthode par défaut.
     * </p>
     * 
     * @return le message personnalisé ou chaîne vide pour utiliser le défaut
     */
    String value() default "";
    
    /**
     * Niveau de log pour cette exécution.
     * <p>
     * Détermine l'importance du message de log. Les valeurs supportées
     * sont : INFO, WARN, ERROR, DEBUG.
     * </p>
     * 
     * @return le niveau de log, INFO par défaut
     */
    String level() default "INFO";
    
    /**
     * Indique si les paramètres de la méthode doivent être inclus dans le log.
     * <p>
     * Attention : peut exposer des données sensibles comme les mots de passe.
     * À utiliser avec précaution sur les méthodes d'authentification.
     * </p>
     * 
     * @return true pour inclure les paramètres, false par défaut
     */
    boolean includeParams() default false;
    
    /**
     * Indique si la durée d'exécution doit être mesurée et loggée.
     * <p>
     * Utile pour le monitoring des performances et l'identification
     * des goulots d'étranglement dans l'application.
     * </p>
     * 
     * @return true pour mesurer la durée, true par défaut
     */
    boolean includeExecutionTime() default true;
}
