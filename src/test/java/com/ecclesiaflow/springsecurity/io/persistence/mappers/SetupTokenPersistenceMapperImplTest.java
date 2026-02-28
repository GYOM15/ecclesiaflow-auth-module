package com.ecclesiaflow.springsecurity.io.persistence.mappers;

import com.ecclesiaflow.springsecurity.business.domain.token.SetupToken;
import com.ecclesiaflow.springsecurity.io.persistence.jpa.SetupTokenEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SetupTokenPersistenceMapperImpl - Tests Unitaires Générés")
class SetupTokenPersistenceMapperImplTest {

    private SetupTokenPersistenceMapperImpl mapper;

    @BeforeEach
    void setUp() {
        mapper = new SetupTokenPersistenceMapperImpl();
    }

    @Nested
    @DisplayName("toDomain - Tests de conversion Entity vers Domain")
    class ToDomainTests {

        @Test
        @DisplayName("Devrait retourner null quand entity est null")
        void shouldReturnNullWhenEntityIsNull() {
            SetupToken result = mapper.toDomain(null);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Devrait convertir entity complète vers domain")
        void shouldConvertCompleteEntityToDomain() {
            SetupTokenEntity entity = SetupTokenEntity.builder()
                    .id(UUID.randomUUID())
                    .tokenHash("hash123")
                    .email("test@example.com")
                    .memberId(UUID.randomUUID())
                    .purpose(SetupToken.TokenPurpose.PASSWORD_SETUP)
                    .status(SetupToken.TokenStatus.ISSUED)
                    .expiresAt(LocalDateTime.now().plusHours(24))
                    .createdAt(LocalDateTime.now())
                    .build();

            SetupToken result = mapper.toDomain(entity);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(entity.getId());
            assertThat(result.getTokenHash()).isEqualTo(entity.getTokenHash());
            assertThat(result.getEmail()).isEqualTo(entity.getEmail());
            assertThat(result.getMemberId()).isEqualTo(entity.getMemberId());
            assertThat(result.getPurpose()).isEqualTo(SetupToken.TokenPurpose.PASSWORD_SETUP);
            assertThat(result.getStatus()).isEqualTo(SetupToken.TokenStatus.ISSUED);
            assertThat(result.getExpiresAt()).isEqualTo(entity.getExpiresAt());
            assertThat(result.getCreatedAt()).isEqualTo(entity.getCreatedAt());
        }

        @Test
        @DisplayName("Devrait convertir tous les statuts entity vers domain")
        void shouldConvertAllEntityStatusesToDomain() {
            SetupTokenEntity entityIssued = createEntityWithStatus(SetupToken.TokenStatus.ISSUED);
            SetupTokenEntity entityExpired = createEntityWithStatus(SetupToken.TokenStatus.EXPIRED);
            SetupTokenEntity entityRevoked = createEntityWithStatus(SetupToken.TokenStatus.REVOKED);

            assertThat(mapper.toDomain(entityIssued).getStatus()).isEqualTo(SetupToken.TokenStatus.ISSUED);
            assertThat(mapper.toDomain(entityExpired).getStatus()).isEqualTo(SetupToken.TokenStatus.EXPIRED);
            assertThat(mapper.toDomain(entityRevoked).getStatus()).isEqualTo(SetupToken.TokenStatus.REVOKED);
        }

        @Test
        @DisplayName("Devrait gérer purpose null dans entity")
        void shouldHandleNullPurposeInEntity() {
            SetupTokenEntity entity = SetupTokenEntity.builder()
                    .id(UUID.randomUUID())
                    .tokenHash("hash")
                    .email("test@example.com")
                    .memberId(UUID.randomUUID())
                    .purpose(null)
                    .status(SetupToken.TokenStatus.ISSUED)
                    .expiresAt(LocalDateTime.now().plusHours(24))
                    .createdAt(LocalDateTime.now())
                    .build();

            SetupToken result = mapper.toDomain(entity);

            assertThat(result.getPurpose()).isNull();
        }

        @Test
        @DisplayName("Devrait gérer status null dans entity")
        void shouldHandleNullStatusInEntity() {
            SetupTokenEntity entity = SetupTokenEntity.builder()
                    .id(UUID.randomUUID())
                    .tokenHash("hash")
                    .email("test@example.com")
                    .memberId(UUID.randomUUID())
                    .purpose(SetupToken.TokenPurpose.PASSWORD_SETUP)
                    .status(null)
                    .expiresAt(LocalDateTime.now().plusHours(24))
                    .createdAt(LocalDateTime.now())
                    .build();

            SetupToken result = mapper.toDomain(entity);

            assertThat(result.getStatus()).isNull();
        }
    }

    @Nested
    @DisplayName("toEntity - Tests de conversion Domain vers Entity")
    class ToEntityTests {

        @Test
        @DisplayName("Devrait retourner null quand domain est null")
        void shouldReturnNullWhenDomainIsNull() {
            SetupTokenEntity result = mapper.toEntity(null);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Devrait convertir domain complet vers entity")
        void shouldConvertCompleteDomainToEntity() {
            SetupToken domain = SetupToken.builder()
                    .id(UUID.randomUUID())
                    .tokenHash("hash123")
                    .email("test@example.com")
                    .memberId(UUID.randomUUID())
                    .purpose(SetupToken.TokenPurpose.PASSWORD_SETUP)
                    .status(SetupToken.TokenStatus.ISSUED)
                    .expiresAt(LocalDateTime.now().plusHours(24))
                    .createdAt(LocalDateTime.now())
                    .build();

            SetupTokenEntity result = mapper.toEntity(domain);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(domain.getId());
            assertThat(result.getTokenHash()).isEqualTo(domain.getTokenHash());
            assertThat(result.getEmail()).isEqualTo(domain.getEmail());
            assertThat(result.getMemberId()).isEqualTo(domain.getMemberId());
            assertThat(result.getPurpose()).isEqualTo(SetupToken.TokenPurpose.PASSWORD_SETUP);
            assertThat(result.getStatus()).isEqualTo(SetupToken.TokenStatus.ISSUED);
            assertThat(result.getExpiresAt()).isEqualTo(domain.getExpiresAt());
            assertThat(result.getCreatedAt()).isEqualTo(domain.getCreatedAt());
        }

        @Test
        @DisplayName("Devrait convertir tous les statuts domain vers entity")
        void shouldConvertAllDomainStatusesToEntity() {
            SetupToken domainIssued = createDomainWithStatus(SetupToken.TokenStatus.ISSUED);
            SetupToken domainExpired = createDomainWithStatus(SetupToken.TokenStatus.EXPIRED);
            SetupToken domainRevoked = createDomainWithStatus(SetupToken.TokenStatus.REVOKED);

            assertThat(mapper.toEntity(domainIssued).getStatus()).isEqualTo(SetupToken.TokenStatus.ISSUED);
            assertThat(mapper.toEntity(domainExpired).getStatus()).isEqualTo(SetupToken.TokenStatus.EXPIRED);
            assertThat(mapper.toEntity(domainRevoked).getStatus()).isEqualTo(SetupToken.TokenStatus.REVOKED);
        }

        @Test
        @DisplayName("Devrait gérer purpose null dans domain")
        void shouldHandleNullPurposeInDomain() {
            SetupToken domain = SetupToken.builder()
                    .id(UUID.randomUUID())
                    .tokenHash("hash")
                    .email("test@example.com")
                    .memberId(UUID.randomUUID())
                    .purpose(null)
                    .status(SetupToken.TokenStatus.ISSUED)
                    .expiresAt(LocalDateTime.now().plusHours(24))
                    .createdAt(LocalDateTime.now())
                    .build();

            SetupTokenEntity result = mapper.toEntity(domain);

            assertThat(result.getPurpose()).isNull();
        }

        @Test
        @DisplayName("Devrait gérer status null dans domain")
        void shouldHandleNullStatusInDomain() {
            SetupToken domain = SetupToken.builder()
                    .id(UUID.randomUUID())
                    .tokenHash("hash")
                    .email("test@example.com")
                    .memberId(UUID.randomUUID())
                    .purpose(SetupToken.TokenPurpose.PASSWORD_SETUP)
                    .status(null)
                    .expiresAt(LocalDateTime.now().plusHours(24))
                    .createdAt(LocalDateTime.now())
                    .build();

            SetupTokenEntity result = mapper.toEntity(domain);

            assertThat(result.getStatus()).isNull();
        }
    }

    @Nested
    @DisplayName("Bidirectionnalité - Tests de conversion aller-retour")
    class BidirectionalityTests {

        @Test
        @DisplayName("Devrait maintenir les données lors d'une conversion entity->domain->entity")
        void shouldMaintainDataDuringEntityToDomainToEntity() {
            SetupTokenEntity original = SetupTokenEntity.builder()
                    .id(UUID.randomUUID())
                    .tokenHash("hash123")
                    .email("test@example.com")
                    .memberId(UUID.randomUUID())
                    .purpose(SetupToken.TokenPurpose.PASSWORD_SETUP)
                    .status(SetupToken.TokenStatus.ISSUED)
                    .expiresAt(LocalDateTime.now().plusHours(24))
                    .createdAt(LocalDateTime.now())
                    .build();

            SetupToken domain = mapper.toDomain(original);
            SetupTokenEntity result = mapper.toEntity(domain);

            assertThat(result.getId()).isEqualTo(original.getId());
            assertThat(result.getTokenHash()).isEqualTo(original.getTokenHash());
            assertThat(result.getEmail()).isEqualTo(original.getEmail());
            assertThat(result.getMemberId()).isEqualTo(original.getMemberId());
            assertThat(result.getPurpose()).isEqualTo(original.getPurpose());
            assertThat(result.getStatus()).isEqualTo(original.getStatus());
            assertThat(result.getExpiresAt()).isEqualTo(original.getExpiresAt());
            assertThat(result.getCreatedAt()).isEqualTo(original.getCreatedAt());
        }

        @Test
        @DisplayName("Devrait maintenir les données lors d'une conversion domain->entity->domain")
        void shouldMaintainDataDuringDomainToEntityToDomain() {
            SetupToken original = SetupToken.builder()
                    .id(UUID.randomUUID())
                    .tokenHash("hash123")
                    .email("test@example.com")
                    .memberId(UUID.randomUUID())
                    .purpose(SetupToken.TokenPurpose.PASSWORD_SETUP)
                    .status(SetupToken.TokenStatus.ISSUED)
                    .expiresAt(LocalDateTime.now().plusHours(24))
                    .createdAt(LocalDateTime.now())
                    .build();

            SetupTokenEntity entity = mapper.toEntity(original);
            SetupToken result = mapper.toDomain(entity);

            assertThat(result.getId()).isEqualTo(original.getId());
            assertThat(result.getTokenHash()).isEqualTo(original.getTokenHash());
            assertThat(result.getEmail()).isEqualTo(original.getEmail());
            assertThat(result.getMemberId()).isEqualTo(original.getMemberId());
            assertThat(result.getPurpose()).isEqualTo(original.getPurpose());
            assertThat(result.getStatus()).isEqualTo(original.getStatus());
            assertThat(result.getExpiresAt()).isEqualTo(original.getExpiresAt());
            assertThat(result.getCreatedAt()).isEqualTo(original.getCreatedAt());
        }
    }

    // Helper methods
    private SetupTokenEntity createEntityWithStatus(SetupToken.TokenStatus status) {
        return SetupTokenEntity.builder()
                .id(UUID.randomUUID())
                .tokenHash("hash")
                .email("test@example.com")
                .memberId(UUID.randomUUID())
                .purpose(SetupToken.TokenPurpose.PASSWORD_SETUP)
                .status(status)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .createdAt(LocalDateTime.now())
                .build();
    }

    private SetupToken createDomainWithStatus(SetupToken.TokenStatus status) {
        return SetupToken.builder()
                .id(UUID.randomUUID())
                .tokenHash("hash")
                .email("test@example.com")
                .memberId(UUID.randomUUID())
                .purpose(SetupToken.TokenPurpose.PASSWORD_SETUP)
                .status(status)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .createdAt(LocalDateTime.now())
                .build();
    }
}
