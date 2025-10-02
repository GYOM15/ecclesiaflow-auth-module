package com.ecclesiaflow.springsecurity.io.persistence.repositories.impl;

import com.ecclesiaflow.springsecurity.business.domain.member.Member;
import com.ecclesiaflow.springsecurity.business.domain.member.Role;
import com.ecclesiaflow.springsecurity.io.persistence.jpa.MemberEntity;
import com.ecclesiaflow.springsecurity.io.persistence.jpa.SpringDataMemberRepository;
import com.ecclesiaflow.springsecurity.io.persistence.mappers.MemberPersistenceMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberRepositoryImpl - Tests d'Adaptation")
class MemberRepositoryImplTest {

    @Mock
    private SpringDataMemberRepository springDataRepository;
    @Mock
    private MemberPersistenceMapper mapper;

    @InjectMocks
    private MemberRepositoryImpl memberRepository;

    private MemberEntity mockEntity;
    private Member mockDomain;
    private static final String EMAIL = "test@domain.com";
    private static final UUID ID = UUID.randomUUID();
    private static final LocalDateTime NOW = LocalDateTime.now().withNano(0);

    @BeforeEach
    void setUp() {
        mockEntity = MemberEntity.builder()
                .id(ID)
                .email(EMAIL)
                .role(Role.MEMBER)
                .enabled(true)
                .password("pwd")
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();

        mockDomain = Member.builder()
                .id(ID)
                .email(EMAIL)
                .role(Role.MEMBER)
                .enabled(true)
                .password("pwd")
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();

        // Configurer le mapper pour qu'il retourne les mocks de manière cohérente
        lenient().when(mapper.toDomain(mockEntity)).thenReturn(mockDomain);
        lenient().when(mapper.toEntity(mockDomain)).thenReturn(mockEntity);
    }

    // ====================================================================
    // Tests findByEmail
    // ====================================================================

    @Test
    @DisplayName("findByEmail - Devrait retourner Optional.of(Member) si trouvé")
    void getByEmail_ShouldReturnMember_WhenFound() {
        // Arrange
        when(springDataRepository.findByEmail(EMAIL)).thenReturn(Optional.of(mockEntity));

        // Act
        Optional<Member> result = memberRepository.getByEmail(EMAIL);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(mockDomain);
        verify(springDataRepository).findByEmail(EMAIL);
        verify(mapper).toDomain(mockEntity);
    }

    @Test
    @DisplayName("findByEmail - Devrait retourner Optional.empty() si non trouvé")
    void getByEmail_ShouldReturnEmpty_WhenNotFound() {
        // Arrange
        when(springDataRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        // Act
        Optional<Member> result = memberRepository.getByEmail(EMAIL);

        // Assert
        assertThat(result).isEmpty();
        verify(springDataRepository).findByEmail(EMAIL);
        verifyNoMoreInteractions(mapper);
    }

    // ====================================================================
    // Tests findByRole
    // ====================================================================

    @Test
    @DisplayName("findByRole - Devrait retourner Member si une entité est trouvée")
    void getByRole_ShouldReturnMember_WhenEntityFound() {
        // Arrange
        Role role = Role.ADMIN;
        UUID adminId = UUID.randomUUID();
        LocalDateTime adminNow = LocalDateTime.now().withNano(0);

        MemberEntity adminEntity = MemberEntity.builder()
                .id(adminId)
                .email("admin@domain.com")
                .role(role)
                .enabled(true)
                .password("admin_pwd")
                .createdAt(adminNow)
                .updatedAt(adminNow)
                .build();

        Member adminDomain = Member.builder()
                .id(adminId)
                .email("admin@domain.com")
                .role(role)
                .enabled(true)
                .password("admin_pwd")
                .createdAt(adminNow)
                .updatedAt(adminNow)
                .build();

        when(springDataRepository.findByRole(role)).thenReturn(adminEntity);
        when(mapper.toDomain(adminEntity)).thenReturn(adminDomain);

        // Act
        Member result = memberRepository.getByRole(role);

        // Assert
        assertThat(result).isEqualTo(adminDomain);
        verify(springDataRepository).findByRole(role);
        verify(mapper).toDomain(adminEntity);
    }

    @Test
    @DisplayName("findByRole - Devrait retourner null si aucune entité n'est trouvée")
    void getByRole_ShouldReturnNull_WhenEntityNotFound() {
        // Arrange
        Role role = Role.ADMIN;
        when(springDataRepository.findByRole(role)).thenReturn(null);

        when(mapper.toDomain(null)).thenReturn(null);

        // Act
        Member result = memberRepository.getByRole(role);

        // Assert
        assertThat(result).isNull();
        verify(springDataRepository).findByRole(role);
        verify(mapper).toDomain(null);
    }

    // ====================================================================
    // Tests save
    // ====================================================================

    @Test
    @DisplayName("save - Devrait mapper -> sauvegarder -> mapper et retourner l'objet domaine")
    void save_ShouldPerformFullCycle() {
        // Arrange: Simuler la création/mise à jour d'un ID par la BDD
        UUID savedId = UUID.randomUUID();

        // Simuler la mise à jour de la date (1 heure plus tard)
        LocalDateTime updatedTime = mockEntity.getUpdatedAt().plusHours(1);

        // 1. Création de l'Entité sauvegardée (nouvelle instance, pas de toBuilder)
        MemberEntity savedEntity = MemberEntity.builder()
                .id(savedId) // ID mis à jour par la persistance
                .email(mockEntity.getEmail())
                .role(mockEntity.getRole())
                .enabled(mockEntity.isEnabled())
                .password(mockEntity.getPassword())
                // Copier les autres champs importants (dates)
                .createdAt(mockEntity.getCreatedAt())
                .updatedAt(updatedTime)
                .build();

        // 2. Création de l'Objet Domaine sauvegardé (utilise toBuilder)
        Member savedDomain = mockDomain.toBuilder()
                .id(savedId)
                .updatedAt(updatedTime)
                .build();

        // Mocks pour la séquence
        when(springDataRepository.save(mockEntity)).thenReturn(savedEntity);
        when(mapper.toDomain(savedEntity)).thenReturn(savedDomain);

        // Act
        Member result = memberRepository.save(mockDomain);

        // Assert
        assertThat(result).isEqualTo(savedDomain);
        verify(mapper).toEntity(mockDomain);
        verify(springDataRepository).save(mockEntity);
        verify(mapper).toDomain(savedEntity);
    }

    // ====================================================================
    // Tests delete
    // ====================================================================

    @Test
    @DisplayName("delete - Devrait mapper -> supprimer l'entité")
    void delete_ShouldDeleteEntity() {
        // Act
        memberRepository.delete(mockDomain);

        // Assert
        verify(mapper).toEntity(mockDomain);
        verify(springDataRepository).delete(mockEntity);
        verifyNoMoreInteractions(springDataRepository, mapper);
    }
}