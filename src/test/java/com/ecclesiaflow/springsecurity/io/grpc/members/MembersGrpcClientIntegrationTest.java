package com.ecclesiaflow.springsecurity.io.grpc.members;

import com.ecclesiaflow.grpc.members.ConfirmationStatusRequest;
import com.ecclesiaflow.grpc.members.ConfirmationStatusResponse;
import com.ecclesiaflow.grpc.members.MembersServiceGrpc;
import com.ecclesiaflow.springsecurity.io.members.MembersGrpcClient;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests d'intégration pour MembersGrpcClient.
 * <p>
 * Teste le members gRPC avec un vrai serveur Members mock via InProcessServer.
 * Couvre la communication réelle et les scénarios d'erreur.
 * </p>
 */
class MembersGrpcClientIntegrationTest {

    private Server server;
    private ManagedChannel channel;
    private MembersGrpcClient client;

    private static final String SERVER_NAME = "test-members-grpc";
    private static final String CONFIRMED_EMAIL = "confirmed@example.com";
    private static final String UNCONFIRMED_EMAIL = "unconfirmed@example.com";
    private static final String NOT_FOUND_EMAIL = "notfound@example.com";
    private static final String UNAVAILABLE_EMAIL = "unavailable@example.com";
    private static final String DEADLINE_EMAIL = "deadline@example.com";
    private static final String INVALID_EMAIL = "invalid@example.com";
    private static final String INTERNAL_ERROR_EMAIL = "internal@example.com";
    private static final String MEMBER_NOT_EXISTS_BUT_CONFIRMED_EMAIL = "phantom@example.com";

    @BeforeEach
    void setUp() throws Exception {
        // Créer un service Members mock
        MembersServiceGrpc.MembersServiceImplBase mockService = new MembersServiceGrpc.MembersServiceImplBase() {
            @Override
            public void getMemberConfirmationStatus(ConfirmationStatusRequest request,
                                                     StreamObserver<ConfirmationStatusResponse> responseObserver) {
                String email = request.getEmail();

                if (email.equals(CONFIRMED_EMAIL)) {
                    responseObserver.onNext(ConfirmationStatusResponse.newBuilder()
                            .setMemberExists(true)
                            .setIsConfirmed(true)
                            .build());
                    responseObserver.onCompleted();
                } else if (email.equals(UNCONFIRMED_EMAIL)) {
                    responseObserver.onNext(ConfirmationStatusResponse.newBuilder()
                            .setMemberExists(true)
                            .setIsConfirmed(false)
                            .build());
                    responseObserver.onCompleted();
                } else if (email.equals(NOT_FOUND_EMAIL)) {
                    responseObserver.onError(new StatusRuntimeException(
                            Status.NOT_FOUND.withDescription("Member not found")));
                } else if (email.equals(UNAVAILABLE_EMAIL)) {
                    responseObserver.onError(new StatusRuntimeException(
                            Status.UNAVAILABLE.withDescription("Service unavailable")));
                } else if (email.equals(DEADLINE_EMAIL)) {
                    responseObserver.onError(new StatusRuntimeException(
                            Status.DEADLINE_EXCEEDED.withDescription("Timeout")));
                } else if (email.equals(INVALID_EMAIL)) {
                    responseObserver.onError(new StatusRuntimeException(
                            Status.INVALID_ARGUMENT.withDescription("Invalid email format")));
                } else if (email.equals(INTERNAL_ERROR_EMAIL)) {
                    responseObserver.onError(new StatusRuntimeException(
                            Status.INTERNAL.withDescription("Database error")));
                } else if (email.equals(MEMBER_NOT_EXISTS_BUT_CONFIRMED_EMAIL)) {
                    // Cas bizarre mais possible: memberExists=false mais isConfirmed=true
                    responseObserver.onNext(ConfirmationStatusResponse.newBuilder()
                            .setMemberExists(false)
                            .setIsConfirmed(true)
                            .build());
                    responseObserver.onCompleted();
                } else {
                    responseObserver.onError(new StatusRuntimeException(
                            Status.UNKNOWN.withDescription("Unknown error")));
                }
            }
        };

        // Démarrer le serveur in-memory
        server = InProcessServerBuilder
                .forName(SERVER_NAME)
                .directExecutor()
                .addService(mockService)
                .build()
                .start();

        // Créer le canal members
        channel = InProcessChannelBuilder
                .forName(SERVER_NAME)
                .directExecutor()
                .build();

        // Créer le members
        client = new MembersGrpcClient(channel);
    }

    @AfterEach
    void tearDown() {
        if (channel != null) {
            channel.shutdownNow();
        }
        if (server != null) {
            server.shutdownNow();
        }
    }

    @Test
    @DisplayName("Should return false when member is confirmed (isEmailNotConfirmed = false)")
    void isEmailNotConfirmed_MemberConfirmed_ReturnsFalse() {
        // When
        boolean result = client.isEmailNotConfirmed(CONFIRMED_EMAIL);

        // Then
        assertFalse(result, "Should return false for confirmed email");
    }

    @Test
    @DisplayName("Should return true when member is not confirmed")
    void isEmailNotConfirmed_MemberNotConfirmed_ReturnsTrue() {
        // When
        boolean result = client.isEmailNotConfirmed(UNCONFIRMED_EMAIL);

        // Then
        assertTrue(result, "Should return true for unconfirmed email");
    }

    @Test
    @DisplayName("Should throw exception when member not found (NOT_FOUND status)")
    void isEmailNotConfirmed_MemberNotFound_ThrowsException() {
        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            client.isEmailNotConfirmed(NOT_FOUND_EMAIL);
        });

        assertTrue(exception.getMessage().contains("Member not found"));
    }

    @Test
    @DisplayName("Should throw exception for unexpected errors")
    void isEmailNotConfirmed_UnexpectedError_ThrowsException() {
        // When/Then
        assertThrows(RuntimeException.class, () -> {
            client.isEmailNotConfirmed("error@example.com");
        });
    }

    @Test
    @DisplayName("Should handle multiple sequential requests")
    void isEmailNotConfirmed_MultipleRequests() {
        // When/Then
        assertFalse(client.isEmailNotConfirmed(CONFIRMED_EMAIL));
        assertTrue(client.isEmailNotConfirmed(UNCONFIRMED_EMAIL));
        assertFalse(client.isEmailNotConfirmed(CONFIRMED_EMAIL)); // Repeat
        assertTrue(client.isEmailNotConfirmed(UNCONFIRMED_EMAIL)); // Repeat
    }

    @Test
    @DisplayName("Should handle concurrent requests safely")
    void isEmailNotConfirmed_ConcurrentRequests() throws InterruptedException {
        // Given
        Thread[] threads = new Thread[10];

        // When
        for (int i = 0; i < threads.length; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                String email = (index % 2 == 0) ? CONFIRMED_EMAIL : UNCONFIRMED_EMAIL;
                boolean expected = (index % 2 != 0);
                boolean result = client.isEmailNotConfirmed(email);
                assertEquals(expected, result);
            });
            threads[i].start();
        }

        // Wait for all threads
        for (Thread thread : threads) {
            thread.join();
        }

        // Then - All requests should complete without errors
    }

    @Test
    @DisplayName("Should validate channel is connected")
    void channelIsConnected() {
        assertNotNull(channel);
        assertFalse(channel.isShutdown());
        assertFalse(channel.isTerminated());
    }

    @Test
    @DisplayName("Should throw MembersServiceUnavailableException when service is unavailable")
    void isEmailNotConfirmed_ServiceUnavailable_ThrowsCustomException() {
        // When/Then
        MembersGrpcClient.MembersServiceUnavailableException exception = assertThrows(
                MembersGrpcClient.MembersServiceUnavailableException.class,
                () -> client.isEmailNotConfirmed(UNAVAILABLE_EMAIL)
        );

        assertTrue(exception.getMessage().contains("unavailable"));
    }

    @Test
    @DisplayName("Should throw RuntimeException when deadline exceeded (timeout)")
    void isEmailNotConfirmed_DeadlineExceeded_ThrowsRuntimeException() {
        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> client.isEmailNotConfirmed(DEADLINE_EMAIL)
        );

        assertTrue(exception.getMessage().contains("Timeout exceeded"));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for invalid argument")
    void isEmailNotConfirmed_InvalidArgument_ThrowsIllegalArgumentException() {
        // When/Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> client.isEmailNotConfirmed(INVALID_EMAIL)
        );

        assertTrue(exception.getMessage().contains("Invalid argument"));
    }

    @Test
    @DisplayName("Should throw RuntimeException for unknown errors (default case)")
    void isEmailNotConfirmed_UnknownError_ThrowsRuntimeException() {
        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> client.isEmailNotConfirmed("unknown@example.com")
        );

        assertTrue(exception.getMessage().contains("Unexpected gRPC error"));
    }

    @Test
    @DisplayName("Should throw RuntimeException for INTERNAL error")
    void isEmailNotConfirmed_InternalError_ThrowsRuntimeException() {
        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> client.isEmailNotConfirmed(INTERNAL_ERROR_EMAIL)
        );

        assertTrue(exception.getMessage().contains("Internal error"));
    }

    @Test
    @DisplayName("Should return true when member doesn't exist but isConfirmed=true (edge case)")
    void isEmailNotConfirmed_MemberNotExistsButConfirmedTrue_ReturnsTrue() {
        // When
        boolean result = client.isEmailNotConfirmed(MEMBER_NOT_EXISTS_BUT_CONFIRMED_EMAIL);

        // Then - Should still block because memberExists=false
        assertTrue(result, "Should return true when member doesn't exist, regardless of isConfirmed value");
    }
}
