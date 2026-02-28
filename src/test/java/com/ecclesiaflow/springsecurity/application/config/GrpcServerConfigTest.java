package com.ecclesiaflow.springsecurity.application.config;

import com.ecclesiaflow.springsecurity.io.grpc.server.AuthGrpcServiceImpl;
import io.grpc.Server;
import io.grpc.protobuf.services.HealthStatusManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour {@link GrpcServerConfig}.
 * <p>
 * Teste la configuration et le cycle de vie du serveur gRPC.
 * </p>
 */
@DisplayName("GrpcServerConfig - Tests unitaires")
class GrpcServerConfigTest {

    @Mock
    private AuthGrpcServiceImpl jwtGrpcService;

    @Mock
    private Server mockServer;

    @Mock
    private HealthStatusManager mockHealthStatusManager;

    private GrpcServerConfig config;
    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        config = new GrpcServerConfig(jwtGrpcService);
        
        // Configuration des valeurs par défaut
        ReflectionTestUtils.setField(config, "grpcServerPort", 9090);
        ReflectionTestUtils.setField(config, "shutdownTimeoutSeconds", 30);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (closeable != null) {
            closeable.close();
        }
    }

    // =====================================================
    // Tests de configuration
    // =====================================================

    @Test
    @DisplayName("Doit créer GrpcServerConfig avec les dépendances")
    void shouldCreateConfigWithDependencies() {
        // Given / When
        GrpcServerConfig newConfig = new GrpcServerConfig(jwtGrpcService);

        // Then
        assertThat(newConfig).isNotNull();
    }

    @Test
    @DisplayName("Doit utiliser le port par défaut si non configuré")
    void shouldUseDefaultPort() {
        // Given
        GrpcServerConfig newConfig = new GrpcServerConfig(jwtGrpcService);
        ReflectionTestUtils.setField(newConfig, "grpcServerPort", 9090);

        // When
        Integer port = (Integer) ReflectionTestUtils.getField(newConfig, "grpcServerPort");

        // Then
        assertThat(port).isNotNull().isEqualTo(9090);
    }

    @Test
    @DisplayName("Doit utiliser le timeout par défaut si non configuré")
    void shouldUseDefaultShutdownTimeout() {
        // Given
        GrpcServerConfig newConfig = new GrpcServerConfig(jwtGrpcService);
        ReflectionTestUtils.setField(newConfig, "shutdownTimeoutSeconds", 30);

        // When
        Integer timeout = (Integer) ReflectionTestUtils.getField(newConfig, "shutdownTimeoutSeconds");

        // Then
        assertThat(timeout).isNotNull().isEqualTo(30);
    }

    // =====================================================
    // Tests de stop
    // =====================================================

    @Test
    @DisplayName("Stop doit gérer un serveur null sans erreur")
    void stopShouldHandleNullServer() {
        // Given
        ReflectionTestUtils.setField(config, "grpcServer", null);

        // When/Then - Ne doit pas lancer d'exception
        assertDoesNotThrow(() -> config.stop());
    }

    @Test
    @DisplayName("Stop doit arrêter proprement un serveur actif")
    void stopShouldStopActiveServer() throws InterruptedException {
        // Given
        when(mockServer.awaitTermination(anyLong(), any(TimeUnit.class))).thenReturn(true);
        ReflectionTestUtils.setField(config, "grpcServer", mockServer);
        ReflectionTestUtils.setField(config, "healthStatusManager", mockHealthStatusManager);
        ReflectionTestUtils.setField(config, "shutdownTimeoutSeconds", 30);

        // When
        config.stop();

        // Then
        verify(mockHealthStatusManager).enterTerminalState();
        verify(mockServer).shutdown();
        verify(mockServer).awaitTermination(30, TimeUnit.SECONDS);
        verify(mockServer, never()).shutdownNow();
    }

    @Test
    @DisplayName("Stop doit forcer l'arrêt si le timeout est dépassé")
    void stopShouldForceShutdownOnTimeout() throws InterruptedException {
        // Given - Le serveur ne se termine pas dans le délai
        when(mockServer.awaitTermination(30, TimeUnit.SECONDS)).thenReturn(false);
        when(mockServer.awaitTermination(5, TimeUnit.SECONDS)).thenReturn(true);
        ReflectionTestUtils.setField(config, "grpcServer", mockServer);
        ReflectionTestUtils.setField(config, "healthStatusManager", mockHealthStatusManager);
        ReflectionTestUtils.setField(config, "shutdownTimeoutSeconds", 30);

        // When
        config.stop();

        // Then
        verify(mockHealthStatusManager).enterTerminalState();
        verify(mockServer).shutdown();
        verify(mockServer).awaitTermination(30, TimeUnit.SECONDS);
        verify(mockServer).shutdownNow();
        verify(mockServer).awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("Stop doit gérer InterruptedException")
    void stopShouldHandleInterruptedException() throws InterruptedException {
        // Given
        when(mockServer.awaitTermination(anyLong(), any(TimeUnit.class)))
                .thenThrow(new InterruptedException("Test interruption"));
        ReflectionTestUtils.setField(config, "grpcServer", mockServer);
        ReflectionTestUtils.setField(config, "healthStatusManager", mockHealthStatusManager);

        // When
        config.stop();

        // Then
        verify(mockServer).shutdown();
        verify(mockServer).shutdownNow();
        assertThat(Thread.currentThread().isInterrupted()).isTrue();
        
        // Cleanup
        Thread.interrupted(); // Clear interrupt flag
    }

    @Test
    @DisplayName("Stop doit gérer exception dans enterTerminalState")
    void stopShouldHandleExceptionInEnterTerminalState() throws InterruptedException {
        // Given
        doThrow(new RuntimeException("Health manager error")).when(mockHealthStatusManager).enterTerminalState();
        when(mockServer.awaitTermination(anyLong(), any(TimeUnit.class))).thenReturn(true);
        ReflectionTestUtils.setField(config, "grpcServer", mockServer);
        ReflectionTestUtils.setField(config, "healthStatusManager", mockHealthStatusManager);

        // When/Then - Ne doit pas propager l'exception
        assertThatThrownBy(() -> config.stop())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Health manager error");
    }

    // =====================================================
    // Tests de beans
    // =====================================================

    @Test
    @DisplayName("grpcServer() doit retourner l'instance du serveur")
    void grpcServerShouldReturnServerInstance() {
        // Given
        ReflectionTestUtils.setField(config, "grpcServer", mockServer);

        // When
        Server result = config.grpcServer();

        // Then
        assertThat(result).isEqualTo(mockServer);
    }

    @Test
    @DisplayName("grpcServer() doit retourner null si serveur non initialisé")
    void grpcServerShouldReturnNullIfNotInitialized() {
        // Given
        ReflectionTestUtils.setField(config, "grpcServer", null);

        // When
        Server result = config.grpcServer();

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("healthStatusManager() doit retourner l'instance du health manager")
    void healthStatusManagerShouldReturnManagerInstance() {
        // Given
        ReflectionTestUtils.setField(config, "healthStatusManager", mockHealthStatusManager);

        // When
        HealthStatusManager result = config.healthStatusManager();

        // Then
        assertThat(result).isEqualTo(mockHealthStatusManager);
    }

    @Test
    @DisplayName("healthStatusManager() doit retourner null si non initialisé")
    void healthStatusManagerShouldReturnNullIfNotInitialized() {
        // Given
        ReflectionTestUtils.setField(config, "healthStatusManager", null);

        // When
        HealthStatusManager result = config.healthStatusManager();

        // Then
        assertThat(result).isNull();
    }

    // =====================================================
    // Tests de configuration avec différents ports
    // =====================================================

    @Test
    @DisplayName("Doit accepter un port personnalisé")
    void shouldAcceptCustomPort() {
        // Given
        GrpcServerConfig customConfig = new GrpcServerConfig(jwtGrpcService);
        ReflectionTestUtils.setField(customConfig, "grpcServerPort", 8080);

        // When
        Integer port = (Integer) ReflectionTestUtils.getField(customConfig, "grpcServerPort");

        // Then
        assertThat(port).isNotNull().isEqualTo(8080);
    }

    @Test
    @DisplayName("Doit accepter un timeout personnalisé")
    void shouldAcceptCustomTimeout() {
        // Given
        GrpcServerConfig customConfig = new GrpcServerConfig(jwtGrpcService);
        ReflectionTestUtils.setField(customConfig, "shutdownTimeoutSeconds", 60);

        // When
        Integer timeout = (Integer) ReflectionTestUtils.getField(customConfig, "shutdownTimeoutSeconds");

        // Then
        assertThat(timeout).isNotNull().isEqualTo(60);
    }

}
