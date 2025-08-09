package com.ecclesiaflow.springsecurity.business.services.impl;

import com.ecclesiaflow.springsecurity.web.exception.InvalidTokenException;
import com.ecclesiaflow.springsecurity.web.exception.JwtProcessingException;
import com.ecclesiaflow.springsecurity.business.services.JWTService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

/**
 * Implémentation du service JWT pour la gestion des tokens d'authentification EcclesiaFlow.
 * <p>
 * Cette classe implémente toutes les opérations liées aux tokens JWT : génération,
 * validation, extraction de claims et vérification d'expiration. Utilise la bibliothèque
 * JJWT pour le traitement sécurisé des tokens avec signature HMAC.
 * </p>
 *
 * <p><strong>Dépendances critiques :</strong></p>
 * <ul>
 *   <li>Clé secrète JWT configurée via {@code jwt.secret}</li>
 *   <li>Durées d'expiration configurables via properties Spring</li>
 *   <li>Bibliothèque JJWT pour le traitement sécurisé des tokens</li>
 * </ul>
 *
 * <p><strong>Cas d'utilisation typiques :</strong></p>
 * <ul>
 *   <li>Génération de tokens d'accès après authentification réussie</li>
 *   <li>Génération de refresh tokens pour le renouvellement automatique</li>
 *   <li>Validation des tokens dans les filtres de sécurité</li>
 *   <li>Extraction des informations utilisateur depuis les tokens</li>
 * </ul>
 *
 * <p><strong>Garanties :</strong> Thread-safe (stateless), opérations en mémoire uniquement, signature sécurisée.</p>
 *
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
@Service
public class JWTServiceImpl implements JWTService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.token.expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token.expiration}")
    private long refreshTokenExpiration;

    // ========== PUBLIC API ==========

    @Override
    public String generateAccessToken(UserDetails userDetails) throws JwtProcessingException {
        return buildToken(userDetails, accessTokenExpiration, Map.of());
    }

    @Override
    public String generateRefreshToken(UserDetails userDetails) throws JwtProcessingException {
        Map<String, Object> extraClaims = Map.of("type", "refresh");
        return buildToken(userDetails, refreshTokenExpiration, extraClaims);
    }

    @Override
    public String extractUsername(String token) throws JwtProcessingException {
        return extractClaim(token, Claims::getSubject);
    }

    @Override
    public boolean isTokenValid(String token) throws JwtProcessingException {
            Claims claims = parseAndValidateClaims(token);
            return !claims.getExpiration().before(new Date());
    }

    @Override
    public boolean isRefreshTokenValid(String token) throws JwtProcessingException, InvalidTokenException {
        if (!isTokenValid(token)) {
            return false;
        }
        Claims claims = parseAndValidateClaims(token);
        String tokenType = claims.get("type", String.class);

        if (!"refresh".equals(tokenType)) {
            throw new InvalidTokenException("Le token fourni n'est pas un token de rafraîchissement");
        }
        return true;
    }

    // ========== PRIVATE HELPERS ==========

    private String buildToken(UserDetails userDetails, long expirationTime, Map<String, Object> extraClaims) throws JwtProcessingException {
        return Jwts
                .builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) throws JwtProcessingException {
        final Claims claims = parseAndValidateClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims parseAndValidateClaims(String token) throws JwtProcessingException {
        return Jwts.parserBuilder().setSigningKey(getSignKey()).build().parseClaimsJws(token).getBody();
    }

    // ========== SECURITY CHECKS ==========
    private Key getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
