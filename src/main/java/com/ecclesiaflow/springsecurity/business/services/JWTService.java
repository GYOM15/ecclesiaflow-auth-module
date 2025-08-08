package com.ecclesiaflow.springsecurity.business.services;

import com.ecclesiaflow.springsecurity.web.exception.InvalidTokenException;
import com.ecclesiaflow.springsecurity.web.exception.JwtProcessingException;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Service de domaine pour la gestion des tokens JWT dans EcclesiaFlow.
 * <p>
 * Cette interface définit les opérations de création, validation et extraction
 * d'informations des tokens JWT. Gère les tokens d'accès et de rafraîchissement
 * avec leurs cycles de vie respectifs.
 * </p>
 * 
 * <p><strong>Responsabilités principales :</strong></p>
 * <ul>
 *   <li>Génération de tokens d'accès et de rafraîchissement</li>
 *   <li>Validation de la signature et de l'expiration des tokens</li>
 *   <li>Extraction d'informations depuis les tokens</li>
 * </ul>
 * 
 * <p><strong>Cas d'utilisation typiques :</strong></p>
 * <ul>
 *   <li>Authentification initiale avec génération de tokens</li>
 *   <li>Validation des tokens lors des requêtes protégées</li>
 *   <li>Rafraîchissement des tokens expirés</li>
 * </ul>
 * 
 * <p><strong>Garanties :</strong> Thread-safe, opérations stateless.</p>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
public interface JWTService {
    
    /**
     * Génère un token d'accès JWT pour un utilisateur.
     * <p>
     * Cette méthode crée un token d'accès JWT basé sur les informations
     * fournies dans l'objet {@link UserDetails}. Le token est signé avec une
     * clé secrète pour garantir son authenticité.
     * </p>
     * 
     * @param userDetails les détails de l'utilisateur
     * @return le token d'accès JWT généré
     * @throws JwtProcessingException si la génération échoue
     */
    String generateAccessToken(UserDetails userDetails) throws JwtProcessingException;
    
    /**
     * Génère un refresh token JWT pour un utilisateur.
     * <p>
     * Cette méthode crée un refresh token JWT basé sur les informations
     * fournies dans l'objet {@link UserDetails}. Le token est signé avec une
     * clé secrète pour garantir son authenticité.
     * </p>
     * 
     * @param userDetails les détails de l'utilisateur
     * @return le refresh token JWT généré
     * @throws JwtProcessingException si la génération échoue
     */
    String generateRefreshToken(UserDetails userDetails) throws JwtProcessingException;
    
    /**
     * Extrait le nom d'utilisateur d'un token JWT.
     * <p>
     * Cette méthode extrait le nom d'utilisateur à partir d'un token JWT.
     * Elle vérifie également la signature et l'expiration du token pour
     * garantir son authenticité.
     * </p>
     * 
     * @param token le token JWT
     * @return le nom d'utilisateur
     * @throws JwtProcessingException si le token est invalide ou corrompu
     */
    String extractUsername(String token) throws JwtProcessingException;

    /**
     * Vérifie si un token d'accès est valide.
     * <p>
     * Cette méthode vérifie la signature et l'expiration d'un token d'accès
     * JWT pour garantir son authenticité.
     * </p>
     * 
     * @param token le token d'accès à valider
     * @return true si le token est valide, false sinon
     * @throws JwtProcessingException si la validation échoue
     */
    boolean isTokenValid(String token) throws JwtProcessingException;

    /**
     * Vérifie si un refresh token est valide.
     * <p>
     * Cette méthode vérifie la signature, l'expiration et le type ("refresh")
     * d'un refresh token JWT pour garantir son authenticité.
     * </p>
     * 
     * @param token le refresh token à valider
     * @return true si le refresh token est valide, false sinon
     * @throws JwtProcessingException si la validation échoue
     * @throws InvalidTokenException si le token n'est pas un refresh token
     */
    boolean isRefreshTokenValid(String token) throws JwtProcessingException, InvalidTokenException;
}
