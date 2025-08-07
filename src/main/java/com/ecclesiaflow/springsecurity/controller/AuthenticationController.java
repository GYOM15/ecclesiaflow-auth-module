package com.ecclesiaflow.springsecurity.controller;

import com.ecclesiaflow.springsecurity.domain.AuthenticationResult;
import com.ecclesiaflow.springsecurity.domain.SigninCredentials;
import com.ecclesiaflow.springsecurity.dto.JwtAuthenticationResponse;
import com.ecclesiaflow.springsecurity.dto.RefreshTokenRequest;
import com.ecclesiaflow.springsecurity.dto.SigninRequest;
import com.ecclesiaflow.springsecurity.exception.InvalidCredentialsException;
import com.ecclesiaflow.springsecurity.exception.InvalidTokenException;
import com.ecclesiaflow.springsecurity.exception.JwtProcessingException;
import com.ecclesiaflow.springsecurity.services.AuthenticationService;
import com.ecclesiaflow.springsecurity.util.AuthenticationMapper;
import com.ecclesiaflow.springsecurity.util.MemberMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "API d'authentification centralisée pour EcclesiaFlow")
public class AuthenticationController {
    private final AuthenticationService authenticationService;

    @PostMapping(value = "/token", produces = "application/vnd.ecclesiaflow.auth.v1+json")
    @Operation(
            summary = "Génération de token d'authentification",
            description = "Authentifie un utilisateur et génère un token JWT pour l'accès aux ressources"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Token généré avec succès",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = JwtAuthenticationResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Données d'authentification invalides",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Identifiants incorrects",
                    content = @Content
            )
    })
    public ResponseEntity<JwtAuthenticationResponse> generateToken(@Valid @RequestBody SigninRequest request) throws InvalidCredentialsException, InvalidTokenException, JwtProcessingException {
        SigninCredentials credentials = MemberMapper.fromSigninRequest(request);
        AuthenticationResult authResult = authenticationService.getAuthenticatedMember(credentials);
        JwtAuthenticationResponse response = AuthenticationMapper.toDto(authResult);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/refresh", produces = "application/vnd.ecclesiaflow.auth.v1+json")
    @Operation(
            summary = "Rafraîchissement du token JWT",
            description = "Génère un nouveau token JWT à partir d'un refresh token valide"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Token rafraîchi avec succès",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = JwtAuthenticationResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Refresh token invalide ou expiré",
                    content = @Content
            )
    })
    public ResponseEntity<JwtAuthenticationResponse> refreshToken(@RequestBody RefreshTokenRequest refreshTokenRequest) {
        AuthenticationResult authResult = authenticationService.refreshToken(refreshTokenRequest);
        // Utilisation du mapper pour convertir le domaine en DTO
        JwtAuthenticationResponse response = AuthenticationMapper.toDto(authResult);
        return ResponseEntity.ok(response);
    }
}
