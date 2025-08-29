package com.ecclesiaflow.springsecurity.web.exception;

/**
 * Exception levée lors d'une requête avec des paramètres invalides
 */
public class InvalidRequestException extends RuntimeException {
    
    public InvalidRequestException(String message) {
        super(message);
    }
    
    public InvalidRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
