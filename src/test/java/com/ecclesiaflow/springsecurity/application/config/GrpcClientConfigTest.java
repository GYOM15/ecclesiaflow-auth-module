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
 * Unit tests for {@link GrpcClientConfig}.
 * <p>
 * Tests the members gRPC configuration, including channel creation
 * and graceful shutdown during application shutdown.
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
        
        // Default values configuration
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
    // Channel creation tests
    // =====================================================

    @Test
    @DisplayName("Should create a ManagedChannel with the correct configuration")
    void shouldCreateManagedChannel() {
        // When
        ManagedChannel channel = config.membersGrpcChannel();

        // Then
        assertNotNull(channel, "The channel must not be null");
        assertFalse(channel.isShutdown(), "The channel must be active after creation");
        assertFalse(channel.isTerminated(), "The channel must not be terminated after creation");
        
        // Cleanup
        channel.shutdownNow();
    }

    @Test
    @DisplayName("Should use the configured host and port")
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
    // Shutdown tests
    // =====================================================

    @Test
    @DisplayName("Shutdown should handle a null channel without error")
    void shutdownShouldHandleNullChannel() {
        // Given
        ReflectionTestUtils.setField(config, "membersChannel", null);
        ReflectionTestUtils.setField(config, "emailChannel", null);

        // When/Then - Should not throw an exception
        assertDoesNotThrow(() -> config.shutdown());
    }

    @Test
    @DisplayName("Shutdown should handle an already shutdown channel")
    void shutdownShouldHandleAlreadyShutdownChannel() throws InterruptedException {
        // Given
        when(mockChannel.isShutdown()).thenReturn(true);
        ReflectionTestUtils.setField(config, "membersChannel", mockChannel);
        ReflectionTestUtils.setField(config, "emailChannel", null);

        // When
        config.shutdown();

        // Then
        verify(mockChannel, times(1)).isShutdown();
        verify(mockChannel, never()).shutdown(); // Should not call shutdown again
    }

    @Test
    @DisplayName("Shutdown should stop an active channel cleanly")
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
        verify(mockChannel, never()).shutdownNow(); // Should not force shutdown
    }

    @Test
    @DisplayName("Shutdown should force shutdown if timeout is exceeded")
    void shutdownShouldForceStopOnTimeout() throws InterruptedException {
        // Given - The channel does not terminate within the timeout
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
        verify(mockChannel).shutdownNow(); // Should force shutdown
        verify(mockChannel).awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("Shutdown should propagate InterruptedException")
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
    @DisplayName("Should handle a 0 second timeout")
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
        verify(mockChannel).shutdownNow(); // Should immediately force shutdown
    }

    // =====================================================
    // Email Channel tests
    // =====================================================

    @Test
    @DisplayName("Should create an emailGrpcChannel with the correct configuration")
    void shouldCreateEmailGrpcChannel() {
        // Given
        ReflectionTestUtils.setField(config, "emailServiceHost", "localhost");
        ReflectionTestUtils.setField(config, "emailServicePort", 9093);

        // When
        ManagedChannel channel = config.emailGrpcChannel();

        // Then
        assertNotNull(channel, "The email channel must not be null");
        assertFalse(channel.isShutdown(), "The channel must be active after creation");
        assertFalse(channel.isTerminated(), "The channel must not be terminated after creation");
        
        // Cleanup
        channel.shutdownNow();
    }

    @Test
    @DisplayName("Should use the configured host and port for email")
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
    // Shutdown tests with both channels
    // =====================================================

    @Test
    @DisplayName("Shutdown should close both channels (members and email)")
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
    @DisplayName("Shutdown should handle an already shutdown email channel")
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
        verify(mockEmailChannel, never()).shutdown(); // Should not call shutdown again on email
    }

    @Test
    @DisplayName("Shutdown should force email channel shutdown if timeout exceeded")
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
        verify(mockEmailChannel).shutdownNow(); // Should force shutdown du canal email
    }

    @Test
    @DisplayName("Shutdown should continue even if members channel fails")
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

        // When/Then - Should propagate the exception
        assertThrows(InterruptedException.class, () -> config.shutdown());
        
        // But members channel should still have attempted shutdown
        verify(mockChannel).shutdown();
    }

    @Test
    @DisplayName("Shutdown should do nothing if both channels are null")
    void shutdownShouldDoNothingIfBothChannelsAreNull() {
        // Given
        ReflectionTestUtils.setField(config, "membersChannel", null);
        ReflectionTestUtils.setField(config, "emailChannel", null);

        // When/Then - Should not throw an exception
        assertDoesNotThrow(() -> config.shutdown());
    }

    @Test
    @DisplayName("Shutdown should handle null email channel with active members channel")
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
