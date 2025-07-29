package com.ecclesiaflow.springsecurity.services.impl;

import com.ecclesiaflow.springsecurity.services.PasswordService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Implémentation du service de gestion des mots de passe
 * Utilise Spring Security PasswordEncoder tout en maintenant l'abstraction métier
 */
@Service
@RequiredArgsConstructor
public class PasswordServiceImpl implements PasswordService {
    
    private final PasswordEncoder passwordEncoder;
    
    @Override
    public String encodePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Le mot de passe ne peut pas être vide");
        }
        return passwordEncoder.encode(rawPassword);
    }
    
    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}
