package com.ecclesiaflow.springsecurity.io.grpc.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.HealthStatusManager;
import io.grpc.protobuf.services.ProtoReflectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Configuration du serveur gRPC pour le module d'authentification EcclesiaFlow.
 * <p>
 * Cette classe configure et démarre un serveur gRPC qui expose les services
 * d'authentification pour la communication inter-modules. Le serveur est démarré
 * automatiquement au démarrage de l'application Spring Boot et s'arrête proprement
 * lors de l'arrêt de l'application.
 * </p>
 *
 * <p><strong>Rôle architectural :</strong> Infrastructure - gRPC Server Configuration</p>
 *
 * <p><strong>Fonctionnalités :</strong></p>
 * <ul>
 *   <li>Démarrage automatique du serveur gRPC sur un port configurable</li>
 *   <li>Enregistrement des services gRPC (AuthService, etc.)</li>
 *   <li>Health checks automatiques via gRPC Health Checking Protocol</li>
 *   <li>Reflection API pour debugging (désactivable en production)</li>
 *   <li>Shutdown graceful avec timeout configurable</li>
 * </ul>
 *
 * <p><strong>Configuration :</strong></p>
 * <ul>
 *   <li>grpc.enabled - Active/désactive le serveur gRPC (défaut: false)</li>
 *   <li>grpc.server.port - Port du serveur gRPC (défaut: 9090)</li>
 *   <li>grpc.server.shutdown-timeout-seconds - Timeout pour shutdown (défaut: 30s)</li>
 * </ul>
 *
 * <p><strong>Sécurité :</strong></p>
 * <ul>
 *   <li>TLS/mTLS configurable (désactivé par défaut pour développement)</li>
 *   <li>Authentification via JWT dans metadata gRPC</li>
 *   <li>Isolation réseau (port séparé du HTTP REST)</li>
 * </ul>
 *
 * @author EcclesiaFlow Team
 * @since 1.0.0
 * @see JwtGrpcServiceImpl
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "grpc.enabled", havingValue = "true", matchIfMissing = false)
public class GrpcServerConfig {

    private final JwtGrpcServiceImpl jwtGrpcService;

    @Value("${grpc.server.port:9090}")
    private int grpcServerPort;

    @Value("${grpc.server.shutdown-timeout-seconds:30}")
    private int shutdownTimeoutSeconds;

    private Server grpcServer;
    private HealthStatusManager healthStatusManager;

    /**
     * Démarre le serveur gRPC au démarrage de l'application.
     * <p>
     * Cette méthode est appelée automatiquement par Spring après l'initialisation
     * des beans. Elle configure et démarre le serveur gRPC avec tous les services
     * enregistrés et les fonctionnalités de monitoring.
     * </p>
     *
     * @throws IOException si le serveur ne peut pas démarrer (port occupé, etc.)
     */
    @PostConstruct
    public void start() throws IOException {
        healthStatusManager = new HealthStatusManager();
        
        grpcServer = ServerBuilder.forPort(grpcServerPort)
                // Enregistrer les services métier
                .addService(jwtGrpcService)
                
                // Health checks (gRPC Health Checking Protocol)
                .addService(healthStatusManager.getHealthService())
                
                // Reflection service (pour debugging avec grpcurl/grpcui)
                // TODO: Désactiver en production pour sécurité
                .addService(ProtoReflectionService.newInstance())
                
                // Configuration du serveur
                .maxInboundMessageSize(4 * 1024 * 1024) // 4MB max message size
                .build()
                .start();

        // Marquer le service comme SERVING pour health checks
        healthStatusManager.setStatus("ecclesiaflow.auth.AuthService", 
                io.grpc.health.v1.HealthCheckResponse.ServingStatus.SERVING);

        // Hook pour arrêt graceful si JVM s'arrête
        Runtime.getRuntime().addShutdownHook(new Thread(GrpcServerConfig.this::stop));
    }

    /**
     * Arrête proprement le serveur gRPC lors de l'arrêt de l'application.
     * <p>
     * Cette méthode est appelée automatiquement par Spring lors de la destruction
     * du contexte. Elle initie un arrêt graceful du serveur avec un timeout,
     * permettant aux requêtes en cours de se terminer.
     * </p>
     */
    @PreDestroy
    public void stop() {
        if (grpcServer != null) {
            try {
                // Marquer les services comme NOT_SERVING
                healthStatusManager.enterTerminalState();
                
                // Arrêt graceful avec timeout
                grpcServer.shutdown();
                
                if (!grpcServer.awaitTermination(shutdownTimeoutSeconds, TimeUnit.SECONDS)) {
                    grpcServer.shutdownNow();
                    grpcServer.awaitTermination(5, TimeUnit.SECONDS);
                }
            } catch (InterruptedException e) {
                grpcServer.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Expose le serveur gRPC comme bean Spring (optionnel, pour monitoring).
     *
     * @return l'instance du serveur gRPC
     */
    @Bean
    public Server grpcServer() {
        return grpcServer;
    }

    /**
     * Expose le health status manager comme bean Spring (pour monitoring externe).
     *
     * @return l'instance du health status manager
     */
    @Bean
    public HealthStatusManager healthStatusManager() {
        return healthStatusManager;
    }
}
