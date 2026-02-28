package com.ecclesiaflow.springsecurity.business.services.impl;

import com.ecclesiaflow.springsecurity.business.domain.token.SetupToken;
import com.ecclesiaflow.springsecurity.business.domain.token.SetupTokenRepository;
import com.ecclesiaflow.springsecurity.business.exceptions.InvalidTokenException;
import com.ecclesiaflow.springsecurity.business.services.SetupTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

/**
 * Implementation of SetupTokenService using opaque tokens stored hashed in DB.
 */
@Service
@RequiredArgsConstructor
public class SetupTokenServiceImpl implements SetupTokenService {

    private final SetupTokenRepository setupTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${auth.token.setup.ttl-hours:24}")
    private int setupTokenTtlHours;

    @Override
    @Transactional
    public String generateSetupToken(String email, UUID memberId) {
        return generateToken(email, memberId, SetupToken.TokenPurpose.PASSWORD_SETUP, 
                LocalDateTime.now().plusHours(setupTokenTtlHours));
    }

    @Override
    @Transactional(readOnly = true)
    public SetupToken validate(String rawToken, SetupToken.TokenPurpose purpose) {
        String tokenHash = hashToken(rawToken);
        
        SetupToken token = setupTokenRepository.findValidToken(tokenHash, LocalDateTime.now())
                .orElseThrow(() -> new InvalidTokenException("Token is invalid or expired"));

        if (token.getPurpose() != purpose) {
            throw new InvalidTokenException("Token purpose mismatch");
        }
        
        return token;
    }

    @Override
    @Transactional
    public void deleteToken(SetupToken token) {
        setupTokenRepository.delete(token);
    }

    private String generateToken(String email, UUID memberId, SetupToken.TokenPurpose purpose, LocalDateTime expiresAt) {
        setupTokenRepository.revokeTokensForMember(memberId);

        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        String tokenHash = hashToken(rawToken);

        SetupToken setupToken = SetupToken.builder()
                .tokenHash(tokenHash)
                .email(email)
                .memberId(memberId)
                .purpose(purpose)
                .status(SetupToken.TokenStatus.ISSUED)
                .expiresAt(expiresAt)
                .createdAt(LocalDateTime.now())
                .build();

        setupTokenRepository.save(setupToken);

        return rawToken;
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
