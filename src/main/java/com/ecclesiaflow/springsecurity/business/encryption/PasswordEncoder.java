package com.ecclesiaflow.springsecurity.business.encryption;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Classe concrète pour l'encodage des mots de passe
 * Utilise BCrypt de façon permanente - pas d'interface car l'algorithme
 * ne peut pas changer sans rendre tous les comptes existants inaccessibles
 */
@Component
public class PasswordEncoder {
    
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    
    /**
     * Encode un mot de passe en clair avec BCrypt
     * Aucune validation - Jackson s'en charge déjà
     * Aucun trim() - les espaces font partie du mot de passe
     * @param rawPassword le mot de passe en clair
     * @return le mot de passe encodé
     */
    public String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }
}
