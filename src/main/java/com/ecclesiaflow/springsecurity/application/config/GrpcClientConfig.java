package com.ecclesiaflow.springsecurity.application.config;

import com.ecclesiaflow.springsecurity.io.members.MembersGrpcClient;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;

/**
 * Members gRPC client configuration for the Auth module.
 * <p>
 * This class configures the gRPC communication channel (ManagedChannel) to the
 * Members module. It manages the channel lifecycle: creation, configuration,
 * and graceful shutdown when the application stops.
 * </p>
 *
 * <p><strong>Architectural role:</strong> Infrastructure - gRPC Client Configuration</p>
 *
 * @author EcclesiaFlow Team
 * @since 1.0.0
 * @see ManagedChannel
 * @see MembersGrpcClient
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "grpc.enabled", havingValue = "true", matchIfMissing = false)
public class GrpcClientConfig {

    @Value("${grpc.members.host:localhost}")
    private String membersServiceHost;

    @Value("${grpc.members.port:9091}")
    private int membersServicePort;

    @Value("${email.module.grpc.host:localhost}")
    private String emailServiceHost;

    @Value("${email.module.grpc.port:9093}")
    private int emailServicePort;

    @Value("${grpc.client.shutdown-timeout-seconds:5}")
    private int shutdownTimeoutSeconds;

    private ManagedChannel membersChannel;
    private ManagedChannel emailChannel;

    /**
     * Creates and configures the gRPC channel to the Members module.
     *
     * @return the configured and ready-to-use gRPC channel
     */
    @Bean
    public ManagedChannel membersGrpcChannel() {
        membersChannel = ManagedChannelBuilder
                .forAddress(membersServiceHost, membersServicePort)

                // DEVELOPMENT: Uses plaintext (no TLS)
                // TODO PRODUCTION: Replace with .useTransportSecurity() + certificates
                .usePlaintext()
                .maxInboundMessageSize(4 * 1024 * 1024)
                .keepAliveTime(120, TimeUnit.SECONDS)
                .keepAliveTimeout(20, TimeUnit.SECONDS)
                .keepAliveWithoutCalls(false)
                .idleTimeout(5, TimeUnit.MINUTES)
                .build();

        return membersChannel;
    }

    /**
     * Creates and configures the gRPC channel to the Email module.
     *
     * @return the configured and ready-to-use gRPC channel
     */
    @Bean
    public ManagedChannel emailGrpcChannel() {
        emailChannel = ManagedChannelBuilder
                .forAddress(emailServiceHost, emailServicePort)
                .usePlaintext()
                .maxInboundMessageSize(4 * 1024 * 1024)
                .keepAliveTime(120, TimeUnit.SECONDS)  // Increased to 2 minutes (gRPC recommended)
                .keepAliveTimeout(20, TimeUnit.SECONDS)
                .keepAliveWithoutCalls(false)           // Do not send pings if no active calls
                .idleTimeout(5, TimeUnit.MINUTES)
                .build();

        return emailChannel;
    }

    /**
     * Gracefully shuts down gRPC channels on application shutdown.
     *
     * @throws InterruptedException if the shutdown is interrupted
     */
    @PreDestroy
    public void shutdown() throws InterruptedException {
        shutdownChannel(membersChannel);
        shutdownChannel(emailChannel);
    }

    private void shutdownChannel(ManagedChannel channel) throws InterruptedException {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
            
            if (!channel.awaitTermination(shutdownTimeoutSeconds, TimeUnit.SECONDS)) {
                channel.shutdownNow();
                channel.awaitTermination(1, TimeUnit.SECONDS);
            }
        }
    }
}
