package com.ecclesiaflow.springsecurity.business.services;

import com.ecclesiaflow.springsecurity.business.domain.AuthenticationResult;
import com.ecclesiaflow.springsecurity.business.domain.MemberRegistration;
import com.ecclesiaflow.springsecurity.business.domain.SigninCredentials;
import com.ecclesiaflow.springsecurity.web.dto.RefreshTokenRequest;
import com.ecclesiaflow.springsecurity.io.entities.Member;
import com.ecclesiaflow.springsecurity.web.exception.InvalidCredentialsException;
import com.ecclesiaflow.springsecurity.web.exception.InvalidRequestException;
import com.ecclesiaflow.springsecurity.web.exception.InvalidTokenException;
import com.ecclesiaflow.springsecurity.web.exception.JwtProcessingException;

/**
 * Service d'authentification centralisée
 * Responsabilité : gestion des authentifications et des tokens
 */
public interface AuthenticationService {
    
    /**
     * Enregistre un nouveau membre
     * @param memberRegistration les données d'enregistrement
     * @return le membre créé
     * @throws InvalidRequestException si les données sont invalides
     */
    Member registerMember(MemberRegistration memberRegistration) throws InvalidRequestException;
    
    /**
     * Authentifie un membre avec ses identifiants
     * @param signinCredentials les identifiants de connexion
     * @return le résultat d'authentification avec les tokens
     * @throws InvalidCredentialsException si les identifiants sont incorrects
     * @throws JwtProcessingException si la génération des tokens échoue
     */
    AuthenticationResult getAuthenticatedMember(SigninCredentials signinCredentials)
            throws InvalidCredentialsException, JwtProcessingException;
    
    /**
     * Rafraîchit un token d'authentification
     * @param refreshTokenRequest la requête de rafraîchissement
     * @return le résultat avec le nouveau token
     * @throws InvalidTokenException si le refresh token est invalide
     * @throws JwtProcessingException si la génération du nouveau token échoue
     */
    AuthenticationResult refreshToken(RefreshTokenRequest refreshTokenRequest) 
            throws InvalidTokenException, JwtProcessingException;
}
