package com.ecclesiaflow.springsecurity.application.config;

import com.ecclesiaflow.springsecurity.io.grpc.server.AuthGrpcServiceImpl;
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
 * gRPC server configuration for the EcclesiaFlow authentication module.
 * <p>
 * This class configures and starts a gRPC server that exposes authentication services
 * for inter-module communication. The server starts automatically with the Spring Boot
 * application and shuts down gracefully when the application stops.
 * </p>
 *
 * <p><strong>Architectural role:</strong> Infrastructure - gRPC Server Configuration</p>
 *
 * <p><strong>Features:</strong></p>
 * <ul>
 *   <li>Automatic gRPC server startup on a configurable port</li>
 *   <li>gRPC service registration (AuthService, etc.)</li>
 *   <li>Automatic health checks via gRPC Health Checking Protocol</li>
 *   <li>Reflection API for debugging (can be disabled in production)</li>
 *   <li>Graceful shutdown with configurable timeout</li>
 * </ul>
 *
 * <p><strong>Configuration:</strong></p>
 * <ul>
 *   <li>grpc.enabled - Enables/disables the gRPC server (default: false)</li>
 *   <li>grpc.server.port - gRPC server port (default: 9090)</li>
 *   <li>grpc.server.shutdown-timeout-seconds - Shutdown timeout (default: 30s)</li>
 * </ul>
 *
 * <p><strong>Security:</strong></p>
 * <ul>
 *   <li>TLS/mTLS configurable (disabled by default for development)</li>
 *   <li>Authentication via JWT in gRPC metadata</li>
 *   <li>Network isolation (separate port from HTTP REST)</li>
 * </ul>
 *
 * @author EcclesiaFlow Team
 * @since 1.0.0
 * @see AuthGrpcServiceImpl
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "grpc.enabled", havingValue = "true", matchIfMissing = false)
public class GrpcServerConfig {

    private final AuthGrpcServiceImpl jwtGrpcService;

    @Value("${grpc.server.port:9090}")
    private int grpcServerPort;

    @Value("${grpc.server.shutdown-timeout-seconds:30}")
    private int shutdownTimeoutSeconds;

    private Server grpcServer;
    private HealthStatusManager healthStatusManager;

    /**
     * Starts the gRPC server on application startup.
     * <p>
     * This method is automatically called by Spring after bean initialization.
     * It configures and starts the gRPC server with all registered services
     * and monitoring features.
     * </p>
     *
     * @throws IOException if the server cannot start (port in use, etc.)
     */
    @PostConstruct
    public void start() throws IOException {
        healthStatusManager = new HealthStatusManager();
        
        grpcServer = ServerBuilder.forPort(grpcServerPort)
                // Register business services
                .addService(jwtGrpcService)
                
                // Health checks (gRPC Health Checking Protocol)
                .addService(healthStatusManager.getHealthService())
                
                // Reflection service (for debugging with grpcurl/grpcui)
                // TODO: Disable in production for security
                .addService(ProtoReflectionService.newInstance())
                
                // Server configuration
                .maxInboundMessageSize(4 * 1024 * 1024) // 4MB max message size
                .build()
                .start();

        // Mark service as SERVING for health checks
        healthStatusManager.setStatus("ecclesiaflow.auth.AuthService", 
                io.grpc.health.v1.HealthCheckResponse.ServingStatus.SERVING);

        // Hook for graceful shutdown if JVM stops
        Runtime.getRuntime().addShutdownHook(new Thread(GrpcServerConfig.this::stop));
    }

    /**
     * Gracefully stops the gRPC server on application shutdown.
     * <p>
     * This method is automatically called by Spring during context destruction.
     * It initiates a graceful server shutdown with a timeout, allowing
     * in-flight requests to complete.
     * </p>
     */
    @PreDestroy
    public void stop() {
        if (grpcServer != null) {
            try {
                // Mark services as NOT_SERVING
                healthStatusManager.enterTerminalState();
                
                // Graceful shutdown with timeout
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
     * Exposes the gRPC server as a Spring bean (optional, for monitoring).
     *
     * @return the gRPC server instance
     */
    @Bean
    public Server grpcServer() {
        return grpcServer;
    }

    /**
     * Exposes the health status manager as a Spring bean (for external monitoring).
     *
     * @return the health status manager instance
     */
    @Bean
    public HealthStatusManager healthStatusManager() {
        return healthStatusManager;
    }
}
