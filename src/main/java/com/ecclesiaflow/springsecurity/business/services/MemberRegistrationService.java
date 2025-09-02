package com.ecclesiaflow.springsecurity.business.services;

import com.ecclesiaflow.springsecurity.business.domain.MemberRegistration;
import com.ecclesiaflow.springsecurity.io.entities.Member;

/**
 * Service de domaine dédié à l'enregistrement de nouveaux membres EcclesiaFlow.
 * <p>
 * Cette interface définit les opérations d'inscription et de validation des nouveaux
 * membres dans le système.
 * </p>
 * 
 * <p><strong>Responsabilités principales :</strong></p>
 * <ul>
 *   <li>Validation des données d'inscription</li>
 *   <li>Encodage sécurisé des mots de passe</li>
 *   <li>Persistance des nouveaux membres</li>
 *   <li>Vérification de l'unicité des emails</li>
 * </ul>
 * 
 * <p><strong>Cas d'utilisation typiques :</strong></p>
 * <ul>
 *   <li>Inscription de nouveaux utilisateurs via formulaire web</li>
 *   <li>Création de comptes administrateurs</li>
 *   <li>Import en masse de membres</li>
 * </ul>
 * 
 * <p><strong>Garanties :</strong> Thread-safe, transactionnel.</p>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
public interface MemberRegistrationService {
    
    /**
     * Enregistre un nouveau membre dans le système.
     * <p>
     * Cette méthode valide les données d'inscription, encode le mot de passe
     * de manière sécurisée et persiste le nouveau membre en base de données.
     * Vérifie l'unicité de l'email avant l'enregistrement.
     * </p>
     * 
     * @param registration les données d'enregistrement du membre, non null
     * @return le membre créé avec son identifiant généré
     * @throws IllegalArgumentException si l'email existe déjà ou si les données sont invalides
     * 
     * @implNote Opération transactionnelle en écriture.
     */
    Member registerMember(MemberRegistration registration);
    
    /**
     * Vérifie si un email est déjà utilisé
     * @param email l'email à vérifier
     * @return true si l'email existe déjà
     */
    boolean isEmailAlreadyUsed(String email);
}
