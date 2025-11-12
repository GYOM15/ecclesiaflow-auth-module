package com.ecclesiaflow.springsecurity.web.controller;

import com.ecclesiaflow.springsecurity.web.api.PasswordManagementApi;
import com.ecclesiaflow.springsecurity.web.delegate.PasswordManagementDelegate;
import com.ecclesiaflow.springsecurity.web.model.ChangePasswordRequest;
import com.ecclesiaflow.springsecurity.web.model.ForgotPasswordRequest;
import com.ecclesiaflow.springsecurity.web.model.ForgotPasswordResponse;
import com.ecclesiaflow.springsecurity.web.model.PasswordManagementResponse;
import com.ecclesiaflow.springsecurity.web.model.SetPasswordRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrôleur REST pour la gestion des mots de passe - Pattern Delegate avec OpenAPI Generator.
 * <p>
 * Ce contrôleur implémente l'interface générée par OpenAPI Generator et utilise le pattern Delegate
 * pour séparer les responsabilités entre la gestion HTTP (contrôleur) et la logique métier (délégué).
 * Respecte le principe d'inversion de dépendance (DIP) de SOLID.
 * </p>
 * 
 * <p><strong>Architecture :</strong></p>
 * <pre>
 * OpenAPI Spec (openapi.yaml)
 *    ↓ génère
 * PasswordManagementApi
 *    ↓ implémentée par
 * PasswordController ← Cette classe
 *    ↓ délègue à
 * PasswordManagementDelegate
 *    ↓ utilise
 * PasswordService + AuthenticationService
 * </pre>
 * 
 * <p><strong>Responsabilités :</strong></p>
 * <ul>
 *   <li>Implémentation de l'interface PasswordManagementApi générée</li>
 *   <li>Délégation de la logique métier au délégué approprié</li>
 *   <li>Respect strict des contrats définis dans la spécification OpenAPI</li>
 * </ul>
 * 
 * @author EcclesiaFlow Team
 * @since 2.0.0 (Refactorisé avec pattern Delegate)
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class PasswordController implements PasswordManagementApi {

    private final PasswordManagementDelegate passwordManagementDelegate;

    /**
     * Définit le mot de passe initial d'un membre après confirmation.
     * <p>
     * Définit le mot de passe initial d'un membre après confirmation et génère automatiquement 
     * ses tokens d'authentification (access token et refresh token) pour une connexion immédiate.
     * </p>
     * 
     * <p><strong>⚠️ IMPORTANT :</strong> Le token temporaire doit être fourni dans le header Authorization 
     * au format 'Bearer {token}' pour des raisons de sécurité.</p>
     * 
     * @param authorization Header Authorization contenant le token temporaire
     * @param setPasswordRequest Requête contenant le nouveau mot de passe
     * @return Réponse avec tokens d'authentification pour connexion immédiate
     * 
     * @implNote <strong>Implémentation :</strong> Délègue au {@link PasswordManagementDelegate}
     * @see PasswordManagementDelegate#setPassword(String, SetPasswordRequest)
     */
    @Override
    public ResponseEntity<PasswordManagementResponse> _authSetInitialPassword(
            String authorization, SetPasswordRequest setPasswordRequest) {
        return passwordManagementDelegate.setPassword(authorization, setPasswordRequest);
    }

    /**
     * Change le mot de passe d'un membre authentifié.
     * <p>
     * Change le mot de passe d'un membre authentifié.
     * </p>
     * 
     * @param changePasswordRequest Requête contenant email, mot de passe actuel et nouveau
     * @return Réponse avec message de confirmation
     * 
     * @implNote <strong>Implémentation :</strong> Délègue au {@link PasswordManagementDelegate}
     * @see PasswordManagementDelegate#changePassword(ChangePasswordRequest)
     */
    @Override
    public ResponseEntity<PasswordManagementResponse> _authChangePassword(ChangePasswordRequest changePasswordRequest) {
        return passwordManagementDelegate.changePassword(changePasswordRequest);
    }

    /**
     * Initie une demande de réinitialisation de mot de passe.
     * <p>
     * Génère un token temporaire et envoie un email avec un lien de réinitialisation.
     * Retourne toujours le même message générique pour des raisons de sécurité.
     * </p>
     * 
     * @param forgotPasswordRequest Requête contenant l'email du compte
     * @return Message générique (ne révèle pas si le compte existe)
     * 
     * @implNote <strong>Implémentation :</strong> Délègue au {@link PasswordManagementDelegate}
     * @see PasswordManagementDelegate#requestPasswordReset(ForgotPasswordRequest)
     */
    @Override
    public ResponseEntity<ForgotPasswordResponse> _authRequestPasswordReset(ForgotPasswordRequest forgotPasswordRequest) {
        return passwordManagementDelegate.requestPasswordReset(forgotPasswordRequest);
    }

    /**
     * Réinitialise le mot de passe d'un membre avec un token de reset.
     * <p>
     * Réinitialise le mot de passe d'un membre existant qui a oublié son mot de passe.
     * Le token temporaire avec purpose="password_reset" doit être fourni dans le header Authorization.
     * </p>
     * 
     * @param authorization Header Authorization contenant le token temporaire
     * @param setPasswordRequest Requête contenant le nouveau mot de passe
     * @return Réponse avec tokens d'authentification pour connexion immédiate
     * 
     * @implNote <strong>Implémentation :</strong> Délègue au {@link PasswordManagementDelegate}
     * @see PasswordManagementDelegate#resetPassword(String, SetPasswordRequest)
     */
    @Override
    public ResponseEntity<PasswordManagementResponse> _authResetPassword(
            String authorization, SetPasswordRequest setPasswordRequest) {
        return passwordManagementDelegate.resetPassword(authorization, setPasswordRequest);
    }
}
