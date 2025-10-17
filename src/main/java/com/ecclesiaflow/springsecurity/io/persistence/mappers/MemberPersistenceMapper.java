package com.ecclesiaflow.springsecurity.io.persistence.mappers;

import com.ecclesiaflow.springsecurity.business.domain.member.Member;
import com.ecclesiaflow.springsecurity.io.persistence.jpa.MemberEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper pour la conversion bidirectionnelle entre objets domaine et entités JPA.
 * <p>
 * Cette classe gère la transformation entre les objets métier {@link Member} de la couche domaine
 * et les entités JPA {@link MemberEntity} de la couche persistance. Elle respecte la séparation
 * des couches architecturales en isolant le domaine des détails de persistance.
 * Suit exactement l'architecture du module membres.
 * </p>
 * 
 * <p><strong>Rôle architectural :</strong> Mapper - Adaptation domaine/persistance</p>
 * 
 * <p><strong>Responsabilités principales :</strong></p>
 * <ul>
 *   <li>Conversion des entités JPA vers objets domaine (toDomain)</li>
 *   <li>Conversion des objets domaine vers entités JPA (toEntity)</li>
 *   <li>Préservation de l'intégrité des données lors des conversions</li>
 *   <li>Gestion des champs spéciaux (horodatages, identifiants)</li>
 * </ul>
 * 
 * <p><strong>Pattern de mapping :</strong></p>
 * <ul>
 *   <li>Mapping 1:1 entre champs correspondants</li>
 *   <li>Préservation des valeurs null appropriées</li>
 *   <li>Gestion des champs auto-générés (createdAt, updatedAt)</li>
 *   <li>Conversion fidèle des types (UUID, LocalDateTime, enum)</li>
 * </ul>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
@Component
public class MemberPersistenceMapper {

    /**
     * Convertit une entité JPA en objet domaine.
     * <p>
     * Cette méthode transforme une {@link MemberEntity} provenant de la base de données
     * en objet {@link Member} utilisable par la couche domaine. Tous les champs sont
     * mappés fidèlement, y compris les horodatages et les états booléens.
     * </p>
     * 
     * @param entity l'entité JPA à convertir, peut être null
     * @return l'objet domaine correspondant, null si entity est null
     */
    public Member toDomain(MemberEntity entity) {
        if (entity == null) {
            return null;
        }

        return Member.builder().
                id(entity.getId()).
                memberId(entity.getMemberId()).
                email(entity.getEmail()).
                role(entity.getRole()).
                password(entity.getPassword()).
                createdAt(entity.getCreatedAt()).
                updatedAt(entity.getUpdatedAt()).
                enabled(entity.isEnabled()).
                build();
    }

    /**
     * Convertit un objet domaine en entité JPA.
     * <p>
     * Cette méthode transforme un objet {@link Member} de la couche domaine
     * en {@link MemberEntity} persistable en base de données. Les horodatages
     * sont préservés ou initialisés selon le contexte.
     * </p>
     * 
     * @param domain l'objet domaine à convertir, peut être null
     * @return l'entité JPA correspondante, null si member est null
     */
    public MemberEntity toEntity(Member domain) {
        if (domain == null) {
            throw new IllegalArgumentException("L'objet domaine ne peut pas être null");
        }

        return MemberEntity.builder().
                id(domain.getId()).
                memberId(domain.getMemberId()).
                email(domain.getEmail()).
                role(domain.getRole()).
                password(domain.getPassword()).
                createdAt(domain.getCreatedAt()).
                updatedAt(domain.getUpdatedAt()).
                enabled(domain.isEnabled()).
                build();
    }
}
