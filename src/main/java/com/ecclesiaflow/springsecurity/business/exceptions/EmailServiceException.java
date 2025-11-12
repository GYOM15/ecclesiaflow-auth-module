package com.ecclesiaflow.springsecurity.business.exceptions;

/**
 * Exception levée lors d'une erreur de communication avec le service Email.
 * <p>
 * Cette exception encapsule les erreurs liées à l'envoi d'emails via le module Email,
 * que ce soit par gRPC ou autre protocole.
 * </p>
 * 
 * <p><strong>Cas d'utilisation :</strong></p>
 * <ul>
 *   <li>Échec de connexion gRPC au module Email</li>
 *   <li>Timeout lors de l'envoi d'email</li>
 *   <li>Erreur de validation de template</li>
 *   <li>Service Email indisponible</li>
 * </ul>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
public class EmailServiceException extends RuntimeException {
    
    private final String emailAddress;
    private final EmailOperation operation;

    public EmailServiceException(String message, String emailAddress, EmailOperation operation, Throwable cause) {
        super(message, cause);
        this.emailAddress = emailAddress;
        this.operation = operation;
    }
    
    public String getEmailAddress() {
        return emailAddress;
    }
    
    public EmailOperation getOperation() {
        return operation;
    }
    
    /**
     * Type d'opération email qui a échoué.
     */
    public enum EmailOperation {
        PASSWORD_RESET("Réinitialisation de mot de passe"),
        PASSWORD_CHANGED("Notification de changement de mot de passe"),
        ACCOUNT_CONFIRMATION("Confirmation de compte"),
        WELCOME("Email de bienvenue");
        
        private final String description;
        
        EmailOperation(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    @Override
    public String getMessage() {
        return String.format("[%s] %s - Email: %s", 
            operation.getDescription(), 
            super.getMessage(), 
            emailAddress != null ? emailAddress : "N/A");
    }
}
