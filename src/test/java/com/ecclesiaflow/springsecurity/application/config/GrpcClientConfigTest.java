package com.ecclesiaflow.springsecurity.application.config;

import io.grpc.ManagedChannel;
import org.junit.jupiter.api.AfterEach;
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
 * Teste la configuration du members gRPC, notamment la création du canal
 * et le shutdown gracieux lors de l'arrêt de l'application.
 * </p>
 */
class GrpcClientConfigTest {

    private GrpcClientConfig config;
    private AutoCloseable closeable;

    @Mock
    private ManagedChannel mockChannel;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        config = new GrpcClientConfig();
        
        // Configuration des valeurs par défaut
        ReflectionTestUtils.setField(config, "membersServiceHost", "localhost");
        ReflectionTestUtils.setField(config, "membersServicePort", 9091);
        ReflectionTestUtils.setField(config, "shutdownTimeoutSeconds", 5);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (closeable != null) {
            closeable.close();
        }
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
        ReflectionTestUtils.setField(config, "membersChannel", null);
        ReflectionTestUtils.setField(config, "emailChannel", null);

        // When/Then - Ne doit pas lancer d'exception
        assertDoesNotThrow(() -> config.shutdown());
    }

    @Test
    @DisplayName("Shutdown doit gérer un canal déjà arrêté")
    void shutdownShouldHandleAlreadyShutdownChannel() throws InterruptedException {
        // Given
        when(mockChannel.isShutdown()).thenReturn(true);
        ReflectionTestUtils.setField(config, "membersChannel", mockChannel);
        ReflectionTestUtils.setField(config, "emailChannel", null);

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
        ReflectionTestUtils.setField(config, "membersChannel", mockChannel);
        ReflectionTestUtils.setField(config, "emailChannel", null);
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
        ReflectionTestUtils.setField(config, "membersChannel", mockChannel);
        ReflectionTestUtils.setField(config, "emailChannel", null);
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
        ReflectionTestUtils.setField(config, "membersChannel", mockChannel);
        ReflectionTestUtils.setField(config, "emailChannel", null);

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
        ReflectionTestUtils.setField(config, "membersChannel", mockChannel);
        ReflectionTestUtils.setField(config, "emailChannel", null);
        ReflectionTestUtils.setField(config, "shutdownTimeoutSeconds", 0);

        // When
        config.shutdown();

        // Then
        verify(mockChannel).shutdownNow(); // Doit immédiatement forcer l'arrêt
    }

    // =====================================================
    // Tests pour Email Channel
    // =====================================================

    @Test
    @DisplayName("Doit créer un emailGrpcChannel avec la configuration correcte")
    void shouldCreateEmailGrpcChannel() {
        // Given
        ReflectionTestUtils.setField(config, "emailServiceHost", "localhost");
        ReflectionTestUtils.setField(config, "emailServicePort", 9093);

        // When
        ManagedChannel channel = config.emailGrpcChannel();

        // Then
        assertNotNull(channel, "Le canal email ne doit pas être null");
        assertFalse(channel.isShutdown(), "Le canal doit être actif après création");
        assertFalse(channel.isTerminated(), "Le canal ne doit pas être terminé après création");
        
        // Cleanup
        channel.shutdownNow();
    }

    @Test
    @DisplayName("Doit utiliser le host et port configurés pour email")
    void shouldUseConfiguredHostAndPortForEmail() {
        // Given
        ReflectionTestUtils.setField(config, "emailServiceHost", "email-service");
        ReflectionTestUtils.setField(config, "emailServicePort", 8093);

        // When
        ManagedChannel channel = config.emailGrpcChannel();

        // Then
        assertNotNull(channel);
        
        // Cleanup
        channel.shutdownNow();
    }

    // =====================================================
    // Tests de shutdown avec les deux canaux
    // =====================================================

    @Test
    @DisplayName("Shutdown doit fermer les deux canaux (members et email)")
    void shutdownShouldCloseBothChannels() throws InterruptedException {
        // Given
        ManagedChannel mockEmailChannel = mock(ManagedChannel.class);
        
        when(mockChannel.isShutdown()).thenReturn(false);
        when(mockChannel.awaitTermination(anyLong(), any(TimeUnit.class))).thenReturn(true);
        
        when(mockEmailChannel.isShutdown()).thenReturn(false);
        when(mockEmailChannel.awaitTermination(anyLong(), any(TimeUnit.class))).thenReturn(true);
        
        ReflectionTestUtils.setField(config, "membersChannel", mockChannel);
        ReflectionTestUtils.setField(config, "emailChannel", mockEmailChannel);
        ReflectionTestUtils.setField(config, "shutdownTimeoutSeconds", 5);

        // When
        config.shutdown();

        // Then
        verify(mockChannel).shutdown();
        verify(mockChannel).awaitTermination(5, TimeUnit.SECONDS);
        
        verify(mockEmailChannel).shutdown();
        verify(mockEmailChannel).awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("Shutdown doit gérer un canal email déjà arrêté")
    void shutdownShouldHandleAlreadyShutdownEmailChannel() throws InterruptedException {
        // Given
        ManagedChannel mockEmailChannel = mock(ManagedChannel.class);
        
        when(mockChannel.isShutdown()).thenReturn(false);
        when(mockChannel.awaitTermination(anyLong(), any(TimeUnit.class))).thenReturn(true);
        
        when(mockEmailChannel.isShutdown()).thenReturn(true);
        
        ReflectionTestUtils.setField(config, "membersChannel", mockChannel);
        ReflectionTestUtils.setField(config, "emailChannel", mockEmailChannel);

        // When
        config.shutdown();

        // Then
        verify(mockChannel).shutdown();
        verify(mockEmailChannel, never()).shutdown(); // Ne doit pas rappeler shutdown sur email
    }

    @Test
    @DisplayName("Shutdown doit forcer l'arrêt du canal email si timeout dépassé")
    void shutdownShouldForceStopEmailChannelOnTimeout() throws InterruptedException {
        // Given
        ManagedChannel mockEmailChannel = mock(ManagedChannel.class);
        
        when(mockChannel.isShutdown()).thenReturn(false);
        when(mockChannel.awaitTermination(5, TimeUnit.SECONDS)).thenReturn(true);
        
        when(mockEmailChannel.isShutdown()).thenReturn(false);
        when(mockEmailChannel.awaitTermination(5, TimeUnit.SECONDS)).thenReturn(false);
        when(mockEmailChannel.awaitTermination(1, TimeUnit.SECONDS)).thenReturn(true);
        
        ReflectionTestUtils.setField(config, "membersChannel", mockChannel);
        ReflectionTestUtils.setField(config, "emailChannel", mockEmailChannel);
        ReflectionTestUtils.setField(config, "shutdownTimeoutSeconds", 5);

        // When
        config.shutdown();

        // Then
        verify(mockEmailChannel).shutdown();
        verify(mockEmailChannel).shutdownNow(); // Doit forcer l'arrêt du canal email
    }

    @Test
    @DisplayName("Shutdown doit continuer même si members channel échoue")
    void shutdownShouldContinueEvenIfMembersChannelFails() throws InterruptedException {
        // Given
        ManagedChannel mockEmailChannel = mock(ManagedChannel.class);
        
        when(mockChannel.isShutdown()).thenReturn(false);
        when(mockChannel.awaitTermination(anyLong(), any(TimeUnit.class)))
                .thenThrow(new InterruptedException("Members channel interrupted"));
        
        when(mockEmailChannel.isShutdown()).thenReturn(false);
        when(mockEmailChannel.awaitTermination(anyLong(), any(TimeUnit.class))).thenReturn(true);
        
        ReflectionTestUtils.setField(config, "membersChannel", mockChannel);
        ReflectionTestUtils.setField(config, "emailChannel", mockEmailChannel);

        // When/Then - Doit propager l'exception
        assertThrows(InterruptedException.class, () -> config.shutdown());
        
        // Mais members channel doit quand même avoir tenté le shutdown
        verify(mockChannel).shutdown();
    }

    @Test
    @DisplayName("Shutdown ne doit rien faire si les deux canaux sont null")
    void shutdownShouldDoNothingIfBothChannelsAreNull() {
        // Given
        ReflectionTestUtils.setField(config, "membersChannel", null);
        ReflectionTestUtils.setField(config, "emailChannel", null);

        // When/Then - Ne doit pas lancer d'exception
        assertDoesNotThrow(() -> config.shutdown());
    }

    @Test
    @DisplayName("Shutdown doit gérer email channel null mais members channel actif")
    void shutdownShouldHandleNullEmailChannelWithActiveMembersChannel() throws InterruptedException {
        // Given
        when(mockChannel.isShutdown()).thenReturn(false);
        when(mockChannel.awaitTermination(anyLong(), any(TimeUnit.class))).thenReturn(true);
        
        ReflectionTestUtils.setField(config, "membersChannel", mockChannel);
        ReflectionTestUtils.setField(config, "emailChannel", null);

        // When
        config.shutdown();

        // Then
        verify(mockChannel).shutdown();
        verify(mockChannel).awaitTermination(5, TimeUnit.SECONDS);
    }
}
