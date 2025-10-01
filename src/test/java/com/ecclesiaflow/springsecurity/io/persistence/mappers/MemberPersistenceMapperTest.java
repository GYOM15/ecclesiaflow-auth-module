package com.ecclesiaflow.springsecurity.io.persistence.mappers;

import com.ecclesiaflow.springsecurity.business.domain.member.Member;
import com.ecclesiaflow.springsecurity.business.domain.member.Role;
import com.ecclesiaflow.springsecurity.io.persistence.jpa.MemberEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MemberPersistenceMapper - Tests Unitaires")
class MemberPersistenceMapperTest {

    private MemberPersistenceMapper mapper;
    private LocalDateTime fixedTime;
    private MemberEntity fullEntity;
    private Member fullDomain;

    @BeforeEach
    void setUp() {
        mapper = new MemberPersistenceMapper();
        fixedTime = LocalDateTime.of(2023, 10, 26, 10, 0);

        fullEntity = MemberEntity.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .role(Role.ADMIN)
                .password("$2a$10$encodedpassword")
                .createdAt(fixedTime)
                .updatedAt(fixedTime.plusDays(1))
                .enabled(true)
                .build();

        fullDomain = Member.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .role(Role.ADMIN)
                .password("$2a$10$encodedpassword")
                .createdAt(fixedTime)
                .updatedAt(fixedTime.plusDays(1))
                .enabled(true)
                .build();
    }

    // ====================================================================
    // Tests de conversion TO DOMAIN (Entity -> Domain)
    // ====================================================================

    @Nested
    @DisplayName("toDomain")
    class ToDomainTests {
        @Test
        @DisplayName("Devrait mapper une entité complète vers un objet domaine complet")
        void shouldMapFullEntityToFullDomain() {
            // Act
            Member domain = mapper.toDomain(fullEntity);

            // Assert
            assertThat(domain).isNotNull();
            assertThat(domain.getId()).isEqualTo(fullEntity.getId());
            assertThat(domain.getEmail()).isEqualTo(fullEntity.getEmail());
            assertThat(domain.getRole()).isEqualTo(fullEntity.getRole());
            assertThat(domain.getPassword()).isEqualTo(fullEntity.getPassword());
            assertThat(domain.getCreatedAt()).isEqualTo(fullEntity.getCreatedAt());
            assertThat(domain.getUpdatedAt()).isEqualTo(fullEntity.getUpdatedAt());
            assertThat(domain.isEnabled()).isEqualTo(fullEntity.isEnabled());
        }

        @Test
        @DisplayName("Devrait gérer les valeurs nulles pour les champs optionnels (hormis primitive enabled)")
        void shouldHandleNullValues() {
            // Arrange
            MemberEntity entityWithNulls = MemberEntity.builder()
                    .id(null)
                    .email("null@test.com")
                    .role(Role.MEMBER)
                    .password(null)
                    .createdAt(null)
                    .updatedAt(null)
                    .enabled(false)
                    .build();

            // Act
            Member domain = mapper.toDomain(entityWithNulls);

            // Assert
            assertThat(domain).isNotNull();
            assertThat(domain.getId()).isNull();
            assertThat(domain.getPassword()).isNull();
            assertThat(domain.getCreatedAt()).isNull();
            assertThat(domain.getUpdatedAt()).isNull();
            assertThat(domain.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("Devrait retourner null si l'entité fournie est null")
        void shouldReturnNullIfEntityIsNull() {
            // Act
            Member domain = mapper.toDomain(null);

            // Assert
            assertThat(domain).isNull();
        }
    }

    // ====================================================================
    // Tests de conversion TO ENTITY (Domain -> Entity)
    // ====================================================================

    @Nested
    @DisplayName("toEntity")
    class ToEntityTests {
        @Test
        @DisplayName("Devrait mapper un objet domaine complet vers une entité complète")
        void shouldMapFullDomainToFullEntity() {
            // Act
            MemberEntity entity = mapper.toEntity(fullDomain);

            // Assert
            assertThat(entity).isNotNull();
            assertThat(entity.getId()).isEqualTo(fullDomain.getId());
            assertThat(entity.getEmail()).isEqualTo(fullDomain.getEmail());
            assertThat(entity.getRole()).isEqualTo(fullDomain.getRole());
            assertThat(entity.getPassword()).isEqualTo(fullDomain.getPassword());
            assertThat(entity.getCreatedAt()).isEqualTo(fullDomain.getCreatedAt());
            assertThat(entity.getUpdatedAt()).isEqualTo(fullDomain.getUpdatedAt());
            assertThat(entity.isEnabled()).isEqualTo(fullDomain.isEnabled());
        }

        @Test
        @DisplayName("Devrait gérer les valeurs nulles pour les champs optionnels")
        void shouldHandleNullValues() {
            // Arrange
            Member domainWithNulls = Member.builder()
                    .id(null)
                    .email("null@test.com")
                    .role(Role.MEMBER)
                    .password(null)
                    .createdAt(null)
                    .updatedAt(null)
                    .enabled(false)
                    .build();

            // Act
            MemberEntity entity = mapper.toEntity(domainWithNulls);

            // Assert
            assertThat(entity).isNotNull();
            assertThat(entity.getId()).isNull();
            assertThat(entity.getPassword()).isNull();
            assertThat(entity.getCreatedAt()).isNull();
            assertThat(entity.getUpdatedAt()).isNull();
            assertThat(entity.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("Devrait lever IllegalArgumentException si l'objet domaine est null")
        void shouldThrowExceptionIfDomainIsNull() {
            // Act & Assert
            assertThatThrownBy(() -> mapper.toEntity(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("L'objet domaine ne peut pas être null");
        }
    }
}