package com.ecclesiaflow.springsecurity.business.domain.member;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository Spring Data JPA pour la gestion des entités Member.
 * <p>
 * Cette interface définit les opérations de persistance pour les membres EcclesiaFlow.
 * Étend {@link JpaRepository} pour bénéficier des opérations CRUD standard et
 * ajoute des méthodes de recherche spécifiques au domaine métier.
 * </p>
 * 
 * <p><strong>Rôle architectural :</strong> Couche d'accès aux données (Repository Pattern)</p>
 * 
 * <p><strong>Dépendances critiques :</strong></p>
 * <ul>
 *   <li>Spring Data JPA - Génération automatique des implémentations</li>
 *   <li>Base de données relationnelle - Stockage persistant</li>
 * </ul>
 * 
 * <p><strong>Cas d'utilisation typiques :</strong></p>
 * <ul>
 *   <li>Recherche de membre par email lors de l'authentification</li>
 *   <li>Recherche de membre par rôle pour les autorisations</li>
 *   <li>Opérations CRUD standard via JpaRepository</li>
 * </ul>
 * 
 * <p><strong>Garanties :</strong> Thread-safe, transactionnel, gestion automatique des connexions.</p>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
public interface MemberRepository extends JpaRepository<Member, UUID> {

    /**
     * Recherche un membre par son adresse email.
     * <p>
     * L'email étant unique dans le système, cette méthode retourne au maximum
     * un seul membre. Utilisée principalement pour l'authentification.
     * </p>
     * 
     * @param email l'adresse email du membre à rechercher, non null
     * @return un {@link Optional} contenant le membre si trouvé, vide sinon
     * @throws IllegalArgumentException si email est null
     * 
     * @implNote Génère automatiquement la requête SQL : SELECT * FROM members WHERE email = ?
     */
    Optional<Member> findByEmail(String email);

    /**
     * Recherche un membre par son rôle.
     * <p>
     * Cette méthode retourne le premier membre trouvé avec le rôle spécifié.
     * Utilisée pour les opérations d'administration et de gestion des permissions.
     * </p>
     * 
     * @param role le rôle du membre à rechercher, non null
     * @return le membre ayant ce rôle, null si aucun membre trouvé
     * @throws IllegalArgumentException si role est null
     * 
     * @implNote Génère automatiquement la requête SQL : SELECT * FROM members WHERE role = ? LIMIT 1
     */
    Member findByRole(Role role);
}
