package com.ecclesiaflow.springsecurity.io.grpc.server;

import com.ecclesiaflow.grpc.auth.AuthServiceGrpc;
import com.ecclesiaflow.grpc.auth.TemporaryTokenRequest;
import com.ecclesiaflow.grpc.auth.TemporaryTokenResponse;
import com.ecclesiaflow.springsecurity.web.exception.JwtProcessingException;
import com.ecclesiaflow.springsecurity.web.security.Jwt;
import com.ecclesiaflow.springsecurity.web.security.JwtProcessor;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests d'intégration pour JwtGrpcServiceImpl.
 * <p>
 * Teste le serveur gRPC avec communication réelle via InProcessServer.
 * Couvre les scénarios de succès et d'erreur.
 * </p>
 */
class JwtGrpcServiceImplIntegrationTest {

    private Server server;
    private ManagedChannel channel;
    private AuthServiceGrpc.AuthServiceBlockingStub stub;

    private Jwt jwt;
    private JwtProcessor jwtProcessor;

    private static final String SERVER_NAME = "test-auth-grpc";
    private static final String EMAIL = "test@example.com";
    private static final UUID MEMBER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final String GENERATED_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIn0.test";

    @BeforeEach
    void setUp() throws Exception {
        // Mock des dépendances
        jwt = mock(Jwt.class);
        jwtProcessor = mock(JwtProcessor.class);

        // Créer le service gRPC
        JwtGrpcServiceImpl service = new JwtGrpcServiceImpl(jwt);

        // Démarrer le serveur in-memory
        server = InProcessServerBuilder
                .forName(SERVER_NAME)
                .directExecutor()
                .addService(service)
                .build()
                .start();

        // Créer le canal members
        channel = InProcessChannelBuilder
                .forName(SERVER_NAME)
                .directExecutor()
                .build();

        // Créer le stub
        stub = AuthServiceGrpc.newBlockingStub(channel);
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
    @DisplayName("Should generate temporary token successfully via real gRPC call")
    void generateTemporaryToken_Success() {
        // Given
        when(jwt.generateTemporaryToken(EMAIL, MEMBER_ID, "password_setup")).thenReturn(GENERATED_TOKEN);

        TemporaryTokenRequest request = TemporaryTokenRequest.newBuilder()
                .setEmail(EMAIL)
                .setMemberId(MEMBER_ID.toString())
                .build();

        // When
        TemporaryTokenResponse response = stub.generateTemporaryToken(request);

        // Then
        assertNotNull(response);
        assertEquals(GENERATED_TOKEN, response.getTemporaryToken());
    }

    @Test
    @DisplayName("Should throw INVALID_ARGUMENT for invalid UUID format")
    void generateTemporaryToken_InvalidUUID() {
        // Given
        TemporaryTokenRequest request = TemporaryTokenRequest.newBuilder()
                .setEmail(EMAIL)
                .setMemberId("invalid-uuid-format")
                .build();

        // When/Then
        StatusRuntimeException exception = assertThrows(
                StatusRuntimeException.class,
                () -> stub.generateTemporaryToken(request)
        );

        assertEquals(Status.INVALID_ARGUMENT.getCode(), exception.getStatus().getCode());
        assertTrue(exception.getStatus().getDescription().contains("must be a valid UUID format"));
    }

    @Test
    @DisplayName("Should throw INVALID_ARGUMENT for empty email")
    void generateTemporaryToken_EmptyEmail() {
        // Given
        TemporaryTokenRequest request = TemporaryTokenRequest.newBuilder()
                .setEmail("")
                .setMemberId(MEMBER_ID.toString())
                .build();

        // When/Then
        StatusRuntimeException exception = assertThrows(
                StatusRuntimeException.class,
                () -> stub.generateTemporaryToken(request)
        );

        assertEquals(Status.INVALID_ARGUMENT.getCode(), exception.getStatus().getCode());
        assertTrue(exception.getStatus().getDescription().contains("Email cannot be empty"));
    }

    @Test
    @DisplayName("Should throw INTERNAL when service throws exception")
    void generateTemporaryToken_ServiceError() {
        // Given
        when(jwt.generateTemporaryToken(any(), any(), eq("password_setup")))
                .thenThrow(new RuntimeException("Database error"));

        TemporaryTokenRequest request = TemporaryTokenRequest.newBuilder()
                .setEmail(EMAIL)
                .setMemberId(MEMBER_ID.toString())
                .build();

        // When/Then
        StatusRuntimeException exception = assertThrows(
                StatusRuntimeException.class,
                () -> stub.generateTemporaryToken(request)
        );

        assertEquals(Status.INTERNAL.getCode(), exception.getStatus().getCode());
        assertTrue(exception.getStatus().getDescription().contains("An unexpected error occurred"));
    }

    @Test
    @DisplayName("Should handle multiple concurrent requests")
    void generateTemporaryToken_ConcurrentRequests() throws InterruptedException {
        // Given
        when(jwt.generateTemporaryToken(any(), any(), eq("password_setup"))).thenReturn(GENERATED_TOKEN);

        TemporaryTokenRequest request = TemporaryTokenRequest.newBuilder()
                .setEmail(EMAIL)
                .setMemberId(MEMBER_ID.toString())
                .build();

        // When - Simuler plusieurs requêtes simultanées
        Thread[] threads = new Thread[5];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                TemporaryTokenResponse response = stub.generateTemporaryToken(request);
                assertNotNull(response);
                assertEquals(GENERATED_TOKEN, response.getTemporaryToken());
            });
            threads[i].start();
        }

        // Wait for all threads
        for (Thread thread : threads) {
            thread.join();
        }

        // Then - All requests should succeed
    }

    @Test
    @DisplayName("Should handle null memberId")
    void generateTemporaryToken_NullMemberId() {
        // Given
        TemporaryTokenRequest request = TemporaryTokenRequest.newBuilder()
                .setEmail(EMAIL)
                .build();

        // When/Then
        StatusRuntimeException exception = assertThrows(
                StatusRuntimeException.class,
                () -> stub.generateTemporaryToken(request)
        );

        assertEquals(Status.INVALID_ARGUMENT.getCode(), exception.getStatus().getCode());
    }

    @Test
    @DisplayName("Should validate that server is running")
    void serverIsRunning() {
        assertNotNull(server);
        assertFalse(server.isShutdown());
        assertFalse(server.isTerminated());
    }

    @Test
    @DisplayName("Should throw INVALID_ARGUMENT for invalid email format")
    void generateTemporaryToken_InvalidEmailFormat() {
        // Given - Email sans @ et domaine
        TemporaryTokenRequest request = TemporaryTokenRequest.newBuilder()
                .setEmail("invalid-email-format")
                .setMemberId(MEMBER_ID.toString())
                .build();

        // When/Then
        StatusRuntimeException exception = assertThrows(
                StatusRuntimeException.class,
                () -> stub.generateTemporaryToken(request)
        );

        assertEquals(Status.INVALID_ARGUMENT.getCode(), exception.getStatus().getCode());
        assertTrue(exception.getStatus().getDescription().contains("Invalid email format"));
    }

    @Test
    @DisplayName("Should throw INTERNAL when JwtProcessingException occurs")
    void generateTemporaryToken_JwtProcessingException() {
        // Given
        when(jwt.generateTemporaryToken(any(), any(), eq("password_setup")))
                .thenThrow(new JwtProcessingException("JWT signing failed"));

        TemporaryTokenRequest request = TemporaryTokenRequest.newBuilder()
                .setEmail(EMAIL)
                .setMemberId(MEMBER_ID.toString())
                .build();

        // When/Then
        StatusRuntimeException exception = assertThrows(
                StatusRuntimeException.class,
                () -> stub.generateTemporaryToken(request)
        );

        assertEquals(Status.INTERNAL.getCode(), exception.getStatus().getCode());
        assertTrue(exception.getStatus().getDescription().contains("Failed to generate temporary token"));
    }
}
