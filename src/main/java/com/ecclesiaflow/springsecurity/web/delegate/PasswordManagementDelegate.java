package com.ecclesiaflow.springsecurity.web.delegate;

import com.ecclesiaflow.springsecurity.business.domain.member.Member;
import com.ecclesiaflow.springsecurity.business.domain.password.PasswordManagement;
import com.ecclesiaflow.springsecurity.business.domain.token.UserTokens;
import com.ecclesiaflow.springsecurity.business.services.AuthenticationService;
import com.ecclesiaflow.springsecurity.business.services.PasswordService;
import com.ecclesiaflow.springsecurity.web.mappers.OpenApiModelMapper;
import com.ecclesiaflow.springsecurity.web.model.ChangePasswordRequest;
import com.ecclesiaflow.springsecurity.web.model.PasswordManagementResponse;
import com.ecclesiaflow.springsecurity.web.model.SetPasswordRequest;
import com.ecclesiaflow.springsecurity.web.security.Jwt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
 * PasswordService + AuthenticationService + Jwt + OpenApiModelMapper
 * </pre>
 * 
 * <p><strong>Responsabilités :</strong></p>
 * <ul>
 *   <li>Définition du mot de passe initial après confirmation</li>
 *   <li>Changement de mot de passe pour membres authentifiés</li>
 *   <li>Extraction et validation du token temporaire depuis le header Authorization</li>
 *   <li>Génération automatique des tokens d'authentification après définition du mot de passe</li>
 *   <li>Transformation des modèles OpenAPI vers objets métier et vice-versa</li>
 * </ul>
 * 
 * @author EcclesiaFlow Team
 * @since 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordManagementDelegate {

    private final PasswordService passwordService;
    private final AuthenticationService authenticationService;
    private final Jwt jwt;
    private final OpenApiModelMapper openApiModelMapper;

    @Value("${jwt.token.expiration}")
    private long accessTokenExpiration;

    /**
     * Définit le mot de passe initial d'un membre après confirmation.
     * <p>
     * Processus :
     * 1. Extraction du token temporaire depuis le header Authorization
     * 2. Validation du token et extraction de l'email
     * 3. Définition du mot de passe initial
     * 4. Récupération du membre
     * 5. Génération automatique des tokens d'authentification
     * 6. Transformation vers le modèle OpenAPI
     * </p>
     * 
     * <p><strong>⚠️ IMPORTANT :</strong> Le token temporaire doit être fourni dans le header Authorization
     * au format 'Bearer {token}' pour des raisons de sécurité.</p>
     * 
     * @param authorizationHeader Header Authorization contenant le token temporaire
     * @param setPasswordRequest Requête contenant le nouveau mot de passe (modèle OpenAPI)
     * @return Réponse avec tokens d'authentification pour connexion immédiate
     */
    public ResponseEntity<PasswordManagementResponse> setPassword(
            String authorizationHeader, SetPasswordRequest setPasswordRequest) {
        
        // Extraction du token temporaire depuis le header Authorization
        String temporaryToken = extractTokenFromHeader(authorizationHeader);
        
        // Création de l'objet métier PasswordManagement
        PasswordManagement passwordManagement = new PasswordManagement(
            setPasswordRequest.getPassword(), 
            temporaryToken
        );
        
        // Validation du token temporaire et extraction de l'email
        String validatedEmail = authenticationService.getEmailFromValidatedTempToken(
            passwordManagement.temporaryToken()
        );
        
        // Extraction du memberId depuis le token temporaire
        java.util.UUID memberId = jwt.extractMemberId(passwordManagement.temporaryToken());
        
        // Définition du mot de passe initial avec le memberId pour lier les deux modules
        passwordService.setInitialPassword(validatedEmail, passwordManagement.password(), memberId);
        
        // Récupération du membre
        Member member = authenticationService.getMemberByEmail(validatedEmail);
        
        // Génération des tokens d'authentification
        UserTokens userTokens = jwt.generateUserTokens(member);
        
        // Transformation vers le modèle OpenAPI
        PasswordManagementResponse response = openApiModelMapper.createPasswordManagementResponse(
            "Mot de passe défini avec succès. Vous êtes maintenant connecté.",
            userTokens,
            accessTokenExpiration / 1000
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * Change le mot de passe d'un membre authentifié.
     * <p>
     * Processus :
     * 1. Extraction des informations depuis la requête OpenAPI
     * 2. Validation du mot de passe actuel
     * 3. Mise à jour du mot de passe
     * </p>
     * 
     * @param changePasswordRequest Requête contenant email, mot de passe actuel et nouveau (modèle OpenAPI)
     * @return Réponse vide avec statut 200
     */
    public ResponseEntity<PasswordManagementResponse> changePassword(ChangePasswordRequest changePasswordRequest) {
        
        // Changement du mot de passe via le service
        passwordService.changePassword(
            changePasswordRequest.getEmail(),
            changePasswordRequest.getCurrentPassword(),
            changePasswordRequest.getNewPassword()
        );
        return ResponseEntity.ok(new PasswordManagementResponse().message("Mot de passe changé avec succès"));
    }

    /**
     * Extrait le token JWT depuis le header Authorization.
     * <p>
     * Valide le format du header et extrait le token en supprimant le préfixe "Bearer ".
     * </p>
     * 
     * @param authorizationHeader Header Authorization au format "Bearer {token}"
     * @return Token JWT extrait
     * @throws RuntimeException si le header est manquant ou invalide
     */
    private String extractTokenFromHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new RuntimeException(
                "Header Authorization manquant ou format invalide. Format attendu: Bearer {token}"
            );
        }
        return authorizationHeader.substring(7);
    }
}
