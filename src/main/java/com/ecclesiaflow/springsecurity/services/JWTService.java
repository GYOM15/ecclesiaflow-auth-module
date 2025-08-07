package com.ecclesiaflow.springsecurity.services;

import com.ecclesiaflow.springsecurity.exception.JwtProcessingException;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Map;

/**
 * Service de gestion des tokens JWT
 * Responsabilité : génération, validation et extraction des informations des tokens
 */
public interface JWTService {
    
    /**
     * Extrait le nom d'utilisateur d'un token JWT
     * @param jwt le token JWT
     * @return le nom d'utilisateur
     * @throws JwtProcessingException si le token est invalide ou corrompu
     */
    String extractUserName(String jwt) throws JwtProcessingException;
    
    /**
     * Génère un token JWT pour un utilisateur
     * @param userDetails les détails de l'utilisateur
     * @return le token JWT généré
     * @throws JwtProcessingException si la génération échoue
     */
    String generateToken(UserDetails userDetails) throws JwtProcessingException;
    
    /**
     * Vérifie si un token est valide pour un utilisateur donné
     * @param token le token à valider
     * @param userDetails les détails de l'utilisateur
     * @return true si le token est valide, false sinon
     * @throws JwtProcessingException si la validation échoue
     */
    boolean isTokenValid(String token, UserDetails userDetails) throws JwtProcessingException;
    
    /**
     * Génère un refresh token avec des claims supplémentaires
     * @param extraClaims les claims supplémentaires
     * @param userDetails les détails de l'utilisateur
     * @return le refresh token généré
     * @throws JwtProcessingException si la génération échoue
     */
    String generateRefreshToken(Map<String, Object> extraClaims, UserDetails userDetails) throws JwtProcessingException;
}
