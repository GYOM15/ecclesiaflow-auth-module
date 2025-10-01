package com.ecclesiaflow.springsecurity.io.persistence.jpa;

import com.ecclesiaflow.springsecurity.business.domain.member.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository Spring Data JPA pour la gestion des entités MemberEntity.
 * <p>
 * Cette interface fournit les opérations CRUD et de recherche pour les membres
 * au niveau de la couche de persistance. Elle travaille exclusivement avec les
 * entités JPA {@link MemberEntity} et ne connaît rien du domaine métier.
 * Suit exactement l'architecture du module membres.
 * </p>
 * 
 * <p><strong>Rôle architectural :</strong> Repository JPA - Accès direct aux données</p>
 * 
 * <p><strong>Responsabilités principales :</strong></p>
 * <ul>
 *   <li>Opérations CRUD automatiques via JpaRepository</li>
 *   <li>Requêtes de recherche personnalisées par méthodes nommées</li>
 *   <li>Gestion des contraintes d'unicité (email)</li>
 * </ul>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
@Repository
public interface SpringDataMemberRepository extends JpaRepository<MemberEntity, UUID> {

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
     */
    Optional<MemberEntity> findByEmail(String email);

    /**
     * Recherche un membre par son rôle.
     * <p>
     * Cette méthode retourne un membre avec le rôle spécifié.
     * <strong>Note:</strong> Cette méthode est principalement utilisée pour les tests.
     * Elle s'attend à ce qu'il y ait un seul membre par rôle dans le contexte de test.
     * </p>
     *
     * @param role le rôle du membre à rechercher, non null
     * @return le membre ayant ce rôle, null si aucun membre trouvé
     * @throws IllegalArgumentException si role est null
     * @throws org.springframework.dao.IncorrectResultSizeDataAccessException si plusieurs membres ont le même rôle
     */
    MemberEntity findByRole(Role role);
}
