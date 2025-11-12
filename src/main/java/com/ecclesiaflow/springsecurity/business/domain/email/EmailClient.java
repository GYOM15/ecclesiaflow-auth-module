package com.ecclesiaflow.springsecurity.business.domain.email;

import java.util.UUID;

/**
 * Port (interface) pour communiquer avec le module Email.
 * <p>
 * Cette interface définit le contrat pour l'envoi d'emails depuis le module Auth.
 * L'implémentation peut être soit REST (WebClient), soit gRPC selon la configuration.
 * </p>
 * <p>
 * <strong>Rôle architectural :</strong> Port (Hexagonal Architecture)
 * </p>
 * <p>
 * <strong>Implémentations possibles :</strong>
 * <ul>
 *   <li>EmailClientImpl (WebClient REST) - Simple et rapide</li>
 *   <li>EmailGrpcClient (gRPC) - Performance optimale</li>
 * </ul>
 * </p>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
public interface EmailClient {
    
    /**
     * Envoie un email de réinitialisation de mot de passe.
     * <p>
     * L'email contient un lien avec un token temporaire valide 15 minutes.
     * </p>
     * 
     * @param toEmail Email destinataire
     * @param resetLink Lien complet de reset : https://app.ecclesiaflow.com/reset-password?token=...
     * @return UUID de l'email envoyé (pour tracking)
     */
    UUID sendPasswordResetEmail(String toEmail, String resetLink);
    
    /**
     * Envoie une notification que le mot de passe a été changé.
     * <p>
     * Email simple pour informer l'utilisateur que son mot de passe a été modifié.
     * Si ce n'est pas lui, il doit contacter le support.
     * </p>
     * 
     * @param toEmail Email destinataire
     * @return UUID de l'email envoyé
     */
    UUID sendPasswordChangedNotification(String toEmail);
    
    /**
     * Envoie un email de bienvenue lors de la définition du mot de passe initial.
     * <p>
     * Email envoyé après que l'utilisateur a défini son mot de passe pour la première fois
     * et que son compte est maintenant activé.
     * </p>
     * 
     * @param toEmail Email destinataire
     * @return UUID de l'email envoyé
     */
    UUID sendWelcomeEmail(String toEmail);
}
