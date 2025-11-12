package com.ecclesiaflow.springsecurity.application.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Configuration pour l'exécution asynchrone des tâches.
 * <p>
 * Active le support de l'annotation @Async pour permettre l'exécution
 * asynchrone des méthodes, notamment pour l'envoi d'emails via les listeners d'événements.
 * </p>
 * 
 * <p><strong>Cas d'usage :</strong></p>
 * <ul>
 *   <li>Envoi d'emails sans bloquer les transactions principales</li>
 *   <li>Traitement d'événements en arrière-plan</li>
 *   <li>Amélioration de la réactivité de l'application</li>
 * </ul>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    // Configuration par défaut de Spring suffisante pour notre cas d'usage
    // Si besoin de personnaliser l'executor, ajouter un @Bean TaskExecutor
}
