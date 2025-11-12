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
 * Configuration du members gRPC pour le module Auth.
 * <p>
 * Cette classe configure le canal de communication gRPC (ManagedChannel) vers
 * le module Members. Elle gère le cycle de vie du canal : création,
 * configuration et fermeture graceful lors de l'arrêt de l'application.
 * </p>
 *
 * <p><strong>Rôle architectural :</strong> Infrastructure - gRPC Client Configuration</p>
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
     * Crée et configure le canal gRPC vers le module Members.
     *
     * @return le canal gRPC configuré et prêt à l'emploi
     */
    @Bean
    public ManagedChannel membersGrpcChannel() {
        membersChannel = ManagedChannelBuilder
                .forAddress(membersServiceHost, membersServicePort)

                // DÉVELOPPEMENT: Utilise plaintext (pas de TLS)
                // TODO PRODUCTION: Remplacer par .useTransportSecurity() + certificats
                .usePlaintext()
                .maxInboundMessageSize(4 * 1024 * 1024)
                .keepAliveTime(30, TimeUnit.SECONDS)
                .keepAliveTimeout(10, TimeUnit.SECONDS)
                .keepAliveWithoutCalls(true)
                .idleTimeout(5, TimeUnit.MINUTES)
                .build();

        return membersChannel;
    }

    /**
     * Crée et configure le canal gRPC vers le module Email.
     *
     * @return le canal gRPC configuré et prêt à l'emploi
     */
    @Bean
    public ManagedChannel emailGrpcChannel() {
        emailChannel = ManagedChannelBuilder
                .forAddress(emailServiceHost, emailServicePort)
                .usePlaintext()
                .maxInboundMessageSize(4 * 1024 * 1024)
                .keepAliveTime(120, TimeUnit.SECONDS)  // Augmenté à 2 minutes (recommandé gRPC)
                .keepAliveTimeout(20, TimeUnit.SECONDS)
                .keepAliveWithoutCalls(false)           // Ne pas envoyer de ping si pas d'appels
                .idleTimeout(5, TimeUnit.MINUTES)
                .build();

        return emailChannel;
    }

    /**
     * Ferme proprement les canaux gRPC lors de l'arrêt de l'application.
     *
     * @throws InterruptedException si l'arrêt est interrompu
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
