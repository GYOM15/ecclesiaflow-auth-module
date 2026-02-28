package com.ecclesiaflow.springsecurity.io.persistence.mappers;

import com.ecclesiaflow.springsecurity.business.domain.token.SetupToken;
import com.ecclesiaflow.springsecurity.io.persistence.jpa.SetupTokenEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("SetupTokenPersistenceMapper - Tests unitaires")
class SetupTokenPersistenceMapperTest {

    private SetupTokenPersistenceMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(SetupTokenPersistenceMapper.class);
    }

    @Nested
    @DisplayName("toDomain - Conversion Entity vers Domain")
    class ToDomainTests {

        @Test
        @DisplayName("Devrait convertir une entité en objet domaine")
        void shouldConvertEntityToDomain() {
            SetupTokenEntity entity = createValidEntity();

            SetupToken domain = mapper.toDomain(entity);

            assertThat(domain).isNotNull();
            assertThat(domain.getId()).isEqualTo(entity.getId());
            assertThat(domain.getTokenHash()).isEqualTo(entity.getTokenHash());
            assertThat(domain.getEmail()).isEqualTo(entity.getEmail());
            assertThat(domain.getMemberId()).isEqualTo(entity.getMemberId());
            assertThat(domain.getPurpose().name()).isEqualTo(entity.getPurpose().name());
            assertThat(domain.getStatus().name()).isEqualTo(entity.getStatus().name());
            assertThat(domain.getExpiresAt()).isEqualTo(entity.getExpiresAt());
            assertThat(domain.getCreatedAt()).isEqualTo(entity.getCreatedAt());
        }

        @Test
        @DisplayName("Devrait retourner null si l'entité est null")
        void shouldReturnNullWhenEntityIsNull() {
            SetupToken domain = mapper.toDomain(null);

            assertThat(domain).isNull();
        }
    }

    @Nested
    @DisplayName("toEntity - Conversion Domain vers Entity")
    class ToEntityTests {

        @Test
        @DisplayName("Devrait convertir un objet domaine en entité")
        void shouldConvertDomainToEntity() {
            SetupToken domain = createValidDomain();

            SetupTokenEntity entity = mapper.toEntity(domain);

            assertThat(entity).isNotNull();
            assertThat(entity.getId()).isEqualTo(domain.getId());
            assertThat(entity.getTokenHash()).isEqualTo(domain.getTokenHash());
            assertThat(entity.getEmail()).isEqualTo(domain.getEmail());
            assertThat(entity.getMemberId()).isEqualTo(domain.getMemberId());
            assertThat(entity.getPurpose().name()).isEqualTo(domain.getPurpose().name());
            assertThat(entity.getStatus().name()).isEqualTo(domain.getStatus().name());
            assertThat(entity.getExpiresAt()).isEqualTo(domain.getExpiresAt());
            assertThat(entity.getCreatedAt()).isEqualTo(domain.getCreatedAt());
        }

        @Test
        @DisplayName("Devrait retourner null si le domaine est null")
        void shouldReturnNullWhenDomainIsNull() {
            SetupTokenEntity entity = mapper.toEntity(null);

            assertThat(entity).isNull();
        }
    }

    @Nested
    @DisplayName("toDomainOrThrow - Conversion avec validation")
    class ToDomainOrThrowTests {

        @Test
        @DisplayName("Devrait convertir une entité valide")
        void shouldConvertValidEntity() {
            SetupTokenEntity entity = createValidEntity();

            SetupToken domain = mapper.toDomainOrThrow(entity);

            assertThat(domain).isNotNull();
            assertThat(domain.getId()).isEqualTo(entity.getId());
        }

        @Test
        @DisplayName("Devrait lever une exception si l'entité est null")
        void shouldThrowExceptionWhenEntityIsNull() {
            assertThatThrownBy(() -> mapper.toDomainOrThrow(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("SetupTokenEntity must not be null");
        }
    }

    @Nested
    @DisplayName("toEntityOrThrow - Conversion avec validation")
    class ToEntityOrThrowTests {

        @Test
        @DisplayName("Devrait convertir un domaine valide")
        void shouldConvertValidDomain() {
            SetupToken domain = createValidDomain();

            SetupTokenEntity entity = mapper.toEntityOrThrow(domain);

            assertThat(entity).isNotNull();
            assertThat(entity.getId()).isEqualTo(domain.getId());
        }

        @Test
        @DisplayName("Devrait lever une exception si le domaine est null")
        void shouldThrowExceptionWhenDomainIsNull() {
            assertThatThrownBy(() -> mapper.toEntityOrThrow(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("SetupToken domain object must not be null");
        }
    }

    @Nested
    @DisplayName("Bidirectionnalité - Tests de conversion aller-retour")
    class BidirectionalityTests {

        @Test
        @DisplayName("Devrait conserver les données lors d'une conversion Entity -> Domain -> Entity")
        void shouldPreserveDataInEntityToDomainToEntity() {
            SetupTokenEntity originalEntity = createValidEntity();

            SetupToken domain = mapper.toDomain(originalEntity);
            SetupTokenEntity resultEntity = mapper.toEntity(domain);

            assertThat(resultEntity.getId()).isEqualTo(originalEntity.getId());
            assertThat(resultEntity.getTokenHash()).isEqualTo(originalEntity.getTokenHash());
            assertThat(resultEntity.getEmail()).isEqualTo(originalEntity.getEmail());
            assertThat(resultEntity.getMemberId()).isEqualTo(originalEntity.getMemberId());
            assertThat(resultEntity.getPurpose()).isEqualTo(originalEntity.getPurpose());
            assertThat(resultEntity.getStatus()).isEqualTo(originalEntity.getStatus());
        }

        @Test
        @DisplayName("Devrait conserver les données lors d'une conversion Domain -> Entity -> Domain")
        void shouldPreserveDataInDomainToEntityToDomain() {
            SetupToken originalDomain = createValidDomain();

            SetupTokenEntity entity = mapper.toEntity(originalDomain);
            SetupToken resultDomain = mapper.toDomain(entity);

            assertThat(resultDomain.getId()).isEqualTo(originalDomain.getId());
            assertThat(resultDomain.getTokenHash()).isEqualTo(originalDomain.getTokenHash());
            assertThat(resultDomain.getEmail()).isEqualTo(originalDomain.getEmail());
            assertThat(resultDomain.getMemberId()).isEqualTo(originalDomain.getMemberId());
            assertThat(resultDomain.getPurpose()).isEqualTo(originalDomain.getPurpose());
            assertThat(resultDomain.getStatus()).isEqualTo(originalDomain.getStatus());
        }
    }

    @Nested
    @DisplayName("Mapping des enums")
    class EnumMappingTests {

        @Test
        @DisplayName("Devrait mapper correctement TokenPurpose.PASSWORD_SETUP")
        void shouldMapPasswordSetupPurpose() {
            SetupTokenEntity entity = createValidEntity();
            entity.setPurpose(SetupToken.TokenPurpose.PASSWORD_SETUP);

            SetupToken domain = mapper.toDomain(entity);

            assertThat(domain.getPurpose()).isEqualTo(SetupToken.TokenPurpose.PASSWORD_SETUP);
        }

        @Test
        @DisplayName("Devrait mapper correctement TokenStatus.ISSUED")
        void shouldMapIssuedStatus() {
            SetupTokenEntity entity = createValidEntity();
            entity.setStatus(SetupToken.TokenStatus.ISSUED);

            SetupToken domain = mapper.toDomain(entity);

            assertThat(domain.getStatus()).isEqualTo(SetupToken.TokenStatus.ISSUED);
        }



        @Test
        @DisplayName("Devrait mapper correctement TokenStatus.REVOKED")
        void shouldMapRevokedStatus() {
            SetupTokenEntity entity = createValidEntity();
            entity.setStatus(SetupToken.TokenStatus.REVOKED);

            SetupToken domain = mapper.toDomain(entity);

            assertThat(domain.getStatus()).isEqualTo(SetupToken.TokenStatus.REVOKED);
        }
    }

    // Helper methods
    private SetupTokenEntity createValidEntity() {
        SetupTokenEntity entity = new SetupTokenEntity();
        entity.setId(UUID.randomUUID());
        entity.setTokenHash("hashed-token");
        entity.setEmail("test@example.com");
        entity.setMemberId(UUID.randomUUID());
        entity.setPurpose(SetupToken.TokenPurpose.PASSWORD_SETUP);
        entity.setStatus(SetupToken.TokenStatus.ISSUED);
        entity.setExpiresAt(LocalDateTime.now().plusHours(24));
        entity.setCreatedAt(LocalDateTime.now());
        return entity;
    }

    private SetupToken createValidDomain() {
        return SetupToken.builder()
                .id(UUID.randomUUID())
                .tokenHash("hashed-token")
                .email("test@example.com")
                .memberId(UUID.randomUUID())
                .purpose(SetupToken.TokenPurpose.PASSWORD_SETUP)
                .status(SetupToken.TokenStatus.ISSUED)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .createdAt(LocalDateTime.now())
                .build();
    }
}
