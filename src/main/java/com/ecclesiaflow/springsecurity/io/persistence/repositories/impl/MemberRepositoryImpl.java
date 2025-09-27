package com.ecclesiaflow.springsecurity.io.persistence.repositories.impl;

import com.ecclesiaflow.springsecurity.business.domain.member.MemberRepository;
import com.ecclesiaflow.springsecurity.business.domain.member.Member;
import com.ecclesiaflow.springsecurity.business.domain.member.Role;
import com.ecclesiaflow.springsecurity.io.persistence.jpa.MemberEntity;
import com.ecclesiaflow.springsecurity.io.persistence.mappers.MemberPersistenceMapper;
import com.ecclesiaflow.springsecurity.io.persistence.jpa.SpringDataMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Implémentation du repository de domaine pour les membres EcclesiaFlow.
 * <p>
 * Cette classe fait le pont entre la couche domaine et la couche de persistance JPA.
 * Elle utilise le pattern Repository avec adaptation entre les entités JPA et les
 * objets de domaine, respectant ainsi la séparation des couches architecturales.
 * Suit exactement l'architecture du module membres.
 * </p>
 * 
 * <p><strong>Rôle architectural :</strong> Repository - Adaptation domaine/persistance</p>
 * 
 * <p><strong>Responsabilités principales :</strong></p>
 * <ul>
 *   <li>Adaptation entre objets domaine {@link Member} et entités JPA {@link MemberEntity}</li>
 *   <li>Délégation des opérations CRUD vers Spring Data JPA</li>
 *   <li>Conversion bidirectionnelle via {@link MemberPersistenceMapper}</li>
 *   <li>Implémentation de l'interface domaine {@link MemberRepository}</li>
 * </ul>
 * 
 * <p><strong>Pattern architectural :</strong></p>
 * <ul>
 *   <li>Repository Pattern - Encapsulation de l'accès aux données</li>
 *   <li>Adapter Pattern - Adaptation entre couches</li>
 *   <li>Dependency Inversion - Implémentation d'interface domaine</li>
 * </ul>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class MemberRepositoryImpl implements MemberRepository {

    private final SpringDataMemberRepository springDataRepository;
    private final MemberPersistenceMapper mapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Member> findByEmail(String email) {
        return springDataRepository.findByEmail(email)
                .map(mapper::toDomain);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Member findByRole(Role role) {
        MemberEntity entity = springDataRepository.findByRole(role);
        return mapper.toDomain(entity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Member save(Member member) {
        MemberEntity entity = mapper.toEntity(member);
        MemberEntity savedEntity = springDataRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(Member member) {
        springDataRepository.delete(mapper.toEntity(member));
    }

}
