package com.ecclesiaflow.springsecurity.web.payloads;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour changer le mot de passe d'un membre.
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Requête pour changer le mot de passe")
public class ChangePasswordRequest {

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "L'email doit être valide")
    @Schema(description = "Email du membre", example = "membre@ecclesiaflow.com")
    private String email;

    @NotBlank(message = "Le mot de passe actuel est obligatoire")
    @Schema(description = "Mot de passe actuel", example = "AncienMotDePasse123")
    private String currentPassword;

    @NotBlank(message = "Le nouveau mot de passe est obligatoire")
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
        message = "Le mot de passe doit contenir au moins une minuscule, une majuscule et un chiffre"
    )
    @Schema(description = "Nouveau mot de passe", example = "NouveauMotDePasse123")
    private String newPassword;
}
