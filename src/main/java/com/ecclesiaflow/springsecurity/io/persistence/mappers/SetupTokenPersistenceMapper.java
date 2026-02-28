package com.ecclesiaflow.springsecurity.io.persistence.mappers;

import com.ecclesiaflow.springsecurity.business.domain.token.SetupToken;
import com.ecclesiaflow.springsecurity.io.persistence.jpa.SetupTokenEntity;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper for SetupToken domain ↔ SetupTokenEntity conversion.
 * Infrastructure layer - adapts persistence to domain.
 */
@Mapper(componentModel = "spring")
public interface SetupTokenPersistenceMapper {

    SetupToken toDomain(SetupTokenEntity entity);

    SetupTokenEntity toEntity(SetupToken domain);

    default SetupToken toDomainOrThrow(SetupTokenEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("SetupTokenEntity must not be null");
        }
        return toDomain(entity);
    }

    default SetupTokenEntity toEntityOrThrow(SetupToken domain) {
        if (domain == null) {
            throw new IllegalArgumentException("SetupToken domain object must not be null");
        }
        return toEntity(domain);
    }
}
