package com.ecclesiaflow.springsecurity.web.security;

import com.ecclesiaflow.springsecurity.business.domain.member.Member;
import com.ecclesiaflow.springsecurity.business.domain.token.UserTokens;
import com.ecclesiaflow.springsecurity.business.services.mappers.ScopeMapper;
import com.ecclesiaflow.springsecurity.business.services.adapters.MemberUserDetailsAdapter;
import com.ecclesiaflow.springsecurity.web.exception.InvalidTokenException;
import com.ecclesiaflow.springsecurity.web.exception.JwtProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Utilitaire JWT pour la couche web d'EcclesiaFlow.
 * <p>
 * Cette classe gère les opérations techniques liées aux tokens JWT :
 * génération de paires de tokens et rafraîchissement. Appartient à la couche web
 * car elle traite des aspects techniques de sécurité HTTP plutôt que de logique métier.
 * </p>
 * 
 * <p><strong>Rôle architectural :</strong> Utilitaire web - Opérations techniques JWT</p>
 * 
 * <p><strong>Responsabilités principales :</strong></p>
 * <ul>
 *   <li>Génération de paires de tokens (accès + rafraîchissement)</li>
 *   <li>Rafraîchissement des tokens d'accès expirés</li>
 *   <li>Orchestration des opérations techniques de tokens</li>
 * </ul>
 * 
 * <p><strong>Cas d'utilisation typiques :</strong></p>
 * <ul>
 *   <li>Génération de tokens après authentification réussie</li>
 *   <li>Renouvellement automatique des tokens expirés</li>
 *   <li>Support des contrôleurs d'authentification</li>
 * </ul>
 * 
 * <p><strong>Garanties :</strong> Thread-safe, opérations stateless.</p>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class Jwt {

    private final JwtProcessor jwtProcessor;
    private final ScopeMapper scopeMapper;

    /**
     * Génère une paire de tokens JWT (accès + rafraîchissement) pour un membre.
     * <p>
     * Cette méthode crée les tokens d'accès et de rafraîchissement pour un membre
     * authentifié. Opération technique qui ne fait pas partie de la logique métier
     * d'authentification.
     * </p>
     * 
     * @param member le membre pour lequel générer les tokens, non null
     * @return un {@link UserTokens} contenant le membre et ses tokens
     * @throws JwtProcessingException si la génération des tokens échoue
     * 
     * @implNote Opération en mémoire uniquement, aucun accès à la base de données.
     */
    public UserTokens generateUserTokens(Member member) throws JwtProcessingException {
        MemberUserDetailsAdapter userDetails = new MemberUserDetailsAdapter(member);
        Set<String> scopes = scopeMapper.mapRoleToScopes(member.getRole());
        String accessToken = jwtProcessor.generateAccessToken(userDetails, member.getMemberId(), scopes);
        String refreshToken = jwtProcessor.generateRefreshToken(userDetails);
        return new UserTokens(accessToken, refreshToken);
    }

    /**
     * <p>
     * Opération purement technique qui valide la structure et l'expiration
     * du token, puis extrait l'email sans faire appel à la couche métier.
     * </p>
     *
     * @param refreshToken le refresh token à valider
     * @return l'email extrait du token
     * @throws InvalidTokenException si le token est invalide ou expiré
     * @throws JwtProcessingException si une erreur se produit lors du traitement
     */
    public String validateAndExtractEmail(String refreshToken)
            throws InvalidTokenException, JwtProcessingException {

        if (!jwtProcessor.isRefreshTokenValid(refreshToken)) {
            throw new InvalidTokenException("Le token de rafraîchissement est invalide ou expiré.");
        }

        return jwtProcessor.extractUsername(refreshToken);
    }

    /**
     * Génère un nouveau token d'accès pour un membre donné.
     * <p>
     * Opération technique pure qui ne fait que générer un nouveau token d'accès
     * en gardant le même refresh token. Le membre est fourni par la couche métier.
     * </p>
     *
     * @param refreshToken l'ancien refresh token à conserver
     * @param member le membre pour lequel générer le nouveau token d'accès
     * @return un {@link UserTokens} avec le nouveau token d'accès et l'ancien refresh token
     * @throws JwtProcessingException si la génération du token échoue
     */
    public UserTokens refreshTokenForMember(String refreshToken, Member member)
            throws JwtProcessingException {
        MemberUserDetailsAdapter userDetails = new MemberUserDetailsAdapter(member);
        Set<String> scopes = scopeMapper.mapRoleToScopes(member.getRole());
        String newAccessToken = jwtProcessor.generateAccessToken(userDetails, member.getMemberId(), scopes);
        return new UserTokens(newAccessToken, refreshToken);
    }

    /**
     * Génère un token temporaire JWT sécurisé pour la définition de mot de passe.
     * <p>
     * Cette méthode utilise le JwtProcessor pour créer un token temporaire sécurisé
     * destiné à permettre à un membre de définir son mot de passe après confirmation d'email.
     * </p>
     *
     * @param email l'email du membre pour lequel générer le token temporaire
     * @param memberId l'UUID du membre
     * @param purpose le but du token ("password_setup" ou "password_reset")
     * @return un token JWT temporaire sécurisé
     * @throws JwtProcessingException si la génération du token échoue
     */
    public String generateTemporaryToken(String email, UUID memberId, String purpose) throws JwtProcessingException {
        return jwtProcessor.generateTemporaryToken(email, memberId, purpose);
    }

    /**
     * Valide un token temporaire JWT pour un email donné.
     * <p>
     * Cette méthode utilise le JwtProcessor pour valider un token temporaire
     * et vérifier qu'il correspond à l'email fourni.
     * </p>
     *
     * @param token le token temporaire à valider
     * @param email l'email du membre pour lequel valider le token
     * @return true si le token est valide et correspond à l'email, false sinon
     */
    public boolean validateTemporaryToken(String token, String email) {
        return jwtProcessor.validateTemporaryToken(token, email);
    }

    /**
     * Extrait l'email d'un token temporaire JWT.
     * <p>
     * Cette méthode utilise le JwtProcessor pour extraire l'email (username)
     * d'un token temporaire.
     * </p>
     *
     * @param temporaryToken le token temporaire dont extraire l'email
     * @return l'email extrait du token
     * @throws JwtProcessingException si l'extraction échoue ou si le token est malformé
     */
    public String extractEmailFromTemporaryToken(String temporaryToken) throws JwtProcessingException {
        return jwtProcessor.extractUsername(temporaryToken);
    }

    /**
     * Extrait le memberId (claim 'cid') d'un token JWT.
     * <p>
     * Cette méthode utilise le JwtProcessor pour extraire le memberId (UUID)
     * depuis le claim 'cid' du token. Utilisé pour lier les modules auth et members.
     * </p>
     *
     * @param token le token JWT dont extraire le memberId
     * @return le memberId (UUID) extrait du token
     * @throws JwtProcessingException si l'extraction échoue ou si le token est malformé
     * @throws InvalidTokenException si le claim 'cid' est absent
     */
    public UUID extractMemberId(String token) throws JwtProcessingException, InvalidTokenException {
        return jwtProcessor.extractMemberId(token);
    }

    /**
     * Extrait le purpose d'un token JWT temporaire.
     * <p>
     * Cette méthode utilise le JwtProcessor pour extraire le purpose du token.
     * Utilisé pour distinguer entre "password_setup" et "password_reset".
     * </p>
     *
     * @param token le token JWT dont extraire le purpose
     * @return le purpose extrait du token ("password_setup" ou "password_reset")
     * @throws JwtProcessingException si l'extraction échoue ou si le token est malformé
     * @throws InvalidTokenException si le claim 'purpose' est absent
     */
    public String extractPurpose(String token) throws JwtProcessingException, InvalidTokenException {
        return jwtProcessor.extractPurpose(token);
    }

    /**
     * Extrait la date d'émission (issuedAt) d'un token JWT.
     * <p>
     * Cette méthode utilise le JwtProcessor pour extraire la date d'émission du token.
     * Utilisé pour valider que le token a été émis après la dernière modification du mot de passe.
     * </p>
     *
     * @param token le token JWT dont extraire la date d'émission
     * @return la date d'émission du token
     * @throws JwtProcessingException si l'extraction échoue ou si le token est malformé
     * @throws InvalidTokenException si la date d'émission est absente
     */
    public LocalDateTime extractIssuedAt(String token) throws JwtProcessingException, InvalidTokenException {
        return jwtProcessor.extractIssuedAt(token);
    }

    public String extractEmail(String token) throws JwtProcessingException, InvalidTokenException {
        return jwtProcessor.extractUsername(token);
    }

    /**
     * Valide qu'un token n'a pas été émis avant la dernière modification du mot de passe.
     * <p>
     * Utilisé pour invalider automatiquement les tokens générés avant un changement de mot de passe,
     * forçant ainsi l'utilisateur à se reconnecter avec ses nouvelles credentials.
     * </p>
     *
     * @param token le token à valider (access token ou temporary token)
     * @param member le membre dont on vérifie le passwordUpdatedAt
     * @return true si le token est valide (émis après ou en même temps que le dernier changement de MDP), false sinon
     * @throws JwtProcessingException si l'extraction du iat échoue
     */
    public boolean isTokenValidForPasswordUpdate(String token, Member member) {
        if (member.getPasswordUpdatedAt() == null) {
            return true;
        }
        LocalDateTime tokenIssuedAt = extractIssuedAt(token);
        return !tokenIssuedAt.isBefore(member.getPasswordUpdatedAt());
    }
}
