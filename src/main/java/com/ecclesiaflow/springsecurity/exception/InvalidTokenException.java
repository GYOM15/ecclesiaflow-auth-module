package com.ecclesiaflow.springsecurity.exception;

/**
 * Exception levée lors de la validation d'un token invalide ou expiré
 */
public class InvalidTokenException extends RuntimeException {
    
    public InvalidTokenException(String message) {
        super(message);
    }
    
    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
