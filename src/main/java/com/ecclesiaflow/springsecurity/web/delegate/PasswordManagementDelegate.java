package com.ecclesiaflow.springsecurity.web.delegate;

import com.ecclesiaflow.springsecurity.business.services.PasswordService;
import com.ecclesiaflow.springsecurity.web.constants.Messages;
import com.ecclesiaflow.springsecurity.web.exception.InvalidRequestException;
import com.ecclesiaflow.springsecurity.web.model.PasswordManagementResponse;
import com.ecclesiaflow.springsecurity.web.model.SetupPasswordRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * Délégué pour la gestion des mots de passe - Pattern Delegate avec OpenAPI Generator.
 * <p>
 * Ce délégué contient toute la logique métier pour la gestion des mots de passe,
 * séparant ainsi les responsabilités entre le contrôleur (gestion HTTP) et la logique applicative.
 * </p>
 * 
 * <p><strong>Architecture :</strong></p>
 * <pre>
 * PasswordController (implémente PasswordManagementApi)
 *    ↓ délègue à
 * PasswordManagementDelegate ← Cette classe
 *    ↓ utilise
 * SetupTokenService + KeycloakAdminClient + MembersGrpcClient
 * </pre>
 * 
 * <p><strong>Responsabilités :</strong></p>
 * <ul>
 *   <li>Définition du mot de passe initial après confirmation email</li>
 *   <li>Extraction et validation du token temporaire depuis le header X-Setup-Token</li>
 *   <li>Création de l'utilisateur dans Keycloak avec le mot de passe</li>
 *   <li>Notification du module Members de l'activation du compte</li>
 * </ul>
 * 
 * @author EcclesiaFlow Team
 * @since 2.0.0
 */
@Service
@RequiredArgsConstructor
public class PasswordManagementDelegate {

    private final PasswordService passwordService;

    public ResponseEntity<PasswordManagementResponse> setupPassword(
            String xSetupToken, String password) {

        if (xSetupToken == null || xSetupToken.isBlank()) {
            throw new InvalidRequestException("Setup token is required");
        }

        passwordService.setupPassword(xSetupToken, password);

        PasswordManagementResponse response = new PasswordManagementResponse()
                .message(Messages.PASSWORD_SETUP_SUCCESS);

        return ResponseEntity.ok(response);
    }
}
