package com.ecclesiaflow.springsecurity.web.delegate;

import com.ecclesiaflow.springsecurity.business.domain.member.Member;
import com.ecclesiaflow.springsecurity.business.domain.token.UserTokens;
import com.ecclesiaflow.springsecurity.business.exceptions.MemberNotFoundException;
import com.ecclesiaflow.springsecurity.business.services.AuthenticationService;
import com.ecclesiaflow.springsecurity.business.services.PasswordService;
import com.ecclesiaflow.springsecurity.web.constants.Messages;
import com.ecclesiaflow.springsecurity.web.exception.InvalidRequestException;
import com.ecclesiaflow.springsecurity.web.mappers.OpenApiModelMapper;
import com.ecclesiaflow.springsecurity.web.model.*;
import com.ecclesiaflow.springsecurity.web.security.Jwt;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.UUID;

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
public class PasswordManagementDelegate {

    private final PasswordService passwordService;
    private final AuthenticationService authenticationService;
    private final Jwt jwt;
    private final OpenApiModelMapper openApiModelMapper;
    private final HttpServletRequest httpServletRequest;

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
        
        String temporaryToken = extractTokenFromHeader(authorizationHeader);
        ValidatedTokenData tokenData = validateAndExtractTemporaryTokenData(temporaryToken, "password_setup");
        

        Member updatedMember = passwordService.setInitialPassword(
            tokenData.member().getEmail(), 
            setPasswordRequest.getPassword(), 
            tokenData.memberId()
        );
        
        return generateAuthenticationResponse(
            updatedMember,
            Messages.PASSWORD_SETUP_SUCCESS
        );
    }

    /**
     * Change le mot de passe d'un membre authentifié.
     * <p>
     * Processus :
     * 1. Extraction du token d'accès depuis le header Authorization
     * 2. Validation du token et extraction de l'email
     * 3. Vérification que le token n'a pas été émis avant la dernière modification du mot de passe
     * 4. Changement du mot de passe
     * 5. Génération de nouveaux tokens d'authentification
     * </p>
     * 
     * @param changePasswordRequest Requête contenant mot de passe actuel et nouveau
     * @return Réponse avec nouveaux tokens d'authentification
     */
    public ResponseEntity<PasswordManagementResponse> changePassword(ChangePasswordRequest changePasswordRequest) {
        String authorizationHeader = httpServletRequest.getHeader("Authorization");
        String accessToken = extractTokenFromHeader(authorizationHeader);
        
        String email;
        try {
            email = jwt.extractEmail(accessToken);
        } catch (Exception e) {
            throw new InvalidRequestException(Messages.AUTH_REQUIRED);
        }
        
        Member member = authenticationService.getMemberByEmail(email);
        
        if (!jwt.isTokenValidForPasswordUpdate(accessToken, member)) {
            throw new InvalidRequestException(Messages.SESSION_EXPIRED);
        }
        
        Member updatedMember = passwordService.changePassword(
            email,
            changePasswordRequest.getCurrentPassword(),
            changePasswordRequest.getNewPassword()
        );
        
        return generateAuthenticationResponse(
            updatedMember,
            Messages.PASSWORD_CHANGE_SUCCESS
        );
    }

    /**
     * Initie une demande de réinitialisation de mot de passe.
     * <p>
     * Processus :
     * 1. Validation de l'email
     * 2. Appel du service métier qui :
     *    - Recherche le membre
     *    - Publie un événement métier si trouvé
     * 3. Retour d'un message générique (sécurité)
     * </p>
     * <p>
     * <strong>Sécurité :</strong> Le message de réponse est toujours identique,
     * que le compte existe ou non, pour ne pas révéler l'existence d'un email.
     * </p>
     * <p>
     * <strong>Architecture :</strong> Le service métier publie un événement,
     * le handler applicatif génère le JWT et le lien, et l'infrastructure envoie l'email.
     * </p>
     * 
     * @param forgotPasswordRequest Requête contenant l'email (modèle OpenAPI)
     * @return Message générique de confirmation
     */
    public ResponseEntity<ForgotPasswordResponse> requestPasswordReset(
            ForgotPasswordRequest forgotPasswordRequest) {
        
        passwordService.requestPasswordReset(forgotPasswordRequest.getEmail());
        
        ForgotPasswordResponse response =
            new ForgotPasswordResponse()
                .message(Messages.RESET_EMAIL_SENT);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Réinitialise le mot de passe d'un membre avec un token de reset.
     * <p>
     * Processus :
     * 1. Extraction du token temporaire depuis le header Authorization
     * 2. Validation du token et extraction de l'email
     * 3. Validation stricte : token doit avoir purpose="password_reset"
     * 4. Validation stricte : compte doit être activé (enabled=true)
     * 5. Réinitialisation du mot de passe
     * 6. Envoi email de confirmation
     * 7. Génération et retour des tokens d'authentification
     * </p>
     * 
     * @param authorizationHeader Header Authorization contenant le token temporaire
     * @param setPasswordRequest Requête contenant le nouveau mot de passe
     * @return Réponse avec tokens d'authentification pour connexion immédiate
     */
    public ResponseEntity<PasswordManagementResponse> resetPassword(
            String authorizationHeader, SetPasswordRequest setPasswordRequest) {
        
        String temporaryToken = extractTokenFromHeader(authorizationHeader);
        ValidatedTokenData tokenData = validateAndExtractTemporaryTokenData(temporaryToken, "password_reset");
        
        if (!tokenData.member().isEnabled()) {
            throw new InvalidRequestException(Messages.INVALID_OR_EXPIRED_LINK);
        }
        
        Member updatedMember = passwordService.resetPasswordWithToken(
            tokenData.member().getEmail(), 
            setPasswordRequest.getPassword()
        );
        
        return generateAuthenticationResponse(
            updatedMember,
            Messages.PASSWORD_RESET_SUCCESS
        );
    }

    // ==================== MÉTHODES HELPER ====================
    
    /**
     * Extrait le token JWT depuis le header Authorization.
     */
    private String extractTokenFromHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new InvalidRequestException(Messages.INVALID_AUTH_HEADER);
        }
        return authorizationHeader.substring(7);
    }
    
    /**
     * Génère une réponse d'authentification avec tokens.
     */
    private ResponseEntity<PasswordManagementResponse> generateAuthenticationResponse(
            Member member, String message) {
        UserTokens userTokens = jwt.generateUserTokens(member);
        PasswordManagementResponse response = openApiModelMapper.createPasswordManagementResponse(
            message,
            userTokens,
            accessTokenExpiration / 1000
        );
        return ResponseEntity.ok(response);
    }
    
    /**
     * Valide un token temporaire et retourne le membre validé avec le purpose vérifié.
     * <p>
     * Délègue à la méthode spécialisée selon le purpose (password_setup ou password_reset).
     * </p>
     */
    private ValidatedTokenData validateAndExtractTemporaryTokenData(String temporaryToken, String expectedPurpose) {
        try {
            String email = jwt.extractEmailFromTemporaryToken(temporaryToken);
            
            if (!jwt.validateTemporaryToken(temporaryToken, email)) {
                throw new InvalidRequestException(Messages.PASSWORD_SETUP_ERROR);
            }
            
            String purpose = jwt.extractPurpose(temporaryToken);
            if (!expectedPurpose.equals(purpose)) {
                throw new InvalidRequestException(Messages.PASSWORD_SETUP_ERROR);
            }
            
            return "password_setup".equals(expectedPurpose)
                ? validatePasswordSetupToken(temporaryToken, email)
                : validatePasswordResetToken(temporaryToken, email);
                
        } catch (InvalidRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidRequestException(Messages.PASSWORD_SETUP_ERROR);
        }
    }
    
    /**
     * Valide un token pour password_setup (membre n'existe pas encore).
     * <p>
     * Vérifie que le membre n'existe pas déjà avec un mot de passe défini.
     * Crée un membre temporaire (non persisté) pour la validation.
     * Le membre sera créé dans la DB lors de l'appel à setInitialPassword().
     * </p>
     */
    private ValidatedTokenData validatePasswordSetupToken(String temporaryToken, String email) {
        UUID memberId = jwt.extractMemberId(temporaryToken);
        
        // Vérifier si le membre existe déjà et a déjà un mot de passe défini
        // Note: getMemberByEmail lance MemberNotFoundException si le membre n'existe pas (normal pour password_setup)
        try {
            Member existingMember = authenticationService.getMemberByEmail(email);
            if (existingMember.isEnabled()) {
                throw new InvalidRequestException(Messages.PASSWORD_SETUP_ERROR);
            }
        } catch (MemberNotFoundException ignored) {

        }
        
        Member temporaryMember = Member.builder()
            .email(email)
            .memberId(memberId)
            .enabled(false)
            .build();
        
        return new ValidatedTokenData(temporaryMember, memberId, "password_setup");
    }
    
    /**
     * Valide un token pour password_reset (membre doit exister).
     * <p>
     * Vérifie que le membre existe dans la DB et que le token n'a pas été invalidé
     * par un changement de mot de passe ultérieur.
     * </p>
     */
    private ValidatedTokenData validatePasswordResetToken(String temporaryToken, String email) {
        Member member = authenticationService.getMemberByEmail(email);

        if (member == null) {
            throw new InvalidRequestException(Messages.PASSWORD_SETUP_ERROR);
        }
        
        if (!jwt.isTokenValidForPasswordUpdate(temporaryToken, member)) {
            throw new InvalidRequestException(Messages.PASSWORD_SETUP_ERROR);
        }
        
        return new ValidatedTokenData(member, null, "password_reset");
    }

    /**
     * Enregistrement des données validées du token temporaire avec le membre.
     */
    private record ValidatedTokenData(Member member, UUID memberId, String purpose) {}
}
