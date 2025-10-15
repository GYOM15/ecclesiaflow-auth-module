package com.ecclesiaflow.springsecurity.web.security;

import com.ecclesiaflow.springsecurity.web.exception.model.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;

import java.io.IOException;
import java.io.PrintWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomAuthenticationEntryPoint - Tests Unitaires")
class CustomAuthenticationEntryPointTest {

    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private AuthenticationException authException;
    @Mock
    private PrintWriter printWriter;

    @InjectMocks
    private CustomAuthenticationEntryPoint entryPoint;

    private static final String REQUEST_URI = "/api/protected";
    private static final String MOCK_JSON_RESPONSE = "{\"status\":401, \"error\":\"Unauthorized\"}";

    @BeforeEach
    void setUp() throws IOException {
        // Configuration de base pour la réponse HTTP
        when(response.getWriter()).thenReturn(printWriter);
        when(request.getRequestURI()).thenReturn(REQUEST_URI);
        when(authException.getMessage()).thenReturn("Full Spring Security Error Message");

        // Configuration du ObjectMapper pour simuler la sérialisation
        lenient().when(objectMapper.writeValueAsString(any(ApiErrorResponse.class)))
                .thenReturn(MOCK_JSON_RESPONSE);
    }

    @Test
    @DisplayName("La méthode commence doit configurer la réponse HTTP 401 et le type JSON")
    void commence_ShouldSetResponseDetails() throws IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid_token");

        // Act
        entryPoint.commence(request, response, authException);

        // Assert
        verify(response).setStatus(HttpStatus.UNAUTHORIZED.value());
        verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
        verify(response).setCharacterEncoding("UTF-8");
        verify(printWriter).write(MOCK_JSON_RESPONSE);
        verify(printWriter).flush();
    }

    @Test
    @DisplayName("La méthode commence doit sérialiser un ApiErrorResponse structuré")
    void commence_ShouldSerializeApiErrorResponse() throws Exception {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid_token");
        ArgumentCaptor<ApiErrorResponse> errorCaptor = ArgumentCaptor.forClass(ApiErrorResponse.class);

        // Act
        entryPoint.commence(request, response, authException);

        // Assert
        verify(objectMapper).writeValueAsString(errorCaptor.capture());
        ApiErrorResponse capturedResponse = errorCaptor.getValue();

        // CORRECTION APPLIQUÉE ICI : Utilisation des accesseurs de Record (status(), error(), path(), errors())
        assertThat(capturedResponse.status()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(capturedResponse.error()).isEqualTo(HttpStatus.UNAUTHORIZED.getReasonPhrase());
        assertThat(capturedResponse.path()).isEqualTo(REQUEST_URI);
        assertThat(capturedResponse.errors()).hasSize(1);
    }

    @Test
    @DisplayName("Erreur: Header manquant - Devrait retourner le message d'absence de header")
    void commence_ShouldReturnMissingHeaderMessage_WhenHeaderIsNull() throws IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn(null);
        ArgumentCaptor<ApiErrorResponse> errorCaptor = ArgumentCaptor.forClass(ApiErrorResponse.class);

        // Act
        entryPoint.commence(request, response, authException);

        // Assert
        verify(objectMapper).writeValueAsString(errorCaptor.capture());
        // CORRECTION APPLIQUÉE ICI : Utilisation de message()
        String message = errorCaptor.getValue().message();
        assertThat(message).contains("Header Authorization manquant");
    }

    @Test
    @DisplayName("Erreur: Préfixe invalide - Devrait retourner le message de format invalide")
    void commence_ShouldReturnInvalidFormatMessage_WhenHeaderHasInvalidPrefix() throws IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcnBhc3M=");
        ArgumentCaptor<ApiErrorResponse> errorCaptor = ArgumentCaptor.forClass(ApiErrorResponse.class);

        // Act
        entryPoint.commence(request, response, authException);

        // Assert
        verify(objectMapper).writeValueAsString(errorCaptor.capture());
        // CORRECTION APPLIQUÉE ICI : Utilisation de message()
        String message = errorCaptor.getValue().message();
        assertThat(message).contains("Format du header Authorization invalide");
    }

    @Test
    @DisplayName("Erreur: Token invalide - Devrait retourner le message de token invalide/expiré")
    void commence_ShouldReturnInvalidTokenMessage_WhenTokenIsPresent() throws IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Bearer expired.jwt.token");
        ArgumentCaptor<ApiErrorResponse> errorCaptor = ArgumentCaptor.forClass(ApiErrorResponse.class);

        // Act
        entryPoint.commence(request, response, authException);

        // Assert
        verify(objectMapper).writeValueAsString(errorCaptor.capture());
        // CORRECTION APPLIQUÉE ICI : Utilisation de message()
        String message = errorCaptor.getValue().message();
        assertThat(message).contains("Token d'authentification invalide ou expiré");
    }

    @Test
    @DisplayName("Devrait écrire un message d'erreur générique si la sérialisation échoue")
    void commence_ShouldHandleSerializationException() throws Exception {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        // Simuler une exception lors de la sérialisation JSON
        when(objectMapper.writeValueAsString(any(ApiErrorResponse.class))).thenThrow(new RuntimeException("JSON error"));

        // Act
        entryPoint.commence(request, response, authException);

        // Assert
        verify(printWriter).write("{\"error\":\"Authentication failed\"}");
        verify(printWriter).flush();
    }

    @Test
    @DisplayName("Erreur: Header vide - Devrait retourner le message d'absence de header")
    void commence_ShouldReturnMissingHeaderMessage_WhenHeaderIsEmpty() throws IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("");
        ArgumentCaptor<ApiErrorResponse> errorCaptor = ArgumentCaptor.forClass(ApiErrorResponse.class);

        // Act
        entryPoint.commence(request, response, authException);

        // Assert
        verify(objectMapper).writeValueAsString(errorCaptor.capture());
        String message = errorCaptor.getValue().message();
        assertThat(message).contains("Header Authorization manquant");
    }

    @Test
    @DisplayName("Erreur: Header avec espaces - Devrait retourner le message d'absence de header")
    void commence_ShouldReturnMissingHeaderMessage_WhenHeaderIsBlank() throws IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("   ");
        ArgumentCaptor<ApiErrorResponse> errorCaptor = ArgumentCaptor.forClass(ApiErrorResponse.class);

        // Act
        entryPoint.commence(request, response, authException);

        // Assert
        verify(objectMapper).writeValueAsString(errorCaptor.capture());
        String message = errorCaptor.getValue().message();
        assertThat(message).contains("Header Authorization manquant");
    }

    @Test
    @DisplayName("Erreur: Header avec Bearer mais sans espace - Devrait retourner format invalide")
    void commence_ShouldReturnInvalidFormatMessage_WhenBearerWithoutSpace() throws IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Bearertoken");
        ArgumentCaptor<ApiErrorResponse> errorCaptor = ArgumentCaptor.forClass(ApiErrorResponse.class);

        // Act
        entryPoint.commence(request, response, authException);

        // Assert
        verify(objectMapper).writeValueAsString(errorCaptor.capture());
        String message = errorCaptor.getValue().message();
        assertThat(message).contains("Format du header Authorization invalide");
    }

    @Test
    @DisplayName("Devrait inclure ValidationError avec les bons champs")
    void commence_ShouldIncludeValidationErrorWithCorrectFields() throws IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        ArgumentCaptor<ApiErrorResponse> errorCaptor = ArgumentCaptor.forClass(ApiErrorResponse.class);

        // Act
        entryPoint.commence(request, response, authException);

        // Assert
        verify(objectMapper).writeValueAsString(errorCaptor.capture());
        ApiErrorResponse capturedResponse = errorCaptor.getValue();
        
        assertThat(capturedResponse.errors()).hasSize(1);
        var validationError = capturedResponse.errors().get(0);
        assertThat(validationError.path()).isEqualTo("authentication");
        assertThat(validationError.type()).isEqualTo("security");
        assertThat(validationError.expected()).isEqualTo("AUTHENTICATION_ERROR");
        assertThat(validationError.received()).isEqualTo("Full Spring Security Error Message");
        assertThat(validationError.code()).isEqualTo("UNAUTHORIZED");
    }

    @Test
    @DisplayName("Devrait utiliser message d'erreur fallback si serialization échoue")
    void commence_ShouldWriteFallbackMessageWhenSerializationFails() throws Exception {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(objectMapper.writeValueAsString(any(ApiErrorResponse.class))).thenThrow(new RuntimeException("JSON error"));

        // Act
        entryPoint.commence(request, response, authException);

        // Assert - Vérifie que le message fallback est écrit
        verify(printWriter).write("{\"error\":\"Authentication failed\"}");
        verify(printWriter).flush();
    }

    @Test
    @DisplayName("Devrait correctement utiliser le URI de la requête dans la réponse")
    void commence_ShouldUseCorrectRequestURIInResponse() throws IOException {
        // Arrange
        String customUri = "/api/v1/custom/endpoint";
        when(request.getRequestURI()).thenReturn(customUri);
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        ArgumentCaptor<ApiErrorResponse> errorCaptor = ArgumentCaptor.forClass(ApiErrorResponse.class);

        // Act
        entryPoint.commence(request, response, authException);

        // Assert
        verify(objectMapper).writeValueAsString(errorCaptor.capture());
        assertThat(errorCaptor.getValue().path()).isEqualTo(customUri);
    }
}