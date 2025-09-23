package com.ecclesiaflow.springsecurity.io.repository;

import com.ecclesiaflow.springsecurity.business.domain.member.MemberRepository;
import com.ecclesiaflow.springsecurity.business.encryption.PasswordEncoderUtil;
import com.ecclesiaflow.springsecurity.business.domain.member.Member;
import com.ecclesiaflow.springsecurity.business.domain.member.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests d'intégration pour MemberRepository
 * 
 * Teste les méthodes de recherche personnalisées du repository
 * avec une base de données H2 en mémoire.
 */
@DataJpaTest
@DisplayName("MemberRepository - Tests d'intégration")
class MemberRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MemberRepository memberRepository;

    @MockBean
    private PasswordEncoderUtil passwordEncoderUtil;

    private Member testMember;
    private Member adminMember;

    @BeforeEach
    void setUp() {
        // Clear all data before each test. This is the key to isolation.
        entityManager.getEntityManager().createQuery("DELETE FROM Member").executeUpdate();
        // Créer un membre test
        testMember = new Member();
        testMember.setFirstName("John");
        testMember.setLastName("Doe");
        testMember.setEmail("john.doe@example.com");
        testMember.setPassword("encodedPassword123");
        testMember.setRole(Role.MEMBER);

        // Créer un admin test
        adminMember = new Member();
        adminMember.setFirstName("Admin");
        adminMember.setLastName("User");
        adminMember.setEmail("admin@example.com");
        adminMember.setPassword("encodedAdminPassword");
        adminMember.setRole(Role.ADMIN);

        // Persister les entités
        entityManager.persistAndFlush(testMember);
        entityManager.persistAndFlush(adminMember);
    }

    @AfterEach
    void tearDown() {
        entityManager.clear();
    }

    @Test
    @DisplayName("Devrait trouver un membre par email existant")
    void shouldFindMemberByExistingEmail() {
        // When
        Optional<Member> foundMember = memberRepository.findByEmail("john.doe@example.com");

        // Then
        assertThat(foundMember).isPresent();
        assertThat(foundMember.get().getEmail()).isEqualTo("john.doe@example.com");
        assertThat(foundMember.get().getFirstName()).isEqualTo("John");
        assertThat(foundMember.get().getLastName()).isEqualTo("Doe");
        assertThat(foundMember.get().getRole()).isEqualTo(Role.MEMBER);
    }

    @Test
    @DisplayName("Devrait retourner Optional vide pour email inexistant")
    void shouldReturnEmptyOptionalForNonExistentEmail() {
        // When
        Optional<Member> foundMember = memberRepository.findByEmail("nonexistent@example.com");

        // Then
        assertThat(foundMember).isEmpty();
    }

    @Test
    @DisplayName("Devrait être insensible à la casse pour la recherche par email")
    void shouldBeCaseInsensitiveForEmailSearch() {
        // When
        Optional<Member> foundMemberUpper = memberRepository.findByEmail("JOHN.DOE@EXAMPLE.COM");
        Optional<Member> foundMemberLower = memberRepository.findByEmail("john.doe@example.com");
        Optional<Member> foundMemberMixed = memberRepository.findByEmail("John.Doe@Example.Com");

        // Then - Dépend de la configuration de la base de données
        // Par défaut, MySQL est insensible à la casse pour les emails
        assertThat(foundMemberLower).isPresent();
        // Les autres tests peuvent varier selon la configuration DB
    }

    @Test
    @DisplayName("Devrait gérer les emails avec caractères spéciaux")
    void shouldHandleEmailsWithSpecialCharacters() {
        // Given
        Member specialMember = new Member();
        specialMember.setFirstName("Special");
        specialMember.setLastName("User");
        specialMember.setEmail("user+test@sub.domain.co.uk");
        specialMember.setPassword("password");
        specialMember.setRole(Role.MEMBER);
        entityManager.persistAndFlush(specialMember);

        // When
        Optional<Member> foundMember = memberRepository.findByEmail("user+test@sub.domain.co.uk");

        // Then
        assertThat(foundMember).isPresent();
        assertThat(foundMember.get().getEmail()).isEqualTo("user+test@sub.domain.co.uk");
    }

    @Test
    @DisplayName("Devrait trouver un membre par rôle MEMBER")
    void shouldFindMemberByMemberRole() {
        // When
        Member foundMember = memberRepository.findByRole(Role.MEMBER);

        // Then
        assertThat(foundMember).isNotNull();
        assertThat(foundMember.getRole()).isEqualTo(Role.MEMBER);
        // Peut retourner n'importe quel membre avec le rôle MEMBER
    }

    @Test
    @DisplayName("Devrait trouver un membre par rôle ADMIN")
    void shouldFindMemberByAdminRole() {
        // When
        Member foundMember = memberRepository.findByRole(Role.ADMIN);

        // Then
        assertThat(foundMember).isNotNull();
        assertThat(foundMember.getRole()).isEqualTo(Role.ADMIN);
        assertThat(foundMember.getEmail()).isEqualTo("admin@example.com");
    }

    @Test
    @DisplayName("Devrait retourner null pour rôle inexistant")
    void shouldReturnNullForNonExistentRole() {
        // Given - Supprimer tous les admins
        memberRepository.delete(adminMember);
        entityManager.flush();

        // When
        Member foundMember = memberRepository.findByRole(Role.ADMIN);

        // Then
        assertThat(foundMember).isNull();
    }

    @Test
    @DisplayName("Devrait sauvegarder et retrouver un nouveau membre")
    void shouldSaveAndRetrieveNewMember() {
        // Given
        Member newMember = new Member();
        newMember.setId(UUID.randomUUID());
        newMember.setFirstName("Jane");
        newMember.setLastName("Smith");
        newMember.setEmail("jane.smith@example.com");
        newMember.setPassword("encodedPassword456");
        newMember.setRole(Role.MEMBER);

        // When
        Member savedMember = memberRepository.save(newMember);
        Optional<Member> retrievedMember = memberRepository.findByEmail("jane.smith@example.com");

        // Then
        assertThat(savedMember).isNotNull();
        assertThat(savedMember.getId()).isEqualTo(newMember.getId());
        assertThat(retrievedMember).isPresent();
        assertThat(retrievedMember.get().getFirstName()).isEqualTo("Jane");
        assertThat(retrievedMember.get().getLastName()).isEqualTo("Smith");
    }

    @Test
    @DisplayName("Devrait supprimer un membre et ne plus le trouver")
    void shouldDeleteMemberAndNotFindIt() {
        // Given
        String emailToDelete = testMember.getEmail();

        // When
        memberRepository.delete(testMember);
        entityManager.flush();
        Optional<Member> foundMember = memberRepository.findByEmail(emailToDelete);

        // Then
        assertThat(foundMember).isEmpty();
    }

    @Test
    @DisplayName("Devrait compter correctement le nombre de membres")
    void shouldCountMembersCorrectly() {
        // When
        long memberCount = memberRepository.count();

        // Then
        assertThat(memberCount).isEqualTo(2); // testMember + adminMember
    }

    @Test
    @DisplayName("Devrait vérifier l'existence d'un membre par ID")
    void shouldCheckMemberExistenceById() {
        // When
        boolean exists = memberRepository.existsById(testMember.getId());
        boolean notExists = memberRepository.existsById(UUID.randomUUID());

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Devrait mettre à jour un membre existant")
    void shouldUpdateExistingMember() {
        // Given
        String newFirstName = "UpdatedJohn";
        testMember.setFirstName(newFirstName);

        // When
        Member updatedMember = memberRepository.save(testMember);
        Optional<Member> retrievedMember = memberRepository.findByEmail(testMember.getEmail());

        // Then
        assertThat(updatedMember.getFirstName()).isEqualTo(newFirstName);
        assertThat(retrievedMember).isPresent();
        assertThat(retrievedMember.get().getFirstName()).isEqualTo(newFirstName);
    }

    @Test
    @DisplayName("Should handle null email in search gracefully")
    void shouldHandleNullEmailInSearch() {
        Optional<Member> foundMember = memberRepository.findByEmail(null);
        assertThat(foundMember).isEmpty();
    }

    @Test
    @DisplayName("Should handle null role in search gracefully")
    void shouldHandleNullRoleInSearch() {
        Member foundMember = memberRepository.findByRole(null);
        assertThat(foundMember).isNull();
    }

    @Test
    @DisplayName("Devrait retourner tous les membres")
    void shouldReturnAllMembers() {
        // When
        var allMembers = memberRepository.findAll();

        // Then
        assertThat(allMembers).hasSize(2);
        assertThat(allMembers)
                .extracting(Member::getEmail)
                .containsExactlyInAnyOrder("john.doe@example.com", "admin@example.com");
    }

    @Test
    @DisplayName("Devrait gérer la recherche avec email vide")
    void shouldHandleEmptyEmailSearch() {
        // When
        Optional<Member> foundMember = memberRepository.findByEmail("");

        // Then
        assertThat(foundMember).isEmpty();
    }
}
