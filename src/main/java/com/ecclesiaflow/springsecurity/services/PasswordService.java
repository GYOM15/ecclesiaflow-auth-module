package com.ecclesiaflow.springsecurity.services;

/**
 * Service métier pour la gestion des mots de passe
 * Abstraction qui découple la logique métier des détails d'implémentation
 */
public interface PasswordService {
    
    /**
     * Encode un mot de passe en clair
     * @param rawPassword le mot de passe en clair
     * @return le mot de passe encodé
     */
    String encodePassword(String rawPassword);
    
    /**
     * Vérifie si un mot de passe en clair correspond au mot de passe encodé
     * @param rawPassword le mot de passe en clair
     * @param encodedPassword le mot de passe encodé
     * @return true si les mots de passe correspondent
     */
    boolean matches(String rawPassword, String encodedPassword);
}
