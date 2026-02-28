package com.ecclesiaflow.springsecurity.business.exceptions;

/**
 * Exception thrown when a setup/reset token is invalid, expired, or already consumed.
 */
public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException(String message) {
        super(message);
    }
}
