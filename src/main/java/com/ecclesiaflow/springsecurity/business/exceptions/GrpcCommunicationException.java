package com.ecclesiaflow.springsecurity.business.exceptions;

import io.grpc.Status;

/**
 * Exception levée lors d'une erreur de communication gRPC inter-modules.
 * <p>
 * Cette exception encapsule les erreurs gRPC et fournit un contexte métier clair
 * pour faciliter le diagnostic et le logging.
 * </p>
 * 
 * <p><strong>Cas d'utilisation :</strong></p>
 * <ul>
 *   <li>Service distant indisponible (UNAVAILABLE)</li>
 *   <li>Timeout de requête (DEADLINE_EXCEEDED)</li>
 *   <li>Arguments invalides (INVALID_ARGUMENT)</li>
 *   <li>Erreur interne du service (INTERNAL)</li>
 * </ul>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
public class GrpcCommunicationException extends RuntimeException {
    
    private final String targetService;
    private final String operation;
    private final Status.Code grpcStatusCode;

    public GrpcCommunicationException(String targetService, String operation, Status.Code grpcStatusCode, String message, Throwable cause) {
        super(message, cause);
        this.targetService = targetService;
        this.operation = operation;
        this.grpcStatusCode = grpcStatusCode;
    }
    
    public String getTargetService() {
        return targetService;
    }
    
    public String getOperation() {
        return operation;
    }
    
    public Status.Code getGrpcStatusCode() {
        return grpcStatusCode;
    }
    
    @Override
    public String getMessage() {
        return String.format("gRPC error [%s] calling %s.%s: %s", 
            grpcStatusCode != null ? grpcStatusCode.name() : "UNKNOWN", 
            targetService, 
            operation, 
            super.getMessage());
    }
}
