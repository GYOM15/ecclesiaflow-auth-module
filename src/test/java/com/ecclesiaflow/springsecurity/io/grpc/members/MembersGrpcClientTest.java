package com.ecclesiaflow.springsecurity.io.grpc.client;

import com.ecclesiaflow.grpc.members.*;
import com.ecclesiaflow.springsecurity.io.members.MembersGrpcClient;
import io.grpc.ManagedChannel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour {@link MembersGrpcClient}.
 * <p>
 * Note : Les tests de communication réelle et de gestion d'erreurs sont dans
 * {@link MembersGrpcClientIntegrationTest} avec InProcessServer.
 * Ces tests se concentrent sur la construction et la logique métier simple.
 * </p>
 */
class MembersGrpcClientTest {

    // =====================================================
    // Tests de construction et logique métier
    // =====================================================

    @Test
    @DisplayName("Doit créer le client avec un canal valide")
    void shouldConstructWithValidChannel() {
        // Given
        ManagedChannel channel = mock(ManagedChannel.class);

        // When
        MembersGrpcClient client = new MembersGrpcClient(channel);

        // Then
        assertNotNull(client, "Le client ne doit pas être null");
    }

    @Test
    @DisplayName("Construction des requêtes Protobuf")
    void shouldConstructProtobufRequests() {
        // When
        ConfirmationStatusRequest request = ConfirmationStatusRequest.newBuilder()
                .setEmail("test@ecclesiaflow.com")
                .build();

        // Then
        assertEquals("test@ecclesiaflow.com", request.getEmail());
    }

    @Test
    @DisplayName("Parsing des réponses Protobuf - membre existe et confirmé")
    void shouldParseConfirmedMember() {
        // Given
        ConfirmationStatusResponse response = ConfirmationStatusResponse.newBuilder()
                .setMemberExists(true)
                .setIsConfirmed(true)
                .build();

        // When - Logique métier : isEmailNotConfirmed
        boolean isNotConfirmed = !response.getMemberExists() || !response.getIsConfirmed();

        // Then
        assertFalse(isNotConfirmed, "Un membre confirmé ne doit PAS être bloqué");
        assertTrue(response.getMemberExists());
        assertTrue(response.getIsConfirmed());
    }

    @Test
    @DisplayName("Parsing des réponses Protobuf - membre existe mais non confirmé")
    void shouldParseUnconfirmedMember() {
        // Given
        ConfirmationStatusResponse response = ConfirmationStatusResponse.newBuilder()
                .setMemberExists(true)
                .setIsConfirmed(false)
                .build();

        // When - Logique métier : isEmailNotConfirmed
        boolean isNotConfirmed = !response.getMemberExists() || !response.getIsConfirmed();

        // Then
        assertTrue(isNotConfirmed, "Un membre non confirmé doit être bloqué");
        assertTrue(response.getMemberExists());
        assertFalse(response.getIsConfirmed());
    }

    @Test
    @DisplayName("Parsing des réponses Protobuf - membre n'existe pas")
    void shouldParseMemberNotFound() {
        // Given
        ConfirmationStatusResponse response = ConfirmationStatusResponse.newBuilder()
                .setMemberExists(false)
                .setIsConfirmed(false)
                .build();

        // When - Logique métier : isEmailNotConfirmed
        boolean isNotConfirmed = !response.getMemberExists() || !response.getIsConfirmed();

        // Then
        assertTrue(isNotConfirmed, "Un membre inexistant doit être bloqué");
        assertFalse(response.getMemberExists());
    }

    @Test
    @DisplayName("Exception personnalisée MembersServiceUnavailableException")
    void shouldCreateCustomException() {
        // Given
        String message = "Service indisponible";
        Throwable cause = new RuntimeException("Test");

        // When
        MembersGrpcClient.MembersServiceUnavailableException exception =
                new MembersGrpcClient.MembersServiceUnavailableException(message, cause);

        // Then
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }
}
