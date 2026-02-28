package com.ecclesiaflow.springsecurity.business.exceptions;

/**
 * Thrown when a compensation (rollback) operation fails during a distributed transaction.
 * For example, when deleting an orphaned Keycloak user fails after a downstream error.
 */
public class CompensationFailedException extends RuntimeException {

    public CompensationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
