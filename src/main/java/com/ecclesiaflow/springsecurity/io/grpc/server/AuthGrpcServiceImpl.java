package com.ecclesiaflow.springsecurity.io.grpc.server;

import com.ecclesiaflow.grpc.auth.*;
import com.ecclesiaflow.springsecurity.business.services.SetupTokenService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * gRPC service implementation for Auth module.
 * Handles token generation requests from Members module.
 *
 * @author EcclesiaFlow Team
 * @since 2.0.0
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "grpc.enabled", havingValue = "true", matchIfMissing = false)
public class AuthGrpcServiceImpl extends AuthServiceGrpc.AuthServiceImplBase {

    private final SetupTokenService setupTokenService;

    @Value("${auth.token.setup.ttl-hours:24}")
    private int setupTokenTtlHours;

    @Value("${auth.password.setup.endpoint:/ecclesiaflow/auth/password/setup}")
    private String passwordSetupEndpoint;

    /**
     * Generates an opaque temporary token for password setup.
     * Called by Members module after email confirmation.
     *
     * @param request          contains email and memberId
     * @param responseObserver observer for async response
     */
    @Override
    public void generateTemporaryToken(
            TemporaryTokenRequest request,
            StreamObserver<TemporaryTokenResponse> responseObserver) {

        String email = request.getEmail();
        String memberIdStr = request.getMemberId();

        try {
            validateEmail(email);
            UUID memberId = validateAndParseUUID(memberIdStr);

            String temporaryToken = setupTokenService.generateSetupToken(email, memberId);

            TemporaryTokenResponse response = TemporaryTokenResponse.newBuilder()
                    .setTemporaryToken(temporaryToken)
                    .setExpiresInSeconds(setupTokenTtlHours * 3600)
                    .setPasswordEndpoint(passwordSetupEndpoint)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (IllegalArgumentException e) {
            handleInvalidArgument(responseObserver, e);

        } catch (Exception e) {
            handleInternalError(responseObserver);
        }
    }

    private static void handleInternalError(StreamObserver<TemporaryTokenResponse> responseObserver) {
        responseObserver.onError(Status.INTERNAL
                .withDescription("Failed to generate temporary token")
                .asRuntimeException());
    }

    private static void handleInvalidArgument(StreamObserver<TemporaryTokenResponse> responseObserver, IllegalArgumentException e) {
        responseObserver.onError(Status.INVALID_ARGUMENT
                .withDescription(e.getMessage())
                .asRuntimeException());
    }

    // ========================================================================
    // Private utility methods
    // ========================================================================

    /**
     * Validates email address format.
     *
     * @param email the email to validate
     * @throws IllegalArgumentException if the email is invalid
     */
    private void validateEmail(String email) {
        if (email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }
        
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException("Invalid email format");
        }
    }

    /**
     * Validates and parses a UUID from a string.
     *
     * @param uuidStr the UUID string to parse
     * @return the parsed UUID
     * @throws IllegalArgumentException if the UUID is invalid
     */
    private UUID validateAndParseUUID(String uuidStr) {
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
