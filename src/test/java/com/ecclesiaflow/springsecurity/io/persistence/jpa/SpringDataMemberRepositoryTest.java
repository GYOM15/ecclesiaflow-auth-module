package com.ecclesiaflow.springsecurity.io.persistence.jpa;

import com.ecclesiaflow.springsecurity.business.domain.member.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;


import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("SpringDataMemberRepository")
class SpringDataMemberRepositoryTest {

    @Autowired
    private SpringDataMemberRepository memberRepository;

    @Autowired
    private TestEntityManager entityManager;

    private MemberEntity member2;

    @BeforeEach
    void setUp() {
        // Nettoyer et configurer les entités de base pour les tests
        entityManager.clear();

        MemberEntity member1 = MemberEntity.builder()
                .email("admin@example.com")
                .password("Admin123!")
                .role(Role.ADMIN)
                .createdAt(LocalDateTime.now())
                .build();

        member2 = MemberEntity.builder()
                .email("user1@example.com")
                .password("User123@")
                .role(Role.MEMBER)
                .createdAt(LocalDateTime.now())
                .build();

        // Membre supplémentaire avec un email différent pour tester la recherche par email
        // On garde le même rôle pour l'instant, on le modifiera dans les tests spécifiques
        MemberEntity member3 = MemberEntity.builder()
                .email("user2@example.com")
                .password("User456&")
                .role(Role.MEMBER)  // On garde le même rôle pour l'instant, on le modifiera dans les tests spécifiques
                .createdAt(LocalDateTime.now())
                .build();

        // Persister seulement les membres nécessaires pour la plupart des tests
        entityManager.persist(member1);  // ADMIN
        entityManager.persist(member2);  // MEMBER  
        // member3 sera persisté dans les tests spécifiques qui en ont besoin
        entityManager.flush(); // S'assurer que les données sont écrites
    }

    // ====================================================================
    // Tests pour findByEmail
    // ====================================================================

    @Test
    @DisplayName("Doit trouver un membre par email existant")
    void findByEmail_ShouldReturnMember_WhenExists() {
        // Act
        Optional<MemberEntity> found = memberRepository.findByEmail("admin@example.com");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("admin@example.com");
        assertThat(found.get().getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("Doit retourner Optional.empty() pour un email non existant")
    void findByEmail_ShouldReturnEmpty_WhenNotExists() {
        // Act
        Optional<MemberEntity> found = memberRepository.findByEmail("nonexistent@example.com");

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Doit gérer la recherche par email pour les champs sensibles (case-insensitivity, si configuré)")
    void findByEmail_ShouldHandleCaseSensitivity() {
        // Act
        Optional<MemberEntity> found = memberRepository.findByEmail("Admin@example.com");

        // Assert
        // On attend un échec de recherche car 'Admin' != 'admin' (case-sensitive)
        assertThat(found).isEmpty();
    }

    // ====================================================================
    // Tests pour findByRole
    // ====================================================================

    @Test
    @DisplayName("Doit trouver un membre ADMIN par rôle")
    void findByRole_ShouldReturnAdminMember() {
        // Act
        MemberEntity found = memberRepository.findByRole(Role.ADMIN);

        // Assert
        assertThat(found).isNotNull();
        assertThat(found.getEmail()).isEqualTo("admin@example.com");
        assertThat(found.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("Doit retourner le membre MEMBER pour le rôle MEMBER")
    void findByRole_ShouldReturnMemberUser() {
        // Act
        MemberEntity found = memberRepository.findByRole(Role.MEMBER);

        // Assert
        assertThat(found).isNotNull();
        assertThat(found.getRole()).isEqualTo(Role.MEMBER);
        assertThat(found.getEmail()).isEqualTo("user1@example.com");
    }

    @Test
    @DisplayName("Doit retourner null si aucun membre n'est trouvé pour un rôle donné")
    void findByRole_ShouldReturnNull_WhenNoMemberWithRoleExists() {
        // On supprime le membre avec le rôle MEMBER pour tester l'absence de résultats
        entityManager.remove(member2);
        entityManager.flush();

        // Act
        MemberEntity found = memberRepository.findByRole(Role.MEMBER);

        // Assert
        assertThat(found).isNull();
    }
}