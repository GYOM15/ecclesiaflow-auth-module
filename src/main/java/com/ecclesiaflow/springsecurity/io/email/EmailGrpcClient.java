package com.ecclesiaflow.springsecurity.io.email;

import com.ecclesiaflow.grpc.email.*;
import com.ecclesiaflow.springsecurity.business.domain.email.EmailClient;
import com.ecclesiaflow.springsecurity.business.exceptions.EmailServiceException;
import com.ecclesiaflow.springsecurity.business.exceptions.EmailServiceException.EmailOperation;
import com.ecclesiaflow.springsecurity.business.exceptions.GrpcCommunicationException;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "grpc.enabled", havingValue = "true", matchIfMissing = false)
public class EmailGrpcClient implements EmailClient {
    
    private final EmailServiceGrpc.EmailServiceBlockingStub stub;
    
    public EmailGrpcClient(ManagedChannel emailGrpcChannel) {
        this.stub = EmailServiceGrpc.newBlockingStub(emailGrpcChannel);
    }
    
    @Override
    public UUID sendPasswordResetEmail(String toEmail, String resetLink) {
        Map<String, String> variables = Map.of(
            "email", toEmail,
            "resetLink", resetLink
        );
        
        return sendEmail(
            toEmail,
            EmailTemplateType.EMAIL_TEMPLATE_PASSWORD_RESET,
            "Réinitialisation de votre mot de passe - EcclesiaFlow",
            variables,
            Priority.PRIORITY_HIGH,
            EmailOperation.PASSWORD_RESET
        );
    }
    
    @Override
    public UUID sendPasswordChangedNotification(String toEmail) {
        Map<String, String> variables = Map.of("email", toEmail);
        
        return sendEmail(
            toEmail,
            EmailTemplateType.EMAIL_TEMPLATE_PASSWORD_CHANGED,
            "Votre mot de passe a été modifié - EcclesiaFlow",
            variables,
            Priority.PRIORITY_NORMAL,
            EmailOperation.PASSWORD_CHANGED
        );
    }
    
    @Override
    public UUID sendWelcomeEmail(String toEmail) {
        Map<String, String> variables = Map.of("email", toEmail);
        
        return sendEmail(
            toEmail,
            EmailTemplateType.EMAIL_TEMPLATE_WELCOME,
            "Bienvenue sur EcclesiaFlow",
            variables,
            Priority.PRIORITY_NORMAL,
            EmailOperation.WELCOME
        );
    }
    
    private UUID sendEmail(String toEmail,
                           EmailTemplateType templateType,
                           String subject,
                           Map<String, String> variables,
                           Priority priority,
                           EmailOperation operation) {
        try {
            SendEmailRequest request = SendEmailRequest.newBuilder()
                    .addTo(toEmail)
                    .setSubject(subject)
                    .setTemplateType(templateType)
                    .putAllVariables(variables)
                    .setPriority(priority)
                    .build();
            
            SendEmailResponse response = stub.sendEmail(request);
            return UUID.fromString(response.getEmailId());
            
        } catch (StatusRuntimeException e) {
            throw mapToEmailServiceException(e, toEmail, operation);
        }
    }
    
    private EmailServiceException mapToEmailServiceException(StatusRuntimeException e,
                                                             String toEmail,
                                                             EmailOperation operation) {
        Status.Code statusCode = e.getStatus().getCode();
        String description = e.getStatus().getDescription();
        
        GrpcCommunicationException grpcException = new GrpcCommunicationException(
                "EmailService",
                "sendEmail",
                statusCode,
                description != null ? description : "No description provided",
                e
        );
        
        return new EmailServiceException(
                "Failed to send email (" + operation.name() + ")",
                toEmail,
                operation,
                grpcException
        );
    }
}
