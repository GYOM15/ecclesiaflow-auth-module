package com.ecclesiaflow.springsecurity.application.handlers;

import com.ecclesiaflow.springsecurity.business.domain.email.EmailClient;
import com.ecclesiaflow.springsecurity.business.events.PasswordChangedEvent;
import com.ecclesiaflow.springsecurity.business.events.PasswordResetEvent;
import com.ecclesiaflow.springsecurity.business.events.PasswordResetRequestedEvent;
import com.ecclesiaflow.springsecurity.business.events.PasswordSetEvent;
import com.ecclesiaflow.springsecurity.web.security.Jwt;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Handler d'événements métier pour la gestion des mots de passe.
 * <p>
 * Cette classe écoute les événements métier (Domain Events) provenant de la couche Business
 * et orchestre les actions techniques nécessaires (génération JWT, construction d'URLs, envoi d'emails).
 * </p>
 * 
 * <p><strong>Responsabilités :</strong></p>
 * <ul>
 *   <li>Transformation des événements métier en actions techniques</li>
 *   <li>Génération des tokens JWT et URLs (concerne Application/Web)</li>
 *   <li>Orchestration des appels vers l'infrastructure (EmailClient)</li>
 *   <li>Exécution asynchrone pour ne pas bloquer les transactions métier</li>
 * </ul>
 * 
 * <p><strong>Architecture :</strong></p>
 * <pre>
 * Business (Services) → publie Domain Events
 *         ↓
 * Application (Handler) → écoute + transforme + orchestre
 *         ↓
 * Infrastructure (EmailClient) → envoie emails via gRPC
 * </pre>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class PasswordEventHandler {

    private final EmailClient emailClient;
    private final Jwt jwt;
    
    @Value("${frontend.base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    /**
     * Gère l'événement de définition du mot de passe initial.
     * <p>
     * Envoie un email de bienvenue au nouvel utilisateur de manière asynchrone.
     * En cas d'échec, l'exception est capturée pour ne pas impacter la transaction métier.
     * Le logging est géré par les aspects AOP.
     * </p>
     * 
     * @param event Événement contenant l'email du nouvel utilisateur
     */
    @EventListener
    @Async
    public void handlePasswordSet(PasswordSetEvent event) {
        try {
            emailClient.sendWelcomeEmail(event.getEmail());
        } catch (Exception e) {
            // Exception capturée pour ne pas impacter la transaction métier
            // Le logging est géré par les aspects AOP
        }
    }
    
    /**
     * Gère l'événement de changement de mot de passe.
     * <p>
     * Envoie une notification de sécurité pour informer l'utilisateur
     * que son mot de passe a été modifié.
     * </p>
     * 
     * @param event Événement contenant l'email de l'utilisateur
     */
    @EventListener
    @Async
    public void handlePasswordChanged(PasswordChangedEvent event) {
        try {
            emailClient.sendPasswordChangedNotification(event.getEmail());
        } catch (Exception e) {
            // Exception capturée pour ne pas impacter la transaction métier
        }
    }
    
    /**
     * Gère l'événement de demande de réinitialisation de mot de passe.
     * <p>
     * Processus :
     * 1. Génération d'un token JWT temporaire (Application concern)
     * 2. Construction du lien de réinitialisation avec l'URL frontend
     * 3. Envoi de l'email avec le lien
     * </p>
     * 
     * @param event Événement contenant l'email et le memberId
     */
    @EventListener
    @Async
    public void handlePasswordResetRequested(PasswordResetRequestedEvent event) {
        try {
            // Génération du token temporaire (Application/Web concern)
            String temporaryToken = jwt.generateTemporaryToken(
                event.getEmail(), 
                event.getMemberId(), 
                "password_reset"
            );
            
            // Construction du lien de réinitialisation
            String resetLink = buildResetLink(temporaryToken);
            
            // Envoi de l'email via Infrastructure
            emailClient.sendPasswordResetEmail(event.getEmail(), resetLink);
        } catch (Exception e) {
            // Exception capturée pour ne pas impacter la transaction métier
        }
    }
    
    /**
     * Gère l'événement de réinitialisation de mot de passe.
     * <p>
     * Envoie une notification de confirmation que le mot de passe a été réinitialisé.
     * </p>
     * 
     * @param event Événement contenant l'email de l'utilisateur
     */
    @EventListener
    @Async
    public void handlePasswordReset(PasswordResetEvent event) {
        try {
            emailClient.sendPasswordChangedNotification(event.getEmail());
        } catch (Exception e) {
            // Exception capturée pour ne pas impacter la transaction métier
        }
    }
    
    /**
     * Construit le lien de réinitialisation de mot de passe.
     * 
     * @param token Token temporaire JWT
     * @return URL complète du formulaire de réinitialisation
     */
    private String buildResetLink(String token) {
        return String.format("%s/reset-password?token=%s", frontendBaseUrl, token);
    }
}
