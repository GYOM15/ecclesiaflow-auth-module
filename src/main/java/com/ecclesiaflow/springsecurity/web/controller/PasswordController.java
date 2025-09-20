package com.ecclesiaflow.springsecurity.web.controller;

import com.ecclesiaflow.springsecurity.business.domain.password.PasswordManagement;
import com.ecclesiaflow.springsecurity.web.mappers.PasswordManagementMapper;
import com.ecclesiaflow.springsecurity.web.mappers.TemporaryTokenMapper;
import com.ecclesiaflow.springsecurity.business.services.PasswordService;
import com.ecclesiaflow.springsecurity.web.dto.*;
import com.ecclesiaflow.springsecurity.web.payloads.ChangePasswordRequest;
import com.ecclesiaflow.springsecurity.web.payloads.SetPasswordRequest;
import com.ecclesiaflow.springsecurity.web.payloads.TempTokenValidationRequest;
import com.ecclesiaflow.springsecurity.web.security.Jwt;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/ecclesiaflow/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "EcclesiaFlow")
public class PasswordController {

    private final PasswordService passwordService;
    private final Jwt jwt;
    private final TemporaryTokenMapper temporaryTokenMapper;
    private final PasswordManagementMapper passwordManagementMapper;

    /**
     * Définit le mot de passe initial d'un membre.
     */
    @PostMapping(value = "/password", produces = "application/vnd.ecclesiaflow.auth.v1+json")
    @Operation(summary = "Définir le mot de passe initial",
            description = "Définit le mot de passe initial d'un membre après confirmation")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mot de passe défini avec succès", content = @Content),
            @ApiResponse(responseCode = "400", description = "Token invalide ou données incorrectes", content = @Content)
    })
    public ResponseEntity<PasswordManagementResponse> setPassword(
            @Valid @RequestBody SetPasswordRequest passwordRequest) {
        PasswordManagement passwordManagement = passwordManagementMapper.fromSetPasswordRequest(passwordRequest);
        passwordService.setInitialPassword(
                passwordManagement.getEmail(),
                passwordManagement.getPassword(),
                passwordManagement.getTemporaryToken()
        );
        PasswordManagementResponse passwordManagementResponse = PasswordManagementMapper.toDto("Mot de passe défini avec succès");
        return ResponseEntity.ok(passwordManagementResponse);
    }

    /**
     * Change le mot de passe d'un membre.
     */
    @PostMapping(value = "/new-password", produces = "application/vnd.ecclesiaflow.auth.v1+json")
    @Operation(summary = "Changer le mot de passe",
            description = "Change le mot de passe d'un membre authentifié")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mot de passe changé avec succès", content = @Content),
            @ApiResponse(responseCode = "400", description = "Mot de passe actuel incorrect", content = @Content)
    })
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        passwordService.changePassword(
                request.getEmail(),
                request.getCurrentPassword(),
                request.getNewPassword()
        );
        return ResponseEntity.ok().build();
    }

    /**
     * Valide un token temporaire.
     */
    @PostMapping(value = "/temporary-valid-token", produces = "application/vnd.ecclesiaflow.auth.v1+json")
    @Operation(summary = "Validation de token temporaire",
            description = "Valide un token temporaire pour un email donné")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token valide", content = @Content),
            @ApiResponse(responseCode = "400", description = "Token invalide ou expiré", content = @Content)
    })
    public ResponseEntity<TemporaryTokenResponse> validateTemporaryToken(
            @Valid @RequestBody TempTokenValidationRequest temporaryTokenRequest) {

        boolean isValidToken = jwt.validateTemporaryToken(
                temporaryTokenRequest.getTemporaryToken(),
                temporaryTokenRequest.getEmail()
        );

        TemporaryTokenResponse response = temporaryTokenMapper.toResponse(
                temporaryTokenRequest.getTemporaryToken()
        );

        return isValidToken ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }
}
