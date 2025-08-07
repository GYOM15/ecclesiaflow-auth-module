package com.ecclesiaflow.springsecurity.services;

import com.ecclesiaflow.springsecurity.exception.InvalidTokenException;
import com.ecclesiaflow.springsecurity.exception.JwtProcessingException;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Service de gestion des tokens JWT
 * Responsabilité : génération, validation et extraction des informations des tokens
 */
public interface JWTService {
    
    /**
     * Génère un token d'accès JWT pour un utilisateur
     * @param userDetails les détails de l'utilisateur
     * @return le token d'accès JWT généré
     * @throws JwtProcessingException si la génération échoue
     */
    String generateAccessToken(UserDetails userDetails) throws JwtProcessingException;
    
    /**
     * Génère un refresh token JWT pour un utilisateur
     * @param userDetails les détails de l'utilisateur
     * @return le refresh token JWT généré
     * @throws JwtProcessingException si la génération échoue
     */
    String generateRefreshToken(UserDetails userDetails) throws JwtProcessingException;
    
    /**
     * Extrait le nom d'utilisateur d'un token JWT
     * @param token le token JWT
     * @return le nom d'utilisateur
     * @throws JwtProcessingException si le token est invalide ou corrompu
     */
    String extractUsername(String token) throws JwtProcessingException;

    /**
     * Vérifie si un token d'accès est valide
     * Vérifie la signature et l'expiration du token
     * @param token le token d'accès à valider
     * @return true si le token est valide, false sinon
     * @throws JwtProcessingException si la validation échoue
     */
    boolean isTokenValid(String token) throws JwtProcessingException;

    /**
     * Vérifie si un refresh token est valide
     * Vérifie la signature, l'expiration ET le claim "type": "refresh"
     * @param token le refresh token à valider
     * @return true si le refresh token est valide, false sinon
     * @throws JwtProcessingException si la validation échoue
     */
    boolean isRefreshTokenValid(String token) throws JwtProcessingException, InvalidTokenException;
}
