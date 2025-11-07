package com.ecclesiaflow.springsecurity.io.grpc.client;

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
 * Configuration du client gRPC pour le module Auth.
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

    @Value("${grpc.client.shutdown-timeout-seconds:5}")
    private int shutdownTimeoutSeconds;

    private ManagedChannel managedChannel;

    /**
     * Crée et configure le canal gRPC vers le module Members.
     *
     * @return le canal gRPC configuré et prêt à l'emploi
     */
    @Bean
    public ManagedChannel membersGrpcChannel() {
        managedChannel = ManagedChannelBuilder
                .forAddress(membersServiceHost, membersServicePort)
                
                // DÉVELOPPEMENT: Utilise plaintext (pas de TLS)
                // TODO PRODUCTION: Remplacer par .useTransportSecurity() + certificats
                .usePlaintext()
                
                // Configuration des messages
                .maxInboundMessageSize(4 * 1024 * 1024) // 4MB max
                
                // Keep-alive pour maintenir la connexion active
                .keepAliveTime(30, TimeUnit.SECONDS)
                .keepAliveTimeout(10, TimeUnit.SECONDS)
                .keepAliveWithoutCalls(true)
                
                // Idle timeout (ferme la connexion si inactive)
                .idleTimeout(5, TimeUnit.MINUTES)
                
                .build();

        return managedChannel;
    }

    /**
     * Ferme proprement le canal gRPC lors de l'arrêt de l'application.
     *
     * @throws InterruptedException si l'arrêt est interrompu
     */
    @PreDestroy
    public void shutdown() throws InterruptedException {
        if (managedChannel != null && !managedChannel.isShutdown()) {
            managedChannel.shutdown();
            
            if (!managedChannel.awaitTermination(shutdownTimeoutSeconds, TimeUnit.SECONDS)) {
                managedChannel.shutdownNow();
                managedChannel.awaitTermination(1, TimeUnit.SECONDS);
            }
        }
    }
}
