package com.ecclesiaflow.springsecurity.web.exception;

import org.springframework.security.authentication.BadCredentialsException;

/**
 * Exception levée lors d'une tentative d'authentification avec des identifiants invalides
 */
public class InvalidCredentialsException extends BadCredentialsException {
    
    public InvalidCredentialsException(String message) {
        super(message);
    }
    
    public InvalidCredentialsException(String message, Throwable cause) {
        super(message, cause);
    }
}
