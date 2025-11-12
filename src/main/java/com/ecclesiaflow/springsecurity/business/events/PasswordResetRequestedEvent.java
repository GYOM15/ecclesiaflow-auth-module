package com.ecclesiaflow.springsecurity.business.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

/**
 * Événement métier déclenché lorsqu'un utilisateur demande une réinitialisation de mot de passe.
 * <p>
 * Cet événement représente un fait métier pur, sans données techniques (JWT, URL, etc.).
 * La génération du token et du lien est déléguée à la couche Application (Handler).
 * </p>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
@Getter
public class PasswordResetRequestedEvent extends ApplicationEvent {
    
    private final String email;
    private final UUID memberId;
    
    public PasswordResetRequestedEvent(Object source, String email, UUID memberId) {
        super(source);
        this.email = email;
        this.memberId = memberId;
    }
}
