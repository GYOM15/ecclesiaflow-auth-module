package com.ecclesiaflow.springsecurity.business.services;

import reactor.core.publisher.Mono;

/**
 * Service pour communiquer avec le module Members d'EcclesiaFlow.
 * <p>
 * Ce service permet au module d'authentification d'interroger le module Members
 * pour vérifier le statut de confirmation des membres sans dupliquer les données.
 * Respecte l'architecture inter-modules et la séparation des responsabilités.
 * </p>
 * 
 * <p><strong>Responsabilités :</strong></p>
 * <ul>
 *   <li>Vérification du statut de confirmation des membres</li>
 *   <li>Communication HTTP avec le module Members</li>
 *   <li>Gestion des erreurs de communication inter-modules</li>
 * </ul>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
public interface MembersModuleService {
    
    /**
     * Vérifie si un membre avec cet email est confirmé.
     * <p>
     * Cette méthode interroge le module Members via HTTP pour vérifier
     * le statut de confirmation d'un membre sans dupliquer les données.
     * </p>
     * 
     * @param email l'email du membre à vérifier
     * @return true si le membre existe et est confirmé, false sinon
     * @throws RuntimeException si la communication avec le module Members échoue
     */
    boolean isEmailNotConfirmed(String email);
}
