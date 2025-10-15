package com.ecclesiaflow.springsecurity.web.controller;

import com.ecclesiaflow.springsecurity.web.api.AuthenticationApi;
import com.ecclesiaflow.springsecurity.web.delegate.AuthenticationDelegate;
import com.ecclesiaflow.springsecurity.web.exception.InvalidCredentialsException;
import com.ecclesiaflow.springsecurity.web.exception.InvalidTokenException;
import com.ecclesiaflow.springsecurity.web.exception.JwtProcessingException;
import com.ecclesiaflow.springsecurity.web.model.JwtAuthenticationResponse;
import com.ecclesiaflow.springsecurity.web.model.RefreshTokenRequest;
import com.ecclesiaflow.springsecurity.web.model.SigninRequest;
import com.ecclesiaflow.springsecurity.web.model.TemporaryTokenRequest;
import com.ecclesiaflow.springsecurity.web.model.TemporaryTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrôleur REST pour l'authentification - Pattern Delegate avec OpenAPI Generator.
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
 * AuthenticationApi
 *    ↓ implémentée par
 * AuthenticationController ← Cette classe
 *    ↓ délègue à
 * AuthenticationDelegate
 *    ↓ utilise
 * AuthenticationService + Jwt
 * </pre>
 * 
 * <p><strong>Responsabilités :</strong></p>
 * <ul>
 *   <li>Implémentation de l'interface AuthenticationApi générée</li>
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
public class AuthenticationController implements AuthenticationApi {

    private final AuthenticationDelegate authenticationDelegate;

    /**
     * Génère un token JWT pour l'authentification d'un utilisateur.
     * <p>
     * Authentifie un utilisateur et génère un token JWT d'accès ainsi qu'un refresh token.
     * </p>
     * 
     * @param signinRequest Requête d'authentification contenant les identifiants
     * @return Réponse d'authentification contenant le token JWT
     * @throws InvalidCredentialsException si les identifiants sont invalides
     * @throws InvalidTokenException si le token est invalide
     * @throws JwtProcessingException si une erreur se produit lors du traitement du token
     * 
     * @implNote <strong>Implémentation :</strong> Délègue au {@link AuthenticationDelegate}
     * @see AuthenticationDelegate#generateToken(SigninRequest)
     */
    @Override
    public ResponseEntity<JwtAuthenticationResponse> _generateToken(SigninRequest signinRequest) 
            throws InvalidCredentialsException, InvalidTokenException, JwtProcessingException {
        return authenticationDelegate.generateToken(signinRequest);
    }

    /**
     * Rafraîchit un token JWT existant.
     * <p>
     * Génère un nouveau token JWT à partir d'un refresh token valide.
     * </p>
     * 
     * @param refreshTokenRequest Requête de rafraîchissement du token
     * @return Réponse d'authentification contenant le nouveau token JWT
     * @throws InvalidTokenException si le refresh token est invalide
     * @throws JwtProcessingException si une erreur se produit lors du traitement
     * 
     * @implNote <strong>Implémentation :</strong> Délègue au {@link AuthenticationDelegate}
     * @see AuthenticationDelegate#refreshToken(RefreshTokenRequest)
     */
    @Override
    public ResponseEntity<JwtAuthenticationResponse> _refreshToken(RefreshTokenRequest refreshTokenRequest) 
            throws InvalidTokenException, JwtProcessingException {
        return authenticationDelegate.refreshToken(refreshTokenRequest);
    }

    /**
     * Génère un token temporaire pour la confirmation d'inscription.
     * <p>
     * Génère un token temporaire pour la confirmation d'inscription d'un membre.
     * </p>
     *
     * @param temporaryTokenRequest Requête contenant l'email pour générer le token temporaire
     * @return Réponse contenant le token temporaire
     * @throws InvalidTokenException si le token ne peut pas être généré
     * @throws JwtProcessingException si une erreur se produit lors du traitement
     * 
     * @implNote <strong>Implémentation :</strong> Délègue au {@link AuthenticationDelegate}
     * @see AuthenticationDelegate#generateTemporaryToken(TemporaryTokenRequest)
     */
    @Override
    public ResponseEntity<TemporaryTokenResponse> _generateTemporaryToken(TemporaryTokenRequest temporaryTokenRequest) 
            throws InvalidTokenException, JwtProcessingException {
        return authenticationDelegate.generateTemporaryToken(temporaryTokenRequest);
    }

}
