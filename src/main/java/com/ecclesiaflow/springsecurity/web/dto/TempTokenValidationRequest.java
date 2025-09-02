package com.ecclesiaflow.springsecurity.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la validation d'un token temporaire.
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Requête de validation d'un token temporaire")
public class TempTokenValidationRequest {

    @NotBlank(message = "Le token est obligatoire")
    @Schema(description = "Token temporaire à valider", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String temporaryToken;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "L'email doit être valide")
    @Schema(description = "Email du membre", example = "membre@ecclesiaflow.com")
    private String email;
}
