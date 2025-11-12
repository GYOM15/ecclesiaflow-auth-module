package com.ecclesiaflow.springsecurity.business.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Événement déclenché lorsqu'un utilisateur réinitialise son mot de passe via un token.
 * <p>
 * Cet événement est publié après la réinitialisation réussie du mot de passe.
 * Il permet de déclencher des actions asynchrones comme l'envoi d'un email
 * de confirmation de réinitialisation.
 * </p>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
@Getter
public class PasswordResetEvent extends ApplicationEvent {
    
    private final String email;
    
    public PasswordResetEvent(Object source, String email) {
        super(source);
        this.email = email;
    }
}
