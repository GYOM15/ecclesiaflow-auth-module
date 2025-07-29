package com.ecclesiaflow.springsecurity.services;

import com.ecclesiaflow.springsecurity.domain.MemberRegistration;
import com.ecclesiaflow.springsecurity.entities.Member;

/**
 * Service dédié à l'enregistrement de nouveaux membres
 * Respecte le principe de responsabilité unique (SRP)
 */
public interface MemberRegistrationService {
    
    /**
     * Enregistre un nouveau membre dans le système
     * @param registration les données d'enregistrement
     * @return le membre créé
     * @throws IllegalArgumentException si l'email existe déjà
     */
    Member registerMember(MemberRegistration registration);
    
    /**
     * Vérifie si un email est déjà utilisé
     * @param email l'email à vérifier
     * @return true si l'email existe déjà
     */
    boolean isEmailAlreadyUsed(String email);
}
