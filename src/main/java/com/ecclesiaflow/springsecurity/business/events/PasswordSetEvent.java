package com.ecclesiaflow.springsecurity.business.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Événement déclenché lorsqu'un utilisateur définit son mot de passe initial.
 * <p>
 * Cet événement est publié après la définition réussie du mot de passe initial
 * et l'activation du compte. Il permet de déclencher des actions asynchrones
 * comme l'envoi d'un email de bienvenue.
 * </p>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
@Getter
public class PasswordSetEvent extends ApplicationEvent {
    
    private final String email;
    
    public PasswordSetEvent(Object source, String email) {
        super(source);
        this.email = email;
    }
}
