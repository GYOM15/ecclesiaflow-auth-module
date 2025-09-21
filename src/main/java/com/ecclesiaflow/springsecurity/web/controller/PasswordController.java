package com.ecclesiaflow.springsecurity.web.controller;

import com.ecclesiaflow.springsecurity.business.domain.token.UserTokens;
import com.ecclesiaflow.springsecurity.io.entities.Member;
import com.ecclesiaflow.springsecurity.web.mappers.PasswordManagementMapper;
import com.ecclesiaflow.springsecurity.business.services.PasswordService;
import com.ecclesiaflow.springsecurity.business.services.AuthenticationService;
import com.ecclesiaflow.springsecurity.web.dto.*;
import com.ecclesiaflow.springsecurity.web.payloads.ChangePasswordRequest;
import com.ecclesiaflow.springsecurity.web.payloads.SetPasswordRequest;
import com.ecclesiaflow.springsecurity.web.security.Jwt;
import com.ecclesiaflow.springsecurity.web.exception.model.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/ecclesiaflow/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "EcclesiaFlow")
public class PasswordController {

    private final PasswordService passwordService;
    private final AuthenticationService authenticationService;
    private final Jwt jwt;
    
    @Value("${jwt.token.expiration}")
    private long accessTokenExpiration;

    @PostMapping(value = "/password", produces = "application/vnd.ecclesiaflow.auth.v1+json")
    @Operation(summary = "Définir le mot de passe initial",
            description = "Définit le mot de passe initial d'un membre après confirmation et génère automatiquement " +
                         "ses tokens d'authentification (access token et refresh token) pour une connexion immédiate. " +
                         "Le token temporaire doit être fourni dans le header Authorization au format 'Bearer {token}' " +
                         "pour des raisons de sécurité.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", 
                    description = "Mot de passe défini avec succès et tokens générés", 
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PasswordManagementResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400", 
                    description = "Token invalide ou données incorrectes", 
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401", 
                    description = "Header Authorization manquant ou format invalide", 
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class)
                    )
            )
    })
    public ResponseEntity<PasswordManagementResponse> setPassword(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody SetPasswordRequest passwordRequest) {

        String temporaryToken = extractTokenFromHeader(authorizationHeader);
        String validatedEmail = authenticationService.getEmailFromValidatedTempToken(temporaryToken);
        passwordService.setInitialPassword(validatedEmail, passwordRequest.getPassword());
        Member member = authenticationService.getMemberByEmail(validatedEmail);
        UserTokens userTokens = jwt.generateUserTokens(member);
        PasswordManagementResponse response = PasswordManagementMapper.toDtoWithTokens(
            "Mot de passe défini avec succès. Vous êtes maintenant connecté.",
                userTokens,
            accessTokenExpiration / 1000
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/new-password", produces = "application/vnd.ecclesiaflow.auth.v1+json")
    @Operation(summary = "Changer le mot de passe",
            description = "Change le mot de passe d'un membre authentifié")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", 
                    description = "Mot de passe changé avec succès"
            ),
            @ApiResponse(
                    responseCode = "400", 
                    description = "Mot de passe actuel incorrect", 
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorResponse.class)
                    )
            )
    })
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        passwordService.changePassword(
                request.getEmail(),
                request.getCurrentPassword(),
                request.getNewPassword()
        );
        return ResponseEntity.ok().build();
    }

    private String extractTokenFromHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Header Authorization manquant ou format invalide. Format attendu: Bearer {token}");
        }
        return authorizationHeader.substring(7);
    }
}
