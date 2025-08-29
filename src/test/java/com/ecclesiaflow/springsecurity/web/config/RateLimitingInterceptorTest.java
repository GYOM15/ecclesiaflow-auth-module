package com.ecclesiaflow.springsecurity.web.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour RateLimitingInterceptor
 *
 * Teste la logique de limitation de débit (rate limiting) pour protéger
 * les endpoints sensibles contre les attaques par déni de service.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitingInterceptor - Tests de limitation de débit")
class RateLimitingInterceptorTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Object handler;

    private RateLimitingInterceptor rateLimitingInterceptor;

    @BeforeEach
    void setUp() {
        rateLimitingInterceptor = new RateLimitingInterceptor();
    }

    @Test
    @DisplayName("Devrait permettre les requêtes sur endpoints non protégés")
    void shouldAllowRequestsOnUnprotectedEndpoints() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/ecclesiaflow/public/info");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");

        // When
        boolean result = rateLimitingInterceptor.preHandle(request, response, handler);

        // Then
        assertThat(result).isTrue();
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    @DisplayName("Devrait permettre les premières requêtes sur endpoints protégés")
    void shouldAllowFirstRequestsOnProtectedEndpoints() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/ecclesiaflow/auth/signin");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);

        // When
        boolean result = rateLimitingInterceptor.preHandle(request, response, handler);

        // Then
        assertThat(result).isTrue();
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    @DisplayName("Devrait bloquer après 5 tentatives sur endpoint auth")
    void shouldBlockAfterFiveAttemptsOnAuthEndpoint() throws Exception {
        // Given
        String clientIp = "192.168.1.100";
        StringWriter responseWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(responseWriter);

        when(request.getRequestURI()).thenReturn("/ecclesiaflow/auth/signin");
        when(request.getRemoteAddr()).thenReturn(clientIp);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(response.getWriter()).thenReturn(printWriter);

        // When - Faire 5 requêtes autorisées
        for (int i = 0; i < 5; i++) {
            boolean result = rateLimitingInterceptor.preHandle(request, response, handler);
            assertThat(result).isTrue();
        }

        // Then - La 6ème requête devrait être bloquée
        boolean blockedResult = rateLimitingInterceptor.preHandle(request, response, handler);
        assertThat(blockedResult).isFalse();

        verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        verify(response).setContentType("application/json");

        printWriter.flush();
        assertThat(responseWriter.toString()).contains("Trop de tentatives");
    }

    @Test
    @DisplayName("Devrait bloquer après 5 tentatives sur endpoint signup")
    void shouldBlockAfterFiveAttemptsOnSignupEndpoint() throws Exception {
        // Given
        String clientIp = "192.168.1.101";
        StringWriter responseWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(responseWriter);

        when(request.getRequestURI()).thenReturn("/ecclesiaflow/members/signup");
        when(request.getRemoteAddr()).thenReturn(clientIp);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(response.getWriter()).thenReturn(printWriter);

        // When - Faire 5 requêtes autorisées
        for (int i = 0; i < 5; i++) {
            boolean result = rateLimitingInterceptor.preHandle(request, response, handler);
            assertThat(result).isTrue();
        }

        // Then - La 6ème requête devrait être bloquée
        boolean blockedResult = rateLimitingInterceptor.preHandle(request, response, handler);
        assertThat(blockedResult).isFalse();

        verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        verify(response).setContentType("application/json");
    }

    @Test
    @DisplayName("Devrait gérer différentes IPs indépendamment")
    void shouldHandleDifferentIPsIndependently() throws Exception {
        // Given
        String ip1 = "192.168.1.100";
        String ip2 = "192.168.1.101";
        StringWriter responseWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(responseWriter);

        when(request.getRequestURI()).thenReturn("/ecclesiaflow/auth/signin");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(response.getWriter()).thenReturn(printWriter);

        // When - IP1 fait 5 requêtes
        when(request.getRemoteAddr()).thenReturn(ip1);
        for (int i = 0; i < 5; i++) {
            boolean result = rateLimitingInterceptor.preHandle(request, response, handler);
            assertThat(result).isTrue();
        }

        // Then - IP2 devrait encore pouvoir faire des requêtes
        when(request.getRemoteAddr()).thenReturn(ip2);
        boolean result = rateLimitingInterceptor.preHandle(request, response, handler);
        assertThat(result).isTrue();

        // But IP1 devrait être bloquée
        when(request.getRemoteAddr()).thenReturn(ip1);
        boolean blockedResult = rateLimitingInterceptor.preHandle(request, response, handler);
        assertThat(blockedResult).isFalse();

        verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        verify(response).setContentType("application/json");
    }

    @Test
    @DisplayName("Devrait extraire IP depuis X-Forwarded-For header et bloquer après la limite")
    void shouldExtractIPFromXForwardedForHeaderAndBlock() throws Exception {
        // Given
        String forwardedIp = "203.0.113.1";
        String proxyIp = "192.168.1.1";
        StringWriter responseWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(responseWriter);

        when(request.getRequestURI()).thenReturn("/ecclesiaflow/auth/signin");
        when(request.getHeader("X-Forwarded-For")).thenReturn(forwardedIp + ", " + proxyIp);
        when(response.getWriter()).thenReturn(printWriter);

        // When - Faire 5 requêtes avec la même IP forwarded
        for (int i = 0; i < 5; i++) {
            boolean result = rateLimitingInterceptor.preHandle(request, response, handler);
            assertThat(result).isTrue();
        }

        // Then - La 6ème requête devrait être bloquée en se basant sur l'IP du header
        boolean blockedResult = rateLimitingInterceptor.preHandle(request, response, handler);
        assertThat(blockedResult).isFalse();

        verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        verify(response).setContentType("application/json");
    }

    @Test
    @DisplayName("Devrait permettre les requêtes sur différents endpoints protégés")
    void shouldAllowRequestsOnDifferentProtectedEndpoints() throws Exception {
        // Given
        String clientIp = "192.168.1.102";
        when(request.getRemoteAddr()).thenReturn(clientIp);
        lenient().when(request.getHeader("X-Forwarded-For")).thenReturn(null);

        // When - Tester différents endpoints protégés
        String[] protectedEndpoints = {
                "/ecclesiaflow/auth/signin",
                "/ecclesiaflow/auth/token",
                "/ecclesiaflow/auth/refresh",
                "/ecclesiaflow/members/signup"
        };

        for (String endpoint : protectedEndpoints) {
            when(request.getRequestURI()).thenReturn(endpoint);
            boolean result = rateLimitingInterceptor.preHandle(request, response, handler);
            assertThat(result).as("Should allow request to %s", endpoint).isTrue();
        }
    }

    @Test
    @DisplayName("Devrait gérer les requêtes avec URI null")
    void shouldHandleRequestsWithNullURI() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");

        // When
        boolean result = rateLimitingInterceptor.preHandle(request, response, handler);

        // Then - Devrait permettre la requête (pas d'endpoint protégé détecté)
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Devrait gérer les requêtes avec IP null")
    void shouldHandleRequestsWithNullIP() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/ecclesiaflow/auth/signin");
        when(request.getRemoteAddr()).thenReturn(null);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);

        // When
        boolean result = rateLimitingInterceptor.preHandle(request, response, handler);

        // Then - Devrait permettre la requête (utilise "null" comme clé)
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Devrait être thread-safe pour requêtes concurrentes")
    void shouldBeThreadSafeForConcurrentRequests() throws Exception {
        // Given
        String clientIp = "192.168.1.103";
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        boolean[] results = new boolean[threadCount];

        // When - Simuler des requêtes concurrentes avec des mocks séparés
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    // Créer des mocks séparés pour chaque thread
                    HttpServletRequest threadRequest = mock(HttpServletRequest.class);
                    HttpServletResponse threadResponse = mock(HttpServletResponse.class);

                    lenient().when(threadRequest.getRequestURI()).thenReturn("/ecclesiaflow/auth/signin");
                    lenient().when(threadRequest.getRemoteAddr()).thenReturn(clientIp);
                    lenient().when(threadRequest.getHeader("X-Forwarded-For")).thenReturn(null);

                    // The preHandle call for the 6th thread and beyond will try to write
                    // so we need to prepare the mock.
                    StringWriter threadResponseWriter = new StringWriter();
                    PrintWriter threadPrintWriter = new PrintWriter(threadResponseWriter);
                    lenient().when(threadResponse.getWriter()).thenReturn(threadPrintWriter);

                    results[index] = rateLimitingInterceptor.preHandle(threadRequest, threadResponse, handler);
                } catch (Exception e) {
                    results[index] = false;
                }
            });
            threads[i].start();
        }

        // Attendre que tous les threads se terminent
        for (Thread thread : threads) {
            thread.join();
        }

        // Then - Au maximum 5 requêtes devraient être autorisées (bucket limit)
        int allowedRequests = 0;
        for (boolean result : results) {
            if (result) allowedRequests++;
        }

        assertThat(allowedRequests).isLessThanOrEqualTo(5);
        assertThat(allowedRequests).isGreaterThan(0); // Au moins une requête devrait passer
    }

    @Test
    @DisplayName("Devrait gérer les exceptions lors de l'écriture de la réponse")
    void shouldHandleExceptionsWhenWritingResponse() throws Exception {
        // Given
        String clientIp = "192.168.1.104";
        when(request.getRequestURI()).thenReturn("/ecclesiaflow/auth/signin");
        when(request.getRemoteAddr()).thenReturn(clientIp);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);

        // Simuler une exception lors de l'écriture
        when(response.getWriter()).thenThrow(new RuntimeException("IO Error"));

        // When - Épuiser le bucket
        for (int i = 0; i < 5; i++) {
            rateLimitingInterceptor.preHandle(request, response, handler);
        }

        // Then - Devrait gérer l'exception gracieusement
        assertThatThrownBy(() -> rateLimitingInterceptor.preHandle(request, response, handler))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("IO Error");
    }
}