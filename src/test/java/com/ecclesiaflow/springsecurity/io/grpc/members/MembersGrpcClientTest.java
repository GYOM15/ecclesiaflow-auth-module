package com.ecclesiaflow.springsecurity.io.grpc.members;

import com.ecclesiaflow.grpc.members.*;
import com.ecclesiaflow.springsecurity.io.members.MembersGrpcClient;
import io.grpc.ManagedChannel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link MembersGrpcClient}.
 * <p>
 * Note: Real communication and error handling tests are in
 * {@link MembersGrpcClientIntegrationTest} with InProcessServer.
 * These tests focus on construction and simple business logic.
 * </p>
 */
class MembersGrpcClientTest {

    // =====================================================
    // Construction and business logic tests
    // =====================================================

    @Test
    @DisplayName("Should create the client with a valid channel")
    void shouldConstructWithValidChannel() {
        // Given
        ManagedChannel channel = mock(ManagedChannel.class);

        // When
        MembersGrpcClient client = new MembersGrpcClient(channel);

        // Then
        assertNotNull(client, "The client must not be null");
    }

    @Test
    @DisplayName("Protobuf request construction")
    void shouldConstructProtobufRequests() {
        // When
        ConfirmationStatusRequest request = ConfirmationStatusRequest.newBuilder()
                .setEmail("test@ecclesiaflow.com")
                .build();

        // Then
        assertEquals("test@ecclesiaflow.com", request.getEmail());
    }

    @Test
    @DisplayName("Protobuf response parsing - member exists and confirmed")
    void shouldParseConfirmedMember() {
        // Given
        ConfirmationStatusResponse response = ConfirmationStatusResponse.newBuilder()
                .setMemberExists(true)
                .setIsConfirmed(true)
                .build();

        // When - Business logic : isEmailNotConfirmed
        boolean isNotConfirmed = !response.getMemberExists() || !response.getIsConfirmed();

        // Then
        assertFalse(isNotConfirmed, "A confirmed member must NOT be blocked");
        assertTrue(response.getMemberExists());
        assertTrue(response.getIsConfirmed());
    }

    @Test
    @DisplayName("Protobuf response parsing - member exists but not confirmed")
    void shouldParseUnconfirmedMember() {
        // Given
        ConfirmationStatusResponse response = ConfirmationStatusResponse.newBuilder()
                .setMemberExists(true)
                .setIsConfirmed(false)
                .build();

        // When - Business logic : isEmailNotConfirmed
        boolean isNotConfirmed = !response.getMemberExists() || !response.getIsConfirmed();

        // Then
        assertTrue(isNotConfirmed, "An unconfirmed member must be blocked");
        assertTrue(response.getMemberExists());
        assertFalse(response.getIsConfirmed());
    }

    @Test
    @DisplayName("Protobuf response parsing - member does not exist")
    void shouldParseMemberNotFound() {
        // Given
        ConfirmationStatusResponse response = ConfirmationStatusResponse.newBuilder()
                .setMemberExists(false)
                .setIsConfirmed(false)
                .build();

        // When - Business logic : isEmailNotConfirmed
        boolean isNotConfirmed = !response.getMemberExists() || !response.getIsConfirmed();

        // Then
        assertTrue(isNotConfirmed, "A non-existent member must be blocked");
        assertFalse(response.getMemberExists());
    }

    @Test
    @DisplayName("Custom exception MembersServiceUnavailableException")
    void shouldCreateCustomException() {
        // Given
        String message = "Service unavailable";
        Throwable cause = new RuntimeException("Test");

        // When
        MembersGrpcClient.MembersServiceUnavailableException exception =
                new MembersGrpcClient.MembersServiceUnavailableException(message, cause);

        // Then
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("Protobuf response parsing - notifyAccountActivated success")
    void shouldParseAccountActivatedSuccess() {
        // Given
        AccountActivatedResponse response = AccountActivatedResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Account activated")
                .build();

        // When
        boolean success = response.getSuccess();

        // Then
        assertTrue(success, "The response should indicate success");
        assertEquals("Account activated", response.getMessage());
    }

    @Test
    @DisplayName("Protobuf response parsing - notifyAccountActivated failure")
    void shouldParseAccountActivatedFailure() {
        // Given
        AccountActivatedResponse response = AccountActivatedResponse.newBuilder()
                .setSuccess(false)
                .setMessage("Member not found")
                .build();

        // When
        boolean success = response.getSuccess();

        // Then
        assertFalse(success, "The response should indicate a failure");
        assertEquals("Member not found", response.getMessage());
    }

    @Test
    @DisplayName("Protobuf request construction - LocalCredentialsAddedRequest")
    void shouldConstructLocalCredentialsAddedRequest() {
        LocalCredentialsAddedRequest request = LocalCredentialsAddedRequest.newBuilder()
                .setKeycloakUserId("keycloak-user-123")
                .build();

        assertEquals("keycloak-user-123", request.getKeycloakUserId());
    }

    @Test
    @DisplayName("Protobuf response parsing - notifyLocalCredentialsAdded success")
    void shouldParseLocalCredentialsAddedSuccess() {
        LocalCredentialsAddedResponse response = LocalCredentialsAddedResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Credentials updated")
                .build();

        assertTrue(response.getSuccess());
        assertEquals("Credentials updated", response.getMessage());
    }

    @Test
    @DisplayName("Protobuf response parsing - notifyLocalCredentialsAdded failure")
    void shouldParseLocalCredentialsAddedFailure() {
        LocalCredentialsAddedResponse response = LocalCredentialsAddedResponse.newBuilder()
                .setSuccess(false)
                .setMessage("Member not found")
                .build();

        assertFalse(response.getSuccess());
        assertEquals("Member not found", response.getMessage());
    }
}
