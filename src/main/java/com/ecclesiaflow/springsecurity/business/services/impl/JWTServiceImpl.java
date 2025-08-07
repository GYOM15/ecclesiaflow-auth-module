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
            // parseAndValidateClaims valide automatiquement la signature ET retourne les claims
            Claims claims = parseAndValidateClaims(token);
            // Vérifier seulement l'expiration
            return !claims.getExpiration().before(new Date());
    }

    @Override
    public boolean isRefreshTokenValid(String token) throws JwtProcessingException, InvalidTokenException {
        // Validation complète : signature + expiration + type en une seule expression
        String tokenType = extractClaim(token, claims -> claims.get("type", String.class));
        return isTokenValid(token) && "refresh".equals(tokenType);
    }

    // ========== TOKEN BUILDING ==========

    private String buildToken(UserDetails userDetails, long expirationTime, Map<String, Object> extraClaims) throws JwtProcessingException {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // ========== CLAIMS EXTRACTION ==========

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) throws JwtProcessingException {
        final Claims claims = parseAndValidateClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Parse et valide un token JWT (signature + format) puis extrait les claims
     * Cette méthode fait la validation complète du token ET retourne les claims
     * @param token le token à parser et valider
     * @return les claims du token si valide
     * @throws JwtProcessingException si le token est invalide (signature, format, etc.)
     */
    private Claims parseAndValidateClaims(String token) throws JwtProcessingException {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // ========== SECURITY CHECKS ==========

    private boolean isTokenExpired(String token) throws JwtProcessingException {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    // ========== SIGNING KEY ==========

    private Key getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
