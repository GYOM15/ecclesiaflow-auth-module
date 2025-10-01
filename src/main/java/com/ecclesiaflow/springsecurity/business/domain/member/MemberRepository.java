package com.ecclesiaflow.springsecurity.business.domain.member;

import java.util.Optional;
import  com.ecclesiaflow.springsecurity.io.persistence.repositories.impl.MemberRepositoryImpl;

/**
 * Interface de Repository pour le domaine Membre.
 * <p>
 * Elle définit le contrat pour les opérations de persistance et de récupération des objets
 * de domaine {@link Member}, tel que requis par la logique métier de la couche de domaine.
 * </p>
 *
 * <p><strong>Rôle architectural :</strong> Port de Domaine (Couche Business/Domaine)</p>
 *
 * <p><strong>Responsabilités :</strong></p>
 * <ul>
 * <li>Définir les contrats pour la gestion des {@link Member}s de domaine.</li>
 * <li>Permettre à la couche domaine d'interagir avec la persistance sans connaître son implémentation.</li>
 * </ul>
 *
 * <p><strong>Dépendances :</strong> Aucune dépendance directe vers des frameworks de persistance.</p>
 *
 * <p><strong>Implémentation :</strong> Sera implémentée par  {@link  MemberRepositoryImpl}
 * dans la couche d'infrastructure (IO), qui utilisera une technologie de persistance spécifique
 * (ex: Spring Data JPA via {@code SpringDataMemberRepository}).</p>
 *
 * <p><strong>Cas d'utilisation typiques :</strong></p>
 * <ul>
 * <li>Recherche d'un membre par email (authentification, gestion de compte).</li>
 * <li>Recherche d'un membre par rôle (autorisations, requêtes métier spécifiques).</li>
 * <li>Opérations de sauvegarde (création/mise à jour) et de suppression de membres.</li>
 * </ul>
 *
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
public interface MemberRepository{

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
    Optional<Member> getByEmail(String email);

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
    Member getByRole(Role role);

    /**
     * Sauvegarde un membre (création ou mise à jour).
     * <p>
     * Si le membre n'existe pas (id null), il sera créé.
     * Si le membre existe, il sera mis à jour.
     * </p>
     *
     * @param member le membre à sauvegarder, non null
     * @return le membre sauvegardé avec les données actualisées
     * @throws IllegalArgumentException si member est null
     */
    Member save(Member member);

    /**
     * Supprime un membre du système.
     * <p>
     * Attention : cette opération est irréversible et peut affecter
     * l'intégrité référentielle avec d'autres modules.
     * </p>
     *
     * @param member le membre à supprimer, non null
     * @throws IllegalArgumentException si member est null
     * @throws IllegalStateException si le membre n'existe pas
     */
    void delete(Member member);
}
