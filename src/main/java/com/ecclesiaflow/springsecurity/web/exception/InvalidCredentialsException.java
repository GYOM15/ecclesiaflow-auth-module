package com.ecclesiaflow.springsecurity.web.exception;

import org.springframework.security.authentication.BadCredentialsException;

/**
 * Exception métier levée lors d'une tentative d'authentification avec des identifiants invalides.
 * <p>
 * Cette exception étend {@link BadCredentialsException} de Spring Security pour s'intégrer
 * naturellement dans l'écosystème d'authentification. Elle est utilisée par les services
 * d'authentification pour signaler des erreurs d'identifiants.
 * </p>
 * 
 * <p><strong>Cas d'utilisation typiques :</strong></p>
 * <ul>
 *   <li>Email inexistant dans la base de données</li>
 *   <li>Mot de passe incorrect</li>
 *   <li>Compte désactivé ou verrouillé</li>
 *   <li>Tentatives d'authentification malveillantes</li>
 * </ul>
 * 
 * <p><strong>Garanties :</strong> Thread-safe, hérite du comportement Spring Security.</p>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
public class InvalidCredentialsException extends BadCredentialsException {
    
    /**
     * Crée une nouvelle exception avec un message d'erreur.
     * 
     * @param message le message décrivant l'erreur d'authentification, non null
     */
    public InvalidCredentialsException(String message) {
        super(message);
    }
    
    /**
     * Crée une nouvelle exception avec un message d'erreur et une cause.
     * 
     * @param message le message décrivant l'erreur d'authentification, non null
     * @param cause la cause sous-jacente de l'erreur, peut être null
     */
    public InvalidCredentialsException(String message, Throwable cause) {
        super(message, cause);
    }
}
