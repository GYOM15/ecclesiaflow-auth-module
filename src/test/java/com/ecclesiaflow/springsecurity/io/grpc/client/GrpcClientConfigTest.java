package com.ecclesiaflow.springsecurity.io.grpc.client;

import io.grpc.ManagedChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour {@link GrpcClientConfig}.
 * <p>
 * Teste la configuration du client gRPC, notamment la création du canal
 * et le shutdown gracieux lors de l'arrêt de l'application.
 * </p>
 */
class GrpcClientConfigTest {

    private GrpcClientConfig config;

    @Mock
    private ManagedChannel mockChannel;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        config = new GrpcClientConfig();
        
        // Configuration des valeurs par défaut
        ReflectionTestUtils.setField(config, "membersServiceHost", "localhost");
        ReflectionTestUtils.setField(config, "membersServicePort", 9091);
        ReflectionTestUtils.setField(config, "shutdownTimeoutSeconds", 5);
    }

    // =====================================================
    // Tests de création du canal
    // =====================================================

    @Test
    @DisplayName("Doit créer un ManagedChannel avec la configuration correcte")
    void shouldCreateManagedChannel() {
        // When
        ManagedChannel channel = config.membersGrpcChannel();

        // Then
        assertNotNull(channel, "Le canal ne doit pas être null");
        assertFalse(channel.isShutdown(), "Le canal doit être actif après création");
        assertFalse(channel.isTerminated(), "Le canal ne doit pas être terminé après création");
        
        // Cleanup
        channel.shutdownNow();
    }

    @Test
    @DisplayName("Doit utiliser le host et port configurés")
    void shouldUseConfiguredHostAndPort() {
        // Given
        ReflectionTestUtils.setField(config, "membersServiceHost", "members-service");
        ReflectionTestUtils.setField(config, "membersServicePort", 8080);

        // When
        ManagedChannel channel = config.membersGrpcChannel();

        // Then
        assertNotNull(channel);
        
        // Cleanup
        channel.shutdownNow();
    }

    // =====================================================
    // Tests de shutdown
    // =====================================================

    @Test
    @DisplayName("Shutdown doit gérer un canal null sans erreur")
    void shutdownShouldHandleNullChannel() {
        // Given
        ReflectionTestUtils.setField(config, "managedChannel", null);

        // When/Then - Ne doit pas lancer d'exception
        assertDoesNotThrow(() -> config.shutdown());
    }

    @Test
    @DisplayName("Shutdown doit gérer un canal déjà arrêté")
    void shutdownShouldHandleAlreadyShutdownChannel() throws InterruptedException {
        // Given
        when(mockChannel.isShutdown()).thenReturn(true);
        ReflectionTestUtils.setField(config, "managedChannel", mockChannel);

        // When
        config.shutdown();

        // Then
        verify(mockChannel, times(1)).isShutdown();
        verify(mockChannel, never()).shutdown(); // Ne doit pas rappeler shutdown
    }

    @Test
    @DisplayName("Shutdown doit arrêter proprement un canal actif")
    void shutdownShouldStopActiveChannel() throws InterruptedException {
        // Given
        when(mockChannel.isShutdown()).thenReturn(false);
        when(mockChannel.awaitTermination(anyLong(), any(TimeUnit.class))).thenReturn(true);
        ReflectionTestUtils.setField(config, "managedChannel", mockChannel);
        ReflectionTestUtils.setField(config, "shutdownTimeoutSeconds", 5);

        // When
        config.shutdown();

        // Then
        verify(mockChannel).isShutdown();
        verify(mockChannel).shutdown();
        verify(mockChannel).awaitTermination(5, TimeUnit.SECONDS);
        verify(mockChannel, never()).shutdownNow(); // Ne doit pas forcer l'arrêt
    }

    @Test
    @DisplayName("Shutdown doit forcer l'arrêt si le timeout est dépassé")
    void shutdownShouldForceStopOnTimeout() throws InterruptedException {
        // Given - Le canal ne se termine pas dans le délai
        when(mockChannel.isShutdown()).thenReturn(false);
        when(mockChannel.awaitTermination(5, TimeUnit.SECONDS)).thenReturn(false);
        when(mockChannel.awaitTermination(1, TimeUnit.SECONDS)).thenReturn(true);
        ReflectionTestUtils.setField(config, "managedChannel", mockChannel);
        ReflectionTestUtils.setField(config, "shutdownTimeoutSeconds", 5);

        // When
        config.shutdown();

        // Then
        verify(mockChannel).isShutdown();
        verify(mockChannel).shutdown();
        verify(mockChannel).awaitTermination(5, TimeUnit.SECONDS);
        verify(mockChannel).shutdownNow(); // Doit forcer l'arrêt
        verify(mockChannel).awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("Shutdown doit propager InterruptedException")
    void shutdownShouldPropagateInterruptedException() throws InterruptedException {
        // Given
        when(mockChannel.isShutdown()).thenReturn(false);
        when(mockChannel.awaitTermination(anyLong(), any(TimeUnit.class)))
                .thenThrow(new InterruptedException("Test interruption"));
        ReflectionTestUtils.setField(config, "managedChannel", mockChannel);

        // When/Then
        assertThrows(InterruptedException.class, () -> config.shutdown());
    }

    @Test
    @DisplayName("Doit gérer un timeout de 0 seconde")
    void shouldHandleZeroTimeout() throws InterruptedException {
        // Given
        when(mockChannel.isShutdown()).thenReturn(false);
        when(mockChannel.awaitTermination(0, TimeUnit.SECONDS)).thenReturn(false);
        when(mockChannel.awaitTermination(1, TimeUnit.SECONDS)).thenReturn(true);
        ReflectionTestUtils.setField(config, "managedChannel", mockChannel);
        ReflectionTestUtils.setField(config, "shutdownTimeoutSeconds", 0);

        // When
        config.shutdown();

        // Then
        verify(mockChannel).shutdownNow(); // Doit immédiatement forcer l'arrêt
    }
}
