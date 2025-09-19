package com.ecclesiaflow.springsecurity.web.controller;

import com.ecclesiaflow.springsecurity.business.domain.Tokens;
import com.ecclesiaflow.springsecurity.business.domain.SigninCredentials;
import com.ecclesiaflow.springsecurity.business.domain.RefreshTokenCredentials;
import com.ecclesiaflow.springsecurity.io.entities.Member;
import com.ecclesiaflow.springsecurity.web.dto.JwtAuthenticationResponse;
import com.ecclesiaflow.springsecurity.web.payloads.RefreshTokenRequest;
import com.ecclesiaflow.springsecurity.web.payloads.SigninRequest;
import com.ecclesiaflow.springsecurity.web.exception.InvalidCredentialsException;
import com.ecclesiaflow.springsecurity.web.exception.InvalidTokenException;
import com.ecclesiaflow.springsecurity.web.exception.JwtProcessingException;
import com.ecclesiaflow.springsecurity.business.services.AuthenticationService;
import com.ecclesiaflow.springsecurity.web.security.Jwt;
import com.ecclesiaflow.springsecurity.web.mappers.AuthenticationMapper;
import com.ecclesiaflow.springsecurity.web.mappers.MemberMapper;
import com.ecclesiaflow.springsecurity.web.mappers.TemporaryTokenMapper;
import com.ecclesiaflow.springsecurity.web.payloads.TemporaryTokenRequest;
import com.ecclesiaflow.springsecurity.web.dto.TemporaryTokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrôleur REST pour l'authentification des utilisateurs EcclesiaFlow.
 * <p>
 * Cette classe expose les endpoints HTTP pour l'authentification, la génération
 * de tokens JWT et le rafraîchissement des tokens. Fait partie de la couche web
 * et orchestre les appels aux services métier et utilitaires techniques.
 * </p>
 * 
 * <p><strong>Responsabilités principales :</strong></p>
 * <ul>
 *   <li>Exposition des endpoints d'authentification REST</li>
 *   <li>Validation des requêtes HTTP entrantes</li>
 *   <li>Orchestration entre services métier et utilitaires techniques</li>
 *   <li>Transformation des réponses métier en DTOs web</li>
 * </ul>
 * 
 * <p><strong>Endpoints exposés :</strong></p>
 * <ul>
 *   <li>POST /ecclesiaflow/auth/token - Authentification et génération de tokens</li>
 *   <li>POST /ecclesiaflow/auth/refresh - Rafraîchissement des tokens</li>
 * </ul>
 * 
 * <p><strong>Garanties :</strong> Thread-safe, stateless, gestion d'erreurs HTTP.</p>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/ecclesiaflow/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "API d'authentification EcclesiaFlow")
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final Jwt jwt;
    private final TemporaryTokenMapper temporaryTokenMapper;

    /**
     * Génère un token JWT pour l'authentification d'un utilisateur.
     * 
     * @param request Requête d'authentification contenant les identifiants de l'utilisateur
     * @return Réponse d'authentification contenant le token JWT
     * @throws InvalidCredentialsException si les identifiants sont invalides
     * @throws InvalidTokenException si le token est invalide
     * @throws JwtProcessingException si une erreur se produit lors du traitement du token
     */
    @PostMapping(value = "/token", produces = "application/vnd.ecclesiaflow.auth.v1+json")
    @Operation(
            summary = "Génération de token JWT",
            description = "Authentifie un utilisateur et génère un token JWT d'accès ainsi qu'un refresh token"
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
        Member member = authenticationService.getAuthenticatedMember(credentials);
        Tokens tokens = jwt.generateUserTokens(member);
        JwtAuthenticationResponse response = AuthenticationMapper.toDto(tokens);
        return ResponseEntity.ok(response);
    }

    /**
     * Rafraîchit un token JWT existant.
     * 
     * @param refreshTokenRequest Requête de rafraîchissement du token
     * @return Réponse d'authentification contenant le nouveau token JWT
     */
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
    public ResponseEntity<JwtAuthenticationResponse> refreshToken(@RequestBody RefreshTokenRequest refreshTokenRequest) throws InvalidTokenException, JwtProcessingException {
        RefreshTokenCredentials refreshTokenCredentials = AuthenticationMapper.fromRefreshTokenRequest(refreshTokenRequest);
        String email = jwt.validateAndExtractEmail(refreshTokenCredentials.getRefreshToken());
        Member member = authenticationService.getMemberByEmail(email);
        Tokens refreshedTokens = jwt.refreshTokenForMember(refreshTokenCredentials.getRefreshToken(), member);
        JwtAuthenticationResponse response = AuthenticationMapper.toDto(refreshedTokens);
        return ResponseEntity.ok(response);
    }

    /**
     * Génère un token temporaire pour la confirmation d'inscription.
     *
     * @param temporaryTokenRequest Requête contenant l'email pour générer le token temporaire
     * @return Réponse contenant le token temporaire
     */
    @PostMapping(value = "/temporary-token", produces = "application/vnd.ecclesiaflow.auth.v1+json")
    @Operation(
            summary = "Génération de token temporaire",
            description = "Génère un token temporaire pour la confirmation d'inscription d'un membre"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Token temporaire généré avec succès",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TemporaryTokenResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Email invalide",
                    content = @Content
            )
    })
    public ResponseEntity<TemporaryTokenResponse> generateTemporaryToken(@Valid @RequestBody TemporaryTokenRequest temporaryTokenRequest) throws InvalidTokenException, JwtProcessingException {
        String email = temporaryTokenMapper.extractEmail(temporaryTokenRequest);
        String temporaryToken = jwt.generateTemporaryToken(email);
        TemporaryTokenResponse response = temporaryTokenMapper.toResponse(temporaryToken);
        return ResponseEntity.ok(response);
    }

}
