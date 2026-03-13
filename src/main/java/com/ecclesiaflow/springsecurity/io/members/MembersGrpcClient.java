package com.ecclesiaflow.springsecurity.io.members;

import com.ecclesiaflow.grpc.members.*;
import com.ecclesiaflow.springsecurity.business.domain.member.MembersClient;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * gRPC adapter for communicating with the Members module.
 * <p>
 * Implements the {@link MembersClient} port using gRPC transport.
 * Encapsulates gRPC complexity and provides a type-safe API for business services.
 * </p>
 *
 * <p><strong>Architectural role:</strong> Adapter - gRPC Client implementing domain port</p>
 *
 * <p><strong>Responsibilities:</strong></p>
 * <ul>
 *   <li>gRPC stub creation and lifecycle management</li>
 *   <li>RPC calls to Members module</li>
 *   <li>Protobuf to Java type conversion</li>
 *   <li>gRPC error handling and mapping to business exceptions</li>
 *   <li>Per-call timeout configuration</li>
 * </ul>
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

    private static final int DEFAULT_TIMEOUT_SECONDS = 5;

    @Override
    public boolean isEmailNotConfirmed(String email) {
        MembersServiceGrpc.MembersServiceBlockingStub stub = MembersServiceGrpc
                .newBlockingStub(membersGrpcChannel)
                .withDeadlineAfter(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        try {
            ConfirmationStatusRequest request = ConfirmationStatusRequest.newBuilder()
                    .setEmail(email)
                    .build();

            ConfirmationStatusResponse response = stub.getMemberConfirmationStatus(request);

            return !response.getMemberExists() || !response.getIsConfirmed();

        } catch (StatusRuntimeException e) {
            throw handleGrpcException(e, "isEmailNotConfirmed");
        }
    }

    @Override
    @Retryable(
            retryFor = StatusRuntimeException.class,
            noRetryFor = IllegalArgumentException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public boolean notifyAccountActivated(java.util.UUID memberId, String keycloakUserId) {
        MembersServiceGrpc.MembersServiceBlockingStub stub = MembersServiceGrpc
                .newBlockingStub(membersGrpcChannel)
                .withDeadlineAfter(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        try {
            AccountActivatedRequest request = AccountActivatedRequest.newBuilder()
                    .setMemberId(memberId.toString())
                    .setKeycloakUserId(keycloakUserId)
                    .build();

            AccountActivatedResponse response = stub.notifyAccountActivated(request);

            if (!response.getSuccess()) {
                throw new RuntimeException("Members module rejected activation: " + response.getMessage());
            }

            return true;

        } catch (StatusRuntimeException e) {
            throw handleGrpcException(e, "notifyAccountActivated");
        }
    }

    @Override
    @Retryable(
            retryFor = StatusRuntimeException.class,
            noRetryFor = IllegalArgumentException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public boolean notifyLocalCredentialsAdded(String keycloakUserId) {
        MembersServiceGrpc.MembersServiceBlockingStub stub = MembersServiceGrpc
                .newBlockingStub(membersGrpcChannel)
                .withDeadlineAfter(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        try {
            LocalCredentialsAddedRequest request = LocalCredentialsAddedRequest.newBuilder()
                    .setKeycloakUserId(keycloakUserId)
                    .build();

            LocalCredentialsAddedResponse response = stub.notifyLocalCredentialsAdded(request);

            if (!response.getSuccess()) {
                throw new RuntimeException("Members module rejected credentials update: " + response.getMessage());
            }

            return true;

        } catch (StatusRuntimeException e) {
            throw handleGrpcException(e, "notifyLocalCredentialsAdded");
        }
    }

    /**
     * Maps gRPC exceptions to appropriate business exceptions.
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
     * Custom exception indicating the Members service is unavailable.
     */
    public static class MembersServiceUnavailableException extends RuntimeException {
        public MembersServiceUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
