package com.ecclesiaflow.springsecurity.business.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Événement déclenché lorsqu'un utilisateur change son mot de passe.
 * <p>
 * Cet événement est publié après le changement réussi du mot de passe.
 * Il permet de déclencher des actions asynchrones comme l'envoi d'un email
 * de notification pour des raisons de sécurité.
 * </p>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
@Getter
public class PasswordChangedEvent extends ApplicationEvent {
    
    private final String email;
    
    public PasswordChangedEvent(Object source, String email) {
        super(source);
        this.email = email;
    }
}
