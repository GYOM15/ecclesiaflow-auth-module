package com.ecclesiaflow.springsecurity.web.controller;

import com.ecclesiaflow.springsecurity.business.domain.MemberRegistration;
import com.ecclesiaflow.springsecurity.web.dto.MemberResponse;
import com.ecclesiaflow.springsecurity.web.dto.SignUpRequest;
import com.ecclesiaflow.springsecurity.io.entities.Member;
import com.ecclesiaflow.springsecurity.business.services.AuthenticationService;
import com.ecclesiaflow.springsecurity.business.mappers.MemberMapper;
import com.ecclesiaflow.springsecurity.business.mappers.MemberResponseMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * CONTRÔLEUR TEMPORAIRE - À MIGRER VERS LE MODULE DE GESTION DES MEMBRES
 * 
 * Ce contrôleur sera déplacé vers ecclesiaflow-member-management-module
 * dans la future architecture multi-tenant où :
 * - Les pasteurs (admins tenant) créent les membres
 * - Les demandes d'inscription sont soumises pour approbation
 * - La gestion des membres est séparée de l'authentification
 */
@Slf4j
@RestController
@RequestMapping("/ecclesiaflow/members")
@RequiredArgsConstructor
@Tag(name = "Members (Temporary)", description = "API temporaire - sera migrée vers le module de gestion des membres")
@Deprecated // Marquer comme deprecated pour indiquer la migration future
public class MembersController {
    private final AuthenticationService authenticationService;

    @GetMapping(value = "/hello", produces = "application/vnd.ecclesiaflow.members.v2+json")
    @Operation(
        summary = "Message de bienvenue pour les membres",
        description = "Endpoint de test pour les membres authentifiés"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Message de bienvenue",
            content = @Content(
                mediaType = "text/plain",
                schema = @Schema(type = "string", example = "Hi Member")
            )
        )
    })
    public ResponseEntity<String> sayHello() {
        return ResponseEntity.ok("Hi Member");
    }

    @PostMapping(value = "/signup", produces = "application/vnd.ecclesiaflow.members.v2+json")
    @Operation(
        summary = "[TEMPORAIRE] Auto-enregistrement d'un membre",
        description = "SERA REMPLACÉ par un système d'approbation admin dans le module de gestion des membres"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Membre créé avec succès (temporaire - sera un système d'approbation)",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MemberResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Données d'enregistrement invalides ou email déjà utilisé",
            content = @Content
        )
    })
    @Deprecated // Sera remplacé par un système d'approbation
    public ResponseEntity<MemberResponse> registerMember(@Valid @RequestBody SignUpRequest signUpRequest) {
        MemberRegistration registration = MemberMapper.fromSignUpRequest(signUpRequest);
        Member member = authenticationService.registerMember(registration);
        MemberResponse response = MemberResponseMapper.fromMember(member, "Member registered (temporary - approval system coming)");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
