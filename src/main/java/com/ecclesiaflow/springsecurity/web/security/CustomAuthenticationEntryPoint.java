package com.ecclesiaflow.springsecurity.web.security;

import com.ecclesiaflow.springsecurity.web.exception.model.ApiErrorResponse;
import com.ecclesiaflow.springsecurity.web.exception.model.ValidationError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * Point d'entrée personnalisé pour gérer les erreurs d'authentification avec ApiErrorResponse.
 * <p>
 * Cette classe intercepte les erreurs d'authentification de Spring Security et les transforme
 * en réponses JSON structurées utilisant le format ApiErrorResponse, garantissant la cohérence
 * avec le reste de l'API EcclesiaFlow.
 * </p>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                        AuthenticationException authException) throws IOException {

        // Déterminer le type d'erreur d'authentification
        String errorMessage = determineErrorMessage(authException, request);
        HttpStatus status = HttpStatus.UNAUTHORIZED;

        // Créer l'erreur de validation
        ValidationError validationError = new ValidationError(
            errorMessage,
            "authentication",
            "security",
            "AUTHENTICATION_ERROR",
            authException.getMessage(),
            "UNAUTHORIZED",
            null,
            null
        );

        // Construire la réponse d'erreur
        ApiErrorResponse errorResponse = ApiErrorResponse.builder()
            .status(status.value())
            .error(status.getReasonPhrase())
            .message(errorMessage)
            .path(request.getRequestURI())
            .errors(List.of(validationError))
            .build();

        // Configurer la réponse HTTP
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // Écrire la réponse JSON
        try {
            String jsonResponse = objectMapper.writeValueAsString(errorResponse);
            response.getWriter().write(jsonResponse);
            response.getWriter().flush();
        } catch (Exception e) {
            response.getWriter().write("{\"error\":\"Authentication failed\"}");
        }
    }

    /**
     * Détermine le message d'erreur approprié selon le type d'exception d'authentification.
     */
    private String determineErrorMessage(AuthenticationException authException, HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || authHeader.trim().isEmpty()) {
            return "Header Authorization manquant. Format attendu: Bearer {token}";
        }
        
        if (!authHeader.startsWith("Bearer ")) {
            return "Format du header Authorization invalide. Format attendu: Bearer {token}";
        }

        // Token présent mais invalide/expiré
        return "Token d'authentification invalide ou expiré";
    }
}
