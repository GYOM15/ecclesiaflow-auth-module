package com.ecclesiaflow.springsecurity.web.security;

import com.ecclesiaflow.springsecurity.business.domain.AuthenticationResult;
import com.ecclesiaflow.springsecurity.business.domain.TokenRefreshData;
import com.ecclesiaflow.springsecurity.io.entities.Member;
import com.ecclesiaflow.springsecurity.io.repository.MemberRepository;
import com.ecclesiaflow.springsecurity.web.exception.InvalidTokenException;
import com.ecclesiaflow.springsecurity.web.exception.JwtProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
public class JwtTokenUtil {

    private final JwtProcessor jwtProcessor;
    private final MemberRepository memberRepository;

    /**
     * Génère une paire de tokens JWT (accès + rafraîchissement) pour un membre.
     * <p>
     * Cette méthode crée les tokens d'accès et de rafraîchissement pour un membre
     * authentifié. Opération technique qui ne fait pas partie de la logique métier
     * d'authentification.
     * </p>
     * 
     * @param member le membre pour lequel générer les tokens, non null
     * @return un {@link AuthenticationResult} contenant le membre et ses tokens
     * @throws JwtProcessingException si la génération des tokens échoue
     * @throws IllegalArgumentException si member est null
     * 
     * @implNote Opération en mémoire uniquement, aucun accès à la base de données.
     */
    public AuthenticationResult generateUserTokens(Member member) throws JwtProcessingException {
        String accessToken = jwtProcessor.generateAccessToken(member);
        String refreshToken = jwtProcessor.generateRefreshToken(member);
        return new AuthenticationResult(member, accessToken, refreshToken);
    }

    /**
     * Rafraîchit un token d'accès à partir d'un refresh token valide.
     * <p>
     * Valide le refresh token, extrait l'utilisateur associé et génère un nouveau
     * token d'accès. Le refresh token reste inchangé pour permettre de futurs
     * rafraîchissements jusqu'à son expiration.
     * </p>
     * 
     * @param refreshData objet contenant le refresh token, non null
     * @return un {@link AuthenticationResult} avec le nouveau token d'accès et l'ancien refresh token
     * @throws InvalidTokenException si le refresh token est invalide, expiré ou de mauvais type
     * @throws JwtProcessingException si la génération du nouveau token d'accès échoue
     * @throws IllegalArgumentException si refreshData est null
     * 
     * @implNote Opération en lecture seule sur la base de données, génère un nouveau token en mémoire.
     */
    @Transactional(readOnly = true)
    public AuthenticationResult refreshToken(TokenRefreshData refreshData) 
            throws InvalidTokenException, JwtProcessingException {

        String refreshToken = refreshData.getRefreshToken();
        if (!jwtProcessor.isRefreshTokenValid(refreshToken)) {
            throw new InvalidTokenException("Le token de rafraîchissement est invalide ou n'est pas du bon type.");
        }

        String username = jwtProcessor.extractUsername(refreshToken);

        // On récupère l'utilisateur sans utiliser d'identifiants externes injectables
        Member member = memberRepository.findByEmail(username)
                .orElseThrow(() -> new InvalidTokenException("Aucun membre correspondant au token."));

        // Génération du nouveau token d'accès
        String newAccessToken = jwtProcessor.generateAccessToken(member);
        
        return new AuthenticationResult(member, newAccessToken, refreshToken);
    }
}
