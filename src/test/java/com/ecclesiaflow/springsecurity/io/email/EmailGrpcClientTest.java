package com.ecclesiaflow.springsecurity.io.email;

import com.ecclesiaflow.grpc.email.*;
import com.ecclesiaflow.springsecurity.business.exceptions.EmailServiceException;
import com.ecclesiaflow.springsecurity.business.exceptions.GrpcCommunicationException;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour {@link EmailGrpcClient}.
 * <p>
 * Teste les appels gRPC vers le service Email et la gestion des erreurs.
 * </p>
 */
@DisplayName("EmailGrpcClient - Tests unitaires")
class EmailGrpcClientTest {

    @Mock
    private ManagedChannel mockChannel;

    @Mock
    private EmailServiceGrpc.EmailServiceBlockingStub mockStub;

    private EmailGrpcClient emailGrpcClient;
    private AutoCloseable closeable;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String RESET_LINK = "https://ecclesiaflow.com/reset?token=abc123";
    private static final UUID EMAIL_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        
        // Créer la VRAIE instance de EmailGrpcClient
        emailGrpcClient = new EmailGrpcClient(mockChannel);
        
        // Injecter le stub mocké via reflection
        org.springframework.test.util.ReflectionTestUtils.setField(emailGrpcClient, "stub", mockStub);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (closeable != null) {
            closeable.close();
        }
    }

    // =====================================================
    // Tests sendPasswordResetEmail
    // =====================================================

    @Test
    @DisplayName("sendPasswordResetEmail - Devrait envoyer email de réinitialisation avec succès")
    void sendPasswordResetEmail_ShouldSendEmailSuccessfully() {
        // Given
        SendEmailResponse response = SendEmailResponse.newBuilder()
                .setEmailId(EMAIL_ID.toString())
                .setStatus(com.ecclesiaflow.grpc.email.Status.STATUS_QUEUED)
                .setQueuedAt(System.currentTimeMillis())
                .build();
        
        when(mockStub.sendEmail(any(SendEmailRequest.class))).thenReturn(response);

        // When
        UUID result = emailGrpcClient.sendPasswordResetEmail(TEST_EMAIL, RESET_LINK);

        // Then
        assertThat(result).isEqualTo(EMAIL_ID);
        
        ArgumentCaptor<SendEmailRequest> requestCaptor = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(mockStub).sendEmail(requestCaptor.capture());
        
        SendEmailRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getToList()).contains(TEST_EMAIL);
        assertThat(capturedRequest.getSubject()).contains("Réinitialisation");
        assertThat(capturedRequest.getTemplateType()).isEqualTo(EmailTemplateType.EMAIL_TEMPLATE_PASSWORD_RESET);
        assertThat(capturedRequest.getVariablesMap()).containsEntry("email", TEST_EMAIL);
        assertThat(capturedRequest.getVariablesMap()).containsEntry("resetLink", RESET_LINK);
        assertThat(capturedRequest.getPriority()).isEqualTo(Priority.PRIORITY_HIGH);
    }

    @Test
    @DisplayName("sendPasswordResetEmail - Devrait lever EmailServiceException si gRPC échoue")
    void sendPasswordResetEmail_ShouldThrowEmailServiceException_WhenGrpcFails() {
        // Given
        StatusRuntimeException grpcException = new StatusRuntimeException(
                Status.UNAVAILABLE.withDescription("Service temporarily unavailable")
        );
        
        when(mockStub.sendEmail(any(SendEmailRequest.class))).thenThrow(grpcException);

        // When & Then
        assertThatThrownBy(() -> emailGrpcClient.sendPasswordResetEmail(TEST_EMAIL, RESET_LINK))
                .isInstanceOf(EmailServiceException.class)
                .hasMessageContaining("Failed to send email")
                .satisfies(ex -> {
                    EmailServiceException emailEx = (EmailServiceException) ex;
                    assertThat(emailEx.getEmailAddress()).isEqualTo(TEST_EMAIL);
                    assertThat(emailEx.getOperation()).isEqualTo(EmailServiceException.EmailOperation.PASSWORD_RESET);
                    assertThat(emailEx.getCause()).isInstanceOf(GrpcCommunicationException.class);
                });
    }

    // =====================================================
    // Tests sendPasswordChangedNotification
    // =====================================================

    @Test
    @DisplayName("sendPasswordChangedNotification - Devrait envoyer notification avec succès")
    void sendPasswordChangedNotification_ShouldSendNotificationSuccessfully() {
        // Given
        SendEmailResponse response = SendEmailResponse.newBuilder()
                .setEmailId(EMAIL_ID.toString())
                .setStatus(com.ecclesiaflow.grpc.email.Status.STATUS_QUEUED)
                .setQueuedAt(System.currentTimeMillis())
                .build();
        
        when(mockStub.sendEmail(any(SendEmailRequest.class))).thenReturn(response);

        // When
        UUID result = emailGrpcClient.sendPasswordChangedNotification(TEST_EMAIL);

        // Then
        assertThat(result).isEqualTo(EMAIL_ID);
        
        ArgumentCaptor<SendEmailRequest> requestCaptor = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(mockStub).sendEmail(requestCaptor.capture());
        
        SendEmailRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getToList()).contains(TEST_EMAIL);
        assertThat(capturedRequest.getSubject()).contains("mot de passe a été modifié");
        assertThat(capturedRequest.getTemplateType()).isEqualTo(EmailTemplateType.EMAIL_TEMPLATE_PASSWORD_CHANGED);
        assertThat(capturedRequest.getVariablesMap()).containsEntry("email", TEST_EMAIL);
        assertThat(capturedRequest.getPriority()).isEqualTo(Priority.PRIORITY_NORMAL);
    }

    @Test
    @DisplayName("sendPasswordChangedNotification - Devrait lever exception avec DEADLINE_EXCEEDED")
    void sendPasswordChangedNotification_ShouldThrowException_OnDeadlineExceeded() {
        // Given
        StatusRuntimeException grpcException = new StatusRuntimeException(
                Status.DEADLINE_EXCEEDED.withDescription("Request timeout")
        );
        
        when(mockStub.sendEmail(any(SendEmailRequest.class))).thenThrow(grpcException);

        // When & Then
        assertThatThrownBy(() -> emailGrpcClient.sendPasswordChangedNotification(TEST_EMAIL))
                .isInstanceOf(EmailServiceException.class)
                .satisfies(ex -> {
                    EmailServiceException emailEx = (EmailServiceException) ex;
                    assertThat(emailEx.getOperation()).isEqualTo(EmailServiceException.EmailOperation.PASSWORD_CHANGED);
                    
                    GrpcCommunicationException grpcEx = (GrpcCommunicationException) emailEx.getCause();
                    assertThat(grpcEx.getGrpcStatusCode()).isEqualTo(Status.Code.DEADLINE_EXCEEDED);
                });
    }

    // =====================================================
    // Tests sendWelcomeEmail
    // =====================================================

    @Test
    @DisplayName("sendWelcomeEmail - Devrait envoyer email de bienvenue avec succès")
    void sendWelcomeEmail_ShouldSendEmailSuccessfully() {
        // Given
        SendEmailResponse response = SendEmailResponse.newBuilder()
                .setEmailId(EMAIL_ID.toString())
                .setStatus(com.ecclesiaflow.grpc.email.Status.STATUS_QUEUED)
                .setQueuedAt(System.currentTimeMillis())
                .build();
        
        when(mockStub.sendEmail(any(SendEmailRequest.class))).thenReturn(response);

        // When
        UUID result = emailGrpcClient.sendWelcomeEmail(TEST_EMAIL);

        // Then
        assertThat(result).isEqualTo(EMAIL_ID);
        
        ArgumentCaptor<SendEmailRequest> requestCaptor = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(mockStub).sendEmail(requestCaptor.capture());
        
        SendEmailRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getToList()).contains(TEST_EMAIL);
        assertThat(capturedRequest.getSubject()).contains("Bienvenue");
        assertThat(capturedRequest.getTemplateType()).isEqualTo(EmailTemplateType.EMAIL_TEMPLATE_WELCOME);
        assertThat(capturedRequest.getVariablesMap()).containsEntry("email", TEST_EMAIL);
        assertThat(capturedRequest.getPriority()).isEqualTo(Priority.PRIORITY_NORMAL);
    }

    @Test
    @DisplayName("sendWelcomeEmail - Devrait lever exception avec INVALID_ARGUMENT")
    void sendWelcomeEmail_ShouldThrowException_OnInvalidArgument() {
        // Given
        StatusRuntimeException grpcException = new StatusRuntimeException(
                Status.INVALID_ARGUMENT.withDescription("Invalid email format")
        );
        
        when(mockStub.sendEmail(any(SendEmailRequest.class))).thenThrow(grpcException);

        // When & Then
        assertThatThrownBy(() -> emailGrpcClient.sendWelcomeEmail(TEST_EMAIL))
                .isInstanceOf(EmailServiceException.class)
                .satisfies(ex -> {
                    EmailServiceException emailEx = (EmailServiceException) ex;
                    assertThat(emailEx.getOperation()).isEqualTo(EmailServiceException.EmailOperation.WELCOME);
                    
                    GrpcCommunicationException grpcEx = (GrpcCommunicationException) emailEx.getCause();
                    assertThat(grpcEx.getGrpcStatusCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
                    assertThat(grpcEx.getMessage()).contains("Invalid email format");
                });
    }

    // =====================================================
    // Tests de mapping d'exceptions
    // =====================================================

    @Test
    @DisplayName("Devrait mapper StatusRuntimeException sans description")
    void shouldMapStatusRuntimeExceptionWithoutDescription() {
        // Given
        StatusRuntimeException grpcException = new StatusRuntimeException(Status.INTERNAL);
        
        when(mockStub.sendEmail(any(SendEmailRequest.class))).thenThrow(grpcException);

        // When & Then
        assertThatThrownBy(() -> emailGrpcClient.sendPasswordResetEmail(TEST_EMAIL, RESET_LINK))
                .isInstanceOf(EmailServiceException.class)
                .satisfies(ex -> {
                    EmailServiceException emailEx = (EmailServiceException) ex;
                    GrpcCommunicationException grpcEx = (GrpcCommunicationException) emailEx.getCause();
                    assertThat(grpcEx.getMessage()).contains("No description provided");
                });
    }

    @Test
    @DisplayName("Devrait gérer différents codes de statut gRPC")
    void shouldHandleDifferentGrpcStatusCodes() {
        // Test UNAVAILABLE
        testGrpcStatusCode(Status.UNAVAILABLE, "Service unavailable");
        
        // Test NOT_FOUND
        testGrpcStatusCode(Status.NOT_FOUND, "Template not found");
        
        // Test PERMISSION_DENIED
        testGrpcStatusCode(Status.PERMISSION_DENIED, "Access denied");
        
        // Test INTERNAL
        testGrpcStatusCode(Status.INTERNAL, "Internal server error");
    }

    private void testGrpcStatusCode(Status status, String description) {
        // Given
        StatusRuntimeException grpcException = new StatusRuntimeException(
                status.withDescription(description)
        );
        
        when(mockStub.sendEmail(any(SendEmailRequest.class))).thenThrow(grpcException);

        // When & Then
        assertThatThrownBy(() -> emailGrpcClient.sendPasswordResetEmail(TEST_EMAIL, RESET_LINK))
                .isInstanceOf(EmailServiceException.class)
                .satisfies(ex -> {
                    EmailServiceException emailEx = (EmailServiceException) ex;
                    GrpcCommunicationException grpcEx = (GrpcCommunicationException) emailEx.getCause();
                    assertThat(grpcEx.getGrpcStatusCode()).isEqualTo(status.getCode());
                    assertThat(grpcEx.getTargetService()).isEqualTo("EmailService");
                    assertThat(grpcEx.getOperation()).isEqualTo("sendEmail");
                });
        
        // Reset pour le prochain test
        reset(mockStub);
    }

    @Test
    @DisplayName("Devrait créer EmailGrpcClient avec ManagedChannel")
    void shouldCreateEmailGrpcClientWithManagedChannel() {
        // Given
        ManagedChannel channel = mock(ManagedChannel.class);

        // When
        EmailGrpcClient client = new EmailGrpcClient(channel);

        // Then
        assertThat(client).isNotNull();
    }
}
