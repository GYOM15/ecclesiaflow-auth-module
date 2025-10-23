package com.ecclesiaflow.springsecurity.business.services;

import java.util.UUID;

/**
 * Service pour la gestion des mots de passe des membres.
 * 
 * <p>Ce service gère les opérations liées aux mots de passe :</p>
 * <ul>
 *   <li>Définition du mot de passe initial après confirmation</li>
 *   <li>Changement de mot de passe pour un membre authentifié</li>
 * </ul>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
public interface PasswordService {

    /**
     * Définit le mot de passe initial d'un membre après confirmation.
     * <p>
     * La validation d'authentification est déléguée à AuthenticationService.
     * </p>
     * 
     * @param email Email du membre (déjà validé par AuthenticationService)
     * @param password Nouveau mot de passe
     * @param memberId UUID du membre (pour lier avec le module members)
     * @throws RuntimeException si le membre n'existe pas ou si l'opération échoue
     */
    void setInitialPassword(String email, String password, UUID memberId);

    /**
     * Change le mot de passe d'un membre authentifié.
     * 
     * @param email Email du membre
     * @param currentPassword Mot de passe actuel
     * @param newPassword Nouveau mot de passe
     * @throws RuntimeException si le mot de passe actuel est incorrect ou si le membre n'existe pas
     */
    void changePassword(String email, String currentPassword, String newPassword);
}
