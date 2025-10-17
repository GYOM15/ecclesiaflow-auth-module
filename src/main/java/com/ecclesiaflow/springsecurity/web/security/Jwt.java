package com.ecclesiaflow.springsecurity.web.security;

import com.ecclesiaflow.springsecurity.business.domain.member.Member;
import com.ecclesiaflow.springsecurity.business.domain.token.UserTokens;
import com.ecclesiaflow.springsecurity.business.services.mappers.ScopeMapper;
import com.ecclesiaflow.springsecurity.business.services.adapters.MemberUserDetailsAdapter;
import com.ecclesiaflow.springsecurity.web.exception.InvalidTokenException;
import com.ecclesiaflow.springsecurity.web.exception.JwtProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
     * Opération technique pure qui respecte l'architecture en couches.
     * </p>
     *
     * @param email l'email du membre pour lequel générer le token temporaire
     * @return un token JWT temporaire sécurisé
     * @throws JwtProcessingException si la génération du token échoue
     */
    public String generateTemporaryToken(String email, UUID memberId) throws JwtProcessingException {
        return jwtProcessor.generateTemporaryToken(email, memberId);
    }

    /**
     * Valide un token temporaire JWT pour un email donné.
     * <p>
     * Cette méthode utilise le JwtProcessor pour valider un token temporaire
     * et vérifier qu'il correspond à l'email fourni. Opération technique pure
     * qui respecte l'architecture en couches.
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
     * d'un token temporaire. Opération technique pure qui respecte l'architecture en couches.
     * </p>
     *
     * @param temporaryToken le token temporaire dont extraire l'email
     * @return l'email extrait du token
     * @throws JwtProcessingException si l'extraction échoue ou si le token est malformé
     */
    public String extractEmailFromTemporaryToken(String temporaryToken) throws JwtProcessingException {
        return jwtProcessor.extractUsername(temporaryToken);
    }

}
