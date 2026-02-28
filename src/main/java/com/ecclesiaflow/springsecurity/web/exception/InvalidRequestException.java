package com.ecclesiaflow.springsecurity.web.exception;

/**
 * Exception thrown when a request contains invalid parameters.
 */
public class InvalidRequestException extends RuntimeException {
    
    public InvalidRequestException(String message) {
        super(message);
    }
    
    public InvalidRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
