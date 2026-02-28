package com.ecclesiaflow.springsecurity.web.exception;

/**
 * Exception thrown when validating an invalid or expired token.
 */
public class InvalidTokenException extends RuntimeException {
    
    public InvalidTokenException(String message) {
        super(message);
    }
    
    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
