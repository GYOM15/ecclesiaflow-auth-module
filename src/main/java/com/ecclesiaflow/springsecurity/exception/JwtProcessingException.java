package com.ecclesiaflow.springsecurity.exception;

/**
 * Exception spécifique pour les erreurs de traitement JWT
 * Rend explicites les erreurs qui peuvent survenir lors des opérations JWT
 */
public class JwtProcessingException extends RuntimeException {
    
    public JwtProcessingException(String message) {
        super(message);
    }
    
    public JwtProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
