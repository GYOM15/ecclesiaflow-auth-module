package com.ecclesiaflow.springsecurity.io.grpc.client;

import com.ecclesiaflow.grpc.members.*;
import com.ecclesiaflow.springsecurity.business.domain.member.MembersClient;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Client gRPC pour communiquer avec le module Members.
 * <p>
 * Cette classe implémente un client gRPC qui permet au module Auth
 * d'appeler les services du module Members via gRPC au lieu de REST/WebClient.
 * Elle encapsule la complexité des appels gRPC et fournit une API simple
 * et type-safe pour les services métier.
 * </p>
 *
 * <p><strong>Rôle architectural :</strong> Adapter - gRPC Client to Business Logic</p>
 *
 * <p><strong>Responsabilités :</strong></p>
 * <ul>
 *   <li>Création et gestion des stubs gRPC (blocking)</li>
 *   <li>Appels RPC vers le module Members (GetMemberConfirmationStatus)</li>
 *   <li>Conversion des réponses Protobuf vers types Java métier</li>
 *   <li>Gestion des erreurs gRPC et mapping vers exceptions métier</li>
 *   <li>Configuration des timeouts par appel</li>
 * </ul>
 *
 * <p><strong>Clean Architecture :</strong></p>
 * <pre>
 * Business Layer → MembersGrpcClient (Adapter) → gRPC Channel → Members Module
 * </pre>
 *
 * @author EcclesiaFlow Team
 * @since 1.0.0
 * @see MembersServiceGrpc
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "grpc.enabled", havingValue = "true", matchIfMissing = false)
public class MembersGrpcClient implements MembersClient {

    private final ManagedChannel membersGrpcChannel;

    // Timeout par défaut pour les appels gRPC
    private static final int DEFAULT_TIMEOUT_SECONDS = 5;

    /**
     * Vérifie si l'email d'un membre n'est PAS confirmé.
     * <p>
     * Cette méthode est appelée par le module Auth lors d'une tentative de connexion
     * pour vérifier que le membre a bien confirmé son email.
     * </p>
     *
     * <p><strong>Flow de communication :</strong></p>
     * <pre>
     * 1. Auth: Tentative de connexion avec email/password
     * 2. Auth → Members (gRPC): GetMemberConfirmationStatus(email)
     * 3. Members: Recherche membre et vérifie statut
     * 4. Members → Auth (gRPC): ConfirmationStatusResponse
     * 5. Auth: Autorise ou bloque connexion
     * </pre>
     *
     * @param email l'email du membre à vérifier (requis, validé côté serveur)
     * @return true si l'email n'est PAS confirmé (ou membre n'existe pas), false si confirmé
     * @throws MembersServiceUnavailableException si le service Members est indisponible
     * @throws RuntimeException si erreur inattendue
     */
    @Override
    public boolean isEmailNotConfirmed(String email) {
        // Création du stub avec timeout
        MembersServiceGrpc.MembersServiceBlockingStub stub = MembersServiceGrpc
                .newBlockingStub(membersGrpcChannel)
                .withDeadlineAfter(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        try {
            // Construction de la requête Protobuf
            ConfirmationStatusRequest request = ConfirmationStatusRequest.newBuilder()
                    .setEmail(email)
                    .build();

            // Appel RPC synchrone
            ConfirmationStatusResponse response = stub.getMemberConfirmationStatus(request);

            // Si le membre n'existe pas OU n'est pas confirmé → true (bloque connexion)
            // Si le membre existe ET est confirmé → false (autorise connexion)
            return !response.getMemberExists() || !response.getIsConfirmed();

        } catch (StatusRuntimeException e) {
            throw handleGrpcException(e, "isEmailNotConfirmed");
        }
    }

    // ========================================================================
    // Gestion des erreurs gRPC
    // ========================================================================

    /**
     * Convertit les exceptions gRPC en exceptions métier appropriées.
     *
     * @param e l'exception gRPC interceptée
     * @param methodName le nom de la méthode pour contexte d'erreur
     * @return l'exception métier appropriée
     */
    private RuntimeException handleGrpcException(StatusRuntimeException e, String methodName) {
        Status.Code code = e.getStatus().getCode();
        String description = e.getStatus().getDescription();

        return switch (code) {
            case UNAVAILABLE -> new MembersServiceUnavailableException(
                    "Members service is unavailable: " + description, e);
            
            case DEADLINE_EXCEEDED -> new RuntimeException(
                    "Timeout exceeded while calling " + methodName + ": " + description, e);
            
            case INVALID_ARGUMENT -> new IllegalArgumentException(
                    "Invalid argument in " + methodName + ": " + description, e);
            
            case NOT_FOUND -> new RuntimeException(
                    "Member not found during " + methodName + ": " + description, e);
            
            case INTERNAL -> new RuntimeException(
                    "Internal error in Members service during " + methodName + ": " + description, e);
            
            default -> new RuntimeException(
                    "Unexpected gRPC error during " + methodName + " [" + code + "]: " + description, e);
        };
    }

    /**
     * Exception personnalisée pour indiquer que le service Members est indisponible.
     */
    public static class MembersServiceUnavailableException extends RuntimeException {
        public MembersServiceUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
