package com.ecclesiaflow.springsecurity.io.grpc.server;

import com.ecclesiaflow.grpc.auth.*;
import com.ecclesiaflow.springsecurity.web.exception.JwtProcessingException;
import com.ecclesiaflow.springsecurity.web.security.Jwt;
import com.ecclesiaflow.springsecurity.web.security.JwtProcessor;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Implémentation du service gRPC JWT.
 * <p>
 * Cette classe implémente le service {@code ecclesiaflow.auth.AuthService} défini
 * dans le fichier proto. Elle sert de pont (adapter) entre le protocole gRPC et
 * les opérations JWT du module d'authentification.
 * </p>
 *
 * <p><strong>Rôle architectural :</strong> Adapter - gRPC to JWT Operations</p>
 *
 * <p><strong>Responsabilités :</strong></p>
 * <ul>
 *   <li>Réception des appels gRPC depuis d'autres modules</li>
 *   <li>Validation des requêtes gRPC (format, données requises)</li>
 *   <li>Conversion messages Protobuf ↔ objets métier Java</li>
 *   <li>Délégation aux services JWT existants ({@link Jwt}, {@link JwtProcessor})</li>
 *   <li>Gestion d'erreurs et conversion vers Status gRPC appropriés</li>
 *   <li>Logging structuré pour traçabilité inter-modules</li>
 * </ul>
 *
 * <p><strong>Clean Architecture :</strong></p>
 * <pre>
 * gRPC Request → JwtGrpcServiceImpl (Adapter) → Jwt (Business) → JwtProcessor
 * </pre>
 *
 * <p><strong>Gestion d'erreurs :</strong></p>
 * <ul>
 *   <li>INVALID_ARGUMENT - Données invalides (email, UUID, etc.)</li>
 *   <li>INTERNAL - Erreur interne lors du traitement JWT</li>
 *   <li>UNAUTHENTICATED - Token invalide ou expiré</li>
 *   <li>UNAVAILABLE - Service temporairement indisponible</li>
 * </ul>
 *
 * <p><strong>Sécurité :</strong></p>
 * <ul>
 *   <li>Validation stricte des entrées (format email, UUID)</li>
 *   <li>Pas de leak d'informations sensibles dans les erreurs</li>
 *   <li>Logging sanitized (pas de tokens en clair)</li>
 * </ul>
 *
 * @author EcclesiaFlow Team
 * @since 1.0.0
 * @see AuthServiceGrpc.AuthServiceImplBase
 * @see Jwt
 * @see JwtProcessor
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "grpc.enabled", havingValue = "true", matchIfMissing = false)
public class JwtGrpcServiceImpl extends AuthServiceGrpc.AuthServiceImplBase {

    private final Jwt jwt;

    /**
     * Génère un token temporaire JWT pour permettre la définition du mot de passe.
     * <p>
     * Cette méthode est appelée par le module Members après qu'un utilisateur
     * ait confirmé son email. Le token temporaire généré permet à l'utilisateur
     * de définir son mot de passe initial (expire en 15 minutes).
     * </p>
     *
     * <p><strong>Flow de communication :</strong></p>
     * <pre>
     * 1. Members: Utilisateur confirme email via token de confirmation
     * 2. Members → Auth (gRPC): GenerateTemporaryToken(email, memberId)
     * 3. Auth: Génère JWT temporaire avec claims "type=temporary, purpose=password_setup"
     * 4. Auth → Members (gRPC): TemporaryTokenResponse avec JWT
     * 5. Members → Utilisateur: Retourne token dans réponse HTTP
     * 6. Utilisateur utilise token pour définir mot de passe
     * </pre>
     *
     * @param request contient email et memberId du membre confirmé
     * @param responseObserver observer pour envoyer la réponse asynchrone
     */
    @Override
    public void generateTemporaryToken(
            TemporaryTokenRequest request,
            StreamObserver<TemporaryTokenResponse> responseObserver) {

        String email = request.getEmail();
        String memberIdStr = request.getMemberId();

        try {
            // Validation des entrées
            validateEmail(email);
            UUID memberId = validateAndParseUUID(memberIdStr);

            // Délégation au service métier
            String temporaryToken = jwt.generateTemporaryToken(email, memberId, "password_setup");

            // Construction de la réponse Protobuf
            TemporaryTokenResponse response = TemporaryTokenResponse.newBuilder()
                    .setTemporaryToken(temporaryToken)
                    .setExpiresInSeconds(900) // 15 minutes
                    .build();

            // Envoi de la réponse
            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(e.getMessage())
                    .withCause(e)
                    .asRuntimeException());

        } catch (JwtProcessingException e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to generate temporary token")
                    .withCause(e)
                    .asRuntimeException());

        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("An unexpected error occurred")
                    .asRuntimeException());
        }
    }

    // ========================================================================
    // Méthodes utilitaires privées
    // ========================================================================

    /**
     * Valide le format d'une adresse email.
     *
     * @param email l'email à valider
     * @throws IllegalArgumentException si l'email est invalide
     */
    private void validateEmail(String email) {
        // Protobuf ne retourne jamais null, mais une chaîne vide par défaut
        if (email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }
        
        // Validation basique du format email
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException("Invalid email format");
        }
    }

    /**
     * Valide et parse un UUID depuis une chaîne de caractères.
     *
     * @param uuidStr la chaîne UUID à parser
     * @return le UUID parsé
     * @throws IllegalArgumentException si l'UUID est invalide
     */
    private UUID validateAndParseUUID(String uuidStr) {
        // Protobuf ne retourne jamais null, mais une chaîne vide par défaut
        if (uuidStr.isBlank()) {
            throw new IllegalArgumentException("member_id" + " cannot be empty");
        }
        
        try {
            return UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "member_id" + " must be a valid UUID format: " + e.getMessage());
        }
    }

}
