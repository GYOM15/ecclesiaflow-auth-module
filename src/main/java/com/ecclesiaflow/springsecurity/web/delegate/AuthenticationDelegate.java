package com.ecclesiaflow.springsecurity.web.delegate;

import com.ecclesiaflow.springsecurity.business.domain.member.Member;
import com.ecclesiaflow.springsecurity.business.domain.password.SigninCredentials;
import com.ecclesiaflow.springsecurity.business.domain.token.TemporaryToken;
import com.ecclesiaflow.springsecurity.business.domain.token.TokenCredentials;
import com.ecclesiaflow.springsecurity.business.domain.token.UserTokens;
import com.ecclesiaflow.springsecurity.business.services.AuthenticationService;
import com.ecclesiaflow.springsecurity.web.exception.InvalidCredentialsException;
import com.ecclesiaflow.springsecurity.web.exception.InvalidTokenException;
import com.ecclesiaflow.springsecurity.web.exception.JwtProcessingException;
import com.ecclesiaflow.springsecurity.web.mappers.MemberMapper;
import com.ecclesiaflow.springsecurity.web.mappers.OpenApiModelMapper;
import com.ecclesiaflow.springsecurity.web.mappers.TemporaryTokenMapper;
import com.ecclesiaflow.springsecurity.web.model.JwtAuthenticationResponse;
import com.ecclesiaflow.springsecurity.web.model.RefreshTokenRequest;
import com.ecclesiaflow.springsecurity.web.model.SigninRequest;
import com.ecclesiaflow.springsecurity.web.model.TemporaryTokenRequest;
import com.ecclesiaflow.springsecurity.web.model.TemporaryTokenResponse;
import com.ecclesiaflow.springsecurity.web.security.Jwt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * Délégué pour la gestion de l'authentification - Pattern Delegate avec OpenAPI Generator.
 * <p>
 * Ce délégué contient toute la logique métier pour l'authentification JWT,
 * séparant ainsi les responsabilités entre le contrôleur (gestion HTTP) et la logique applicative.
 * </p>
 * 
 * <p><strong>Architecture :</strong></p>
 * <pre>
 * AuthenticationController (implémente AuthenticationApi)
 *    ↓ délègue à
 * AuthenticationDelegate ← Cette classe
 *    ↓ utilise
 * AuthenticationService + Jwt + OpenApiModelMapper
 * </pre>
 * 
 * <p><strong>Responsabilités :</strong></p>
 * <ul>
 *   <li>Génération de tokens JWT (access + refresh)</li>
 *   <li>Rafraîchissement des tokens</li>
 *   <li>Génération de tokens temporaires</li>
 *   <li>Transformation des modèles OpenAPI vers objets métier</li>
 *   <li>Transformation des résultats métier vers modèles OpenAPI</li>
 * </ul>
 * 
 * @author EcclesiaFlow Team
 * @since 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationDelegate {

    private final AuthenticationService authenticationService;
    private final Jwt jwt;
    private final OpenApiModelMapper openApiModelMapper;
    private final TemporaryTokenMapper temporaryTokenMapper;

    /**
     * Génère un token JWT pour l'authentification d'un utilisateur.
     * <p>
     * Processus :
     * 1. Conversion du modèle OpenAPI vers l'objet métier SigninCredentials
     * 2. Authentification via le service métier
     * 3. Génération des tokens JWT (access + refresh)
     * 4. Transformation vers le modèle OpenAPI
     * </p>
     * 
     * @param signinRequest Requête d'authentification (modèle OpenAPI)
     * @return Réponse avec tokens JWT
     * @throws InvalidCredentialsException si les identifiants sont invalides
     * @throws InvalidTokenException si le token est invalide
     * @throws JwtProcessingException si une erreur se produit lors du traitement du token
     */
    public ResponseEntity<JwtAuthenticationResponse> generateToken(SigninRequest signinRequest) 
            throws InvalidCredentialsException, InvalidTokenException, JwtProcessingException {
        
        // Conversion du modèle OpenAPI vers l'objet métier
        SigninCredentials credentials = MemberMapper.fromSigninRequest(signinRequest);
        
        // Authentification et récupération du membre
        Member member = authenticationService.getAuthenticatedMember(credentials);
        
        // Génération des tokens JWT
        UserTokens userTokens = jwt.generateUserTokens(member);
        
        // Transformation vers le modèle OpenAPI
        JwtAuthenticationResponse response = openApiModelMapper.createJwtAuthenticationResponse(userTokens);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Rafraîchit un token JWT existant.
     * <p>
     * Processus :
     * 1. Extraction du refresh token depuis la requête
     * 2. Validation du refresh token et extraction de l'email
     * 3. Récupération du membre
     * 4. Génération de nouveaux tokens
     * 5. Transformation vers le modèle OpenAPI
     * </p>
     * 
     * @param refreshTokenRequest Requête de rafraîchissement (modèle OpenAPI)
     * @return Réponse avec nouveaux tokens JWT
     * @throws InvalidTokenException si le refresh token est invalide
     * @throws JwtProcessingException si une erreur se produit lors du traitement
     */
    public ResponseEntity<JwtAuthenticationResponse> refreshToken(RefreshTokenRequest refreshTokenRequest) 
            throws InvalidTokenException, JwtProcessingException {
        
        // Extraction et validation du refresh token
        TokenCredentials tokenCredentials = new TokenCredentials(refreshTokenRequest.getRefreshToken());
        String email = jwt.validateAndExtractEmail(tokenCredentials.token());
        
        // Récupération du membre
        Member member = authenticationService.getMemberByEmail(email);
        
        // Génération de nouveaux tokens
        UserTokens userTokens = jwt.refreshTokenForMember(tokenCredentials.token(), member);
        
        // Transformation vers le modèle OpenAPI
        JwtAuthenticationResponse response = openApiModelMapper.createJwtAuthenticationResponse(userTokens);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Génère un token temporaire pour la confirmation d'inscription.
     * <p>
     * Processus :
     * 1. Extraction de l'email depuis la requête
     * 2. Génération du token temporaire JWT
     * 3. Transformation vers le modèle OpenAPI
     * </p>
     * 
     * @param temporaryTokenRequest Requête contenant l'email (modèle OpenAPI)
     * @return Réponse avec token temporaire
     * @throws InvalidTokenException si le token ne peut pas être généré
     * @throws JwtProcessingException si une erreur se produit lors du traitement
     */
    public ResponseEntity<TemporaryTokenResponse> generateTemporaryToken(TemporaryTokenRequest temporaryTokenRequest) 
            throws InvalidTokenException, JwtProcessingException {
        
        // Transformation DTO web → Domain
        TemporaryToken tempToken =
            temporaryTokenMapper.toDomain(temporaryTokenRequest);

        String temporaryToken = jwt.generateTemporaryToken(
            tempToken.email(),
            tempToken.memberId()
        );
        
        // Transformation vers le modèle OpenAPI
        TemporaryTokenResponse response = openApiModelMapper.createTemporaryTokenResponse(temporaryToken);
        
        return ResponseEntity.ok(response);
    }
}
