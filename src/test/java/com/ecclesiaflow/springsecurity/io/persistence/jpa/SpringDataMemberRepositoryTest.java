package com.ecclesiaflow.springsecurity.io.persistence.jpa;

import com.ecclesiaflow.springsecurity.business.domain.member.Role;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;

@DataJpaTest
@DisplayName("SpringDataMemberRepository - Couverture complète")
class SpringDataMemberRepositoryTest {

    @Autowired
    private SpringDataMemberRepository memberRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Validator validator;
    private MemberEntity member2;

    @BeforeEach
    void setUp() {
        entityManager.clear();

        // Setup validator pour tests de validation
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

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

        entityManager.persist(member1);
        entityManager.persist(member2);
        entityManager.flush();
    }

    // ====================================================================
    // Tests Repository - findByEmail
    // ====================================================================

    @Test
    @DisplayName("findByEmail - Doit trouver un membre par email existant")
    void findByEmail_ShouldReturnMember_WhenExists() {
        Optional<MemberEntity> found = memberRepository.findByEmail("admin@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("admin@example.com");
        assertThat(found.get().getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("findByEmail - Doit retourner Optional.empty() pour email non existant")
    void findByEmail_ShouldReturnEmpty_WhenNotExists() {
        Optional<MemberEntity> found = memberRepository.findByEmail("nonexistent@example.com");
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("findByEmail - Doit être case-sensitive")
    void findByEmail_ShouldBeCaseSensitive() {
        Optional<MemberEntity> found = memberRepository.findByEmail("Admin@example.com");
        assertThat(found).isEmpty();
    }

    // ====================================================================
    // Tests Repository - findByRole
    // ====================================================================

    @Test
    @DisplayName("findByRole - Doit trouver un membre ADMIN par rôle")
    void findByRole_ShouldReturnAdminMember() {
        MemberEntity found = memberRepository.findByRole(Role.ADMIN);

        assertThat(found).isNotNull();
        assertThat(found.getEmail()).isEqualTo("admin@example.com");
        assertThat(found.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("findByRole - Doit retourner le membre MEMBER pour le rôle MEMBER")
    void findByRole_ShouldReturnMemberUser() {
        MemberEntity found = memberRepository.findByRole(Role.MEMBER);

        assertThat(found).isNotNull();
        assertThat(found.getRole()).isEqualTo(Role.MEMBER);
        assertThat(found.getEmail()).isEqualTo("user1@example.com");
    }

    @Test
    @DisplayName("findByRole - Doit retourner null si aucun membre trouvé")
    void findByRole_ShouldReturnNull_WhenNoMemberWithRoleExists() {
        entityManager.remove(member2);
        entityManager.flush();

        MemberEntity found = memberRepository.findByRole(Role.MEMBER);
        assertThat(found).isNull();
    }

    // ====================================================================
    // Tests Validation - @NotBlank email
    // ====================================================================

    @Test
    @DisplayName("Validation - Email null devrait échouer")
    void validation_EmailNull_ShouldFail() {
        MemberEntity entity = MemberEntity.builder()
                .email(null)
                .password("ValidPass123!")
                .role(Role.MEMBER)
                .build();

        Set<ConstraintViolation<MemberEntity>> violations = validator.validate(entity);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @Test
    @DisplayName("Validation - Email vide devrait échouer")
    void validation_EmailEmpty_ShouldFail() {
        MemberEntity entity = MemberEntity.builder()
                .email("")
                .password("ValidPass123!")
                .role(Role.MEMBER)
                .build();

        Set<ConstraintViolation<MemberEntity>> violations = validator.validate(entity);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @Test
    @DisplayName("Validation - Email avec espaces uniquement devrait échouer")
    void validation_EmailBlank_ShouldFail() {
        MemberEntity entity = MemberEntity.builder()
                .email("   ")
                .password("ValidPass123!")
                .role(Role.MEMBER)
                .build();

        Set<ConstraintViolation<MemberEntity>> violations = validator.validate(entity);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    // ====================================================================
    // Tests Validation - @NotBlank @Size @Pattern password
    // ====================================================================

    @Test
    @DisplayName("Validation - Password null devrait échouer")
    void validation_PasswordNull_ShouldFail() {
        MemberEntity entity = MemberEntity.builder()
                .email("test@example.com")
                .password(null)
                .role(Role.MEMBER)
                .build();

        Set<ConstraintViolation<MemberEntity>> violations = validator.validate(entity);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    @DisplayName("Validation - Password trop court devrait échouer")
    void validation_PasswordTooShort_ShouldFail() {
        MemberEntity entity = MemberEntity.builder()
                .email("test@example.com")
                .password("Short1!")
                .role(Role.MEMBER)
                .build();

        Set<ConstraintViolation<MemberEntity>> violations = validator.validate(entity);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("password") &&
                        v.getMessage().contains("au moins 8 caractères")
        );
    }

    @Test
    @DisplayName("Validation - Password sans majuscule devrait échouer")
    void validation_PasswordNoUppercase_ShouldFail() {
        MemberEntity entity = MemberEntity.builder()
                .email("test@example.com")
                .password("lowercase123!")
                .role(Role.MEMBER)
                .build();

        Set<ConstraintViolation<MemberEntity>> violations = validator.validate(entity);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    @DisplayName("Validation - Password sans minuscule devrait échouer")
    void validation_PasswordNoLowercase_ShouldFail() {
        MemberEntity entity = MemberEntity.builder()
                .email("test@example.com")
                .password("UPPERCASE123!")
                .role(Role.MEMBER)
                .build();

        Set<ConstraintViolation<MemberEntity>> violations = validator.validate(entity);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    @DisplayName("Validation - Password sans chiffre devrait échouer")
    void validation_PasswordNoDigit_ShouldFail() {
        MemberEntity entity = MemberEntity.builder()
                .email("test@example.com")
                .password("NoDigitPass!")
                .role(Role.MEMBER)
                .build();

        Set<ConstraintViolation<MemberEntity>> violations = validator.validate(entity);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    @DisplayName("Validation - Password sans caractère spécial devrait échouer")
    void validation_PasswordNoSpecialChar_ShouldFail() {
        MemberEntity entity = MemberEntity.builder()
                .email("test@example.com")
                .password("NoSpecial123")
                .role(Role.MEMBER)
                .build();

        Set<ConstraintViolation<MemberEntity>> violations = validator.validate(entity);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    @DisplayName("Validation - Password valide devrait réussir")
    void validation_ValidPassword_ShouldSucceed() {
        MemberEntity entity = MemberEntity.builder()
                .email("test@example.com")
                .password("ValidPass123!")
                .role(Role.MEMBER)
                .build();

        Set<ConstraintViolation<MemberEntity>> violations = validator.validate(entity);
        assertThat(violations).filteredOn(v -> v.getPropertyPath().toString().equals("password"))
                .isEmpty();
    }

    // ====================================================================
    // Tests Validation - @NotNull role
    // ====================================================================

    @Test
    @DisplayName("Validation - Role null devrait échouer")
    void validation_RoleNull_ShouldFail() {
        MemberEntity entity = MemberEntity.builder()
                .email("test@example.com")
                .password("ValidPass123!")
                .role(null)
                .build();

        Set<ConstraintViolation<MemberEntity>> violations = validator.validate(entity);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("role"));
    }

    // ====================================================================
    // Tests equals/hashCode avec timestamps
    // ====================================================================

    @Test
    @DisplayName("equals - Doit comparer createdAt")
    void equals_ShouldCompareCreatedAt() {
        LocalDateTime time1 = LocalDateTime.of(2024, 1, 1, 12, 0);
        LocalDateTime time2 = LocalDateTime.of(2024, 1, 2, 12, 0);

        MemberEntity entity1 = MemberEntity.builder()
                .email("test@example.com")
                .password("Pass123!")
                .role(Role.MEMBER)
                .enabled(true)
                .createdAt(time1)
                .build();

        MemberEntity entity2 = MemberEntity.builder()
                .email("test@example.com")
                .password("Pass123!")
                .role(Role.MEMBER)
                .enabled(true)
                .createdAt(time2)
                .build();

        assertThat(entity1).isNotEqualTo(entity2);
        assertThat(entity1.hashCode()).isNotEqualTo(entity2.hashCode());
    }

    @Test
    @DisplayName("equals - Doit comparer updatedAt")
    void equals_ShouldCompareUpdatedAt() {
        LocalDateTime time1 = LocalDateTime.of(2024, 1, 1, 12, 0);
        LocalDateTime time2 = LocalDateTime.of(2024, 1, 2, 12, 0);

        MemberEntity entity1 = MemberEntity.builder()
                .email("test@example.com")
                .password("Pass123!")
                .role(Role.MEMBER)
                .enabled(true)
                .updatedAt(time1)
                .build();

        MemberEntity entity2 = MemberEntity.builder()
                .email("test@example.com")
                .password("Pass123!")
                .role(Role.MEMBER)
                .enabled(true)
                .updatedAt(time2)
                .build();

        assertThat(entity1).isNotEqualTo(entity2);
        assertThat(entity1.hashCode()).isNotEqualTo(entity2.hashCode());
    }

    @Test
    @DisplayName("equals - Timestamps null vs non-null")
    void equals_TimestampsNullVsNonNull() {
        MemberEntity entityWithNullTimestamps = MemberEntity.builder()
                .email("test@example.com")
                .password("Pass123!")
                .role(Role.MEMBER)
                .createdAt(null)
                .updatedAt(null)
                .build();

        MemberEntity entityWithTimestamps = MemberEntity.builder()
                .email("test@example.com")
                .password("Pass123!")
                .role(Role.MEMBER)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        assertThat(entityWithNullTimestamps).isNotEqualTo(entityWithTimestamps);
    }

    // ====================================================================
    // Tests JPA - UUID Generation
    // ====================================================================

    @Test
    @DisplayName("JPA - Doit générer UUID automatiquement")
    void jpa_ShouldGenerateUuid() {
        MemberEntity entity = MemberEntity.builder()
                .email("uuid@example.com")
                .password("UuidPass123!")
                .role(Role.MEMBER)
                .build();

        assertThat(entity.getId()).isNull();

        MemberEntity saved = entityManager.persistAndFlush(entity);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getId()).isInstanceOf(UUID.class);
    }

    @Test
    @DisplayName("JPA - Doit permettre de définir l'UUID manuellement après création")
    void jpa_ShouldAllowManualUuidSetting() {
        MemberEntity entity = MemberEntity.builder()
                .email("manual-uuid@example.com")
                .password("ManualPass123!")
                .role(Role.MEMBER)
                .build();

        // Persister d'abord sans ID
        MemberEntity saved = entityManager.persistAndFlush(entity);
        UUID generatedId = saved.getId();
        
        // Vérifier que l'UUID a été généré automatiquement
        assertThat(generatedId).isNotNull();
        assertThat(generatedId).isInstanceOf(UUID.class);
        
        // Vérifier que l'entité peut être récupérée par son ID généré
        MemberEntity found = entityManager.find(MemberEntity.class, generatedId);
        assertThat(found).isNotNull();
        assertThat(found.getEmail()).isEqualTo("manual-uuid@example.com");
    }

    // ====================================================================
    // Tests JPA - Timestamps
    // ====================================================================

    @Test
    @DisplayName("JPA - @CreationTimestamp doit définir createdAt")
    void jpa_CreationTimestamp_ShouldSetCreatedAt() {
        MemberEntity entity = MemberEntity.builder()
                .email("timestamp@example.com")
                .password("TimestampPass123!")
                .role(Role.MEMBER)
                .build();

        assertThat(entity.getCreatedAt()).isNull();

        MemberEntity saved = entityManager.persistAndFlush(entity);

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getCreatedAt()).isBefore(LocalDateTime.now().plusSeconds(1));
    }

    @Test
    @DisplayName("JPA - @UpdateTimestamp doit définir updatedAt")
    void jpa_UpdateTimestamp_ShouldSetUpdatedAt() {
        MemberEntity entity = MemberEntity.builder()
                .email("update-timestamp@example.com")
                .password("UpdatePass123!")
                .role(Role.MEMBER)
                .build();

        assertThat(entity.getUpdatedAt()).isNull();

        MemberEntity saved = entityManager.persistAndFlush(entity);

        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isBefore(LocalDateTime.now().plusSeconds(1));
    }

    @Test
    @DisplayName("JPA - updatedAt doit être mis à jour lors d'une modification")
    void jpa_UpdateTimestamp_ShouldUpdateOnModification() throws InterruptedException {
        MemberEntity entity = MemberEntity.builder()
                .email("update-test@example.com")
                .password("UpdateTest123!")
                .role(Role.MEMBER)
                .build();

        MemberEntity saved = entityManager.persistAndFlush(entity);
        LocalDateTime initialUpdatedAt = saved.getUpdatedAt();

        Thread.sleep(100); // Attendre pour voir la différence

        saved.setPassword("NewPassword123!");
        entityManager.flush();
        entityManager.refresh(saved);

        assertThat(saved.getUpdatedAt()).isAfter(initialUpdatedAt);
    }

    // ====================================================================
    // Tests toString avec toutes les variations
    // ====================================================================

    @Test
    @DisplayName("toString - Doit inclure tous les champs non-null")
    void toString_ShouldIncludeAllNonNullFields() {
        UUID testId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        MemberEntity entity = MemberEntity.builder()
                .id(testId)
                .email("toString@example.com")
                .password("ToStringPass123!")
                .role(Role.ADMIN)
                .createdAt(now)
                .updatedAt(now)
                .enabled(true)
                .build();

        String result = entity.toString();

        assertThat(result).contains("MemberEntity");
        assertThat(result).contains("id=" + testId);
        assertThat(result).contains("email=toString@example.com");
        assertThat(result).contains("password=ToStringPass123!");
        assertThat(result).contains("role=ADMIN");
        assertThat(result).contains("enabled=true");
    }

    // ====================================================================
    // Tests constructeurs et builder
    // ====================================================================

    @Test
    @DisplayName("AllArgsConstructor - Doit créer entité avec tous les champs")
    void allArgsConstructor_ShouldCreateEntityWithAllFields() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        MemberEntity entity = new MemberEntity(
                id,
                "constructor@example.com",
                "ConstructorPass123!",
                Role.MEMBER,
                now,
                now,
                true
        );

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getEmail()).isEqualTo("constructor@example.com");
        assertThat(entity.getPassword()).isEqualTo("ConstructorPass123!");
        assertThat(entity.getRole()).isEqualTo(Role.MEMBER);
        assertThat(entity.getCreatedAt()).isEqualTo(now);
        assertThat(entity.getUpdatedAt()).isEqualTo(now);
        assertThat(entity.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("Builder - @Builder.Default pour enabled doit être false")
    void builder_EnabledDefaultShouldBeFalse() {
        MemberEntity entity = MemberEntity.builder()
                .email("default@example.com")
                .password("DefaultPass123!")
                .role(Role.MEMBER)
                .build();

        assertThat(entity.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("Builder - Doit pouvoir override enabled")
    void builder_ShouldOverrideEnabledDefault() {
        MemberEntity entity = MemberEntity.builder()
                .email("override@example.com")
                .password("OverridePass123!")
                .role(Role.MEMBER)
                .enabled(true)
                .build();

        assertThat(entity.isEnabled()).isTrue();
    }

    // ====================================================================
    // Tests Setters avec timestamps
    // ====================================================================

    @Test
    @DisplayName("Setters - Doit pouvoir définir createdAt et updatedAt")
    void setters_ShouldSetTimestamps() {
        MemberEntity entity = new MemberEntity();
        LocalDateTime createdTime = LocalDateTime.of(2024, 1, 1, 12, 0);
        LocalDateTime updatedTime = LocalDateTime.of(2024, 1, 2, 12, 0);

        entity.setCreatedAt(createdTime);
        entity.setUpdatedAt(updatedTime);

        assertThat(entity.getCreatedAt()).isEqualTo(createdTime);
        assertThat(entity.getUpdatedAt()).isEqualTo(updatedTime);
    }

    // ====================================================================
    // Tests COUVERTURE BRANCHES - equals() avec toutes les combinaisons
    // ====================================================================

    @Test
    @DisplayName("equals - Branches avec champs null vs non-null")
    void equals_BranchesNullVsNonNull() {
        UUID id1 = UUID.randomUUID();
        LocalDateTime time1 = LocalDateTime.of(2024, 1, 1, 12, 0);

        // Entité avec tous les champs null
        MemberEntity entityAllNull = MemberEntity.builder()
                .id(null)
                .email(null)
                .password(null)
                .role(null)
                .createdAt(null)
                .updatedAt(null)
                .enabled(false)
                .build();

        // Entité avec tous les champs non-null
        MemberEntity entityAllNonNull = MemberEntity.builder()
                .id(id1)
                .email("test@example.com")
                .password("Password123!")
                .role(Role.MEMBER)
                .createdAt(time1)
                .updatedAt(time1)
                .enabled(true)
                .build();

        // Entité identique à entityAllNull
        MemberEntity entityAllNull2 = MemberEntity.builder()
                .id(null)
                .email(null)
                .password(null)
                .role(null)
                .createdAt(null)
                .updatedAt(null)
                .enabled(false)
                .build();

        // Test branches equals()
        assertThat(entityAllNull).isEqualTo(entityAllNull2); // null == null
        assertThat(entityAllNull).isNotEqualTo(entityAllNonNull); // null != non-null
        assertThat(entityAllNonNull).isNotEqualTo(entityAllNull); // non-null != null
    }

    @Test
    @DisplayName("equals - Branches avec chaque champ différent")
    void equals_BranchesEachFieldDifferent() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        LocalDateTime time1 = LocalDateTime.of(2024, 1, 1, 12, 0);
        LocalDateTime time2 = LocalDateTime.of(2024, 1, 2, 12, 0);

        MemberEntity baseEntity = MemberEntity.builder()
                .id(id1)
                .email("base@example.com")
                .password("BasePass123!")
                .role(Role.MEMBER)
                .createdAt(time1)
                .updatedAt(time1)
                .enabled(true)
                .build();

        // Test branche ID différent
        MemberEntity differentId = MemberEntity.builder()
                .id(id2) // Différent
                .email("base@example.com")
                .password("BasePass123!")
                .role(Role.MEMBER)
                .createdAt(time1)
                .updatedAt(time1)
                .enabled(true)
                .build();

        // Test branche email différent
        MemberEntity differentEmail = MemberEntity.builder()
                .id(id1)
                .email("different@example.com") // Différent
                .password("BasePass123!")
                .role(Role.MEMBER)
                .createdAt(time1)
                .updatedAt(time1)
                .enabled(true)
                .build();

        // Test branche password différent
        MemberEntity differentPassword = MemberEntity.builder()
                .id(id1)
                .email("base@example.com")
                .password("DifferentPass123!") // Différent
                .role(Role.MEMBER)
                .createdAt(time1)
                .updatedAt(time1)
                .enabled(true)
                .build();

        // Test branche role différent
        MemberEntity differentRole = MemberEntity.builder()
                .id(id1)
                .email("base@example.com")
                .password("BasePass123!")
                .role(Role.ADMIN) // Différent
                .createdAt(time1)
                .updatedAt(time1)
                .enabled(true)
                .build();

        // Test branche enabled différent
        MemberEntity differentEnabled = MemberEntity.builder()
                .id(id1)
                .email("base@example.com")
                .password("BasePass123!")
                .role(Role.MEMBER)
                .createdAt(time1)
                .updatedAt(time1)
                .enabled(false) // Différent
                .build();

        // Test branche createdAt différent
        MemberEntity differentCreatedAt = MemberEntity.builder()
                .id(id1)
                .email("base@example.com")
                .password("BasePass123!")
                .role(Role.MEMBER)
                .createdAt(time2) // Différent
                .updatedAt(time1)
                .enabled(true)
                .build();

        // Test branche updatedAt différent
        MemberEntity differentUpdatedAt = MemberEntity.builder()
                .id(id1)
                .email("base@example.com")
                .password("BasePass123!")
                .role(Role.MEMBER)
                .createdAt(time1)
                .updatedAt(time2) // Différent
                .enabled(true)
                .build();

        // Vérifier toutes les branches de différence
        assertThat(baseEntity).isNotEqualTo(differentId);
        assertThat(baseEntity).isNotEqualTo(differentEmail);
        assertThat(baseEntity).isNotEqualTo(differentPassword);
        assertThat(baseEntity).isNotEqualTo(differentRole);
        assertThat(baseEntity).isNotEqualTo(differentEnabled);
        assertThat(baseEntity).isNotEqualTo(differentCreatedAt);
        assertThat(baseEntity).isNotEqualTo(differentUpdatedAt);
    }

    @Test
    @DisplayName("equals - Branches spéciales (null, même objet, classe différente)")
    void equals_SpecialBranches() {
        MemberEntity entity = MemberEntity.builder()
                .email("special@example.com")
                .password("SpecialPass123!")
                .role(Role.MEMBER)
                .build();

        // Branche: même objet (reflexive)
        assertThat(entity.equals(entity)).isTrue();

        // Branche: objet null
        assertThat(entity.equals(null)).isFalse();

        // Branche: classe différente
        assertThat(entity.equals("not a MemberEntity")).isFalse();

        // Branche: objet d'une autre classe mais même interface
        Object differentClass = new Object();
        assertThat(entity.equals(differentClass)).isFalse();
    }

    // ====================================================================
    // Tests COUVERTURE BRANCHES - hashCode() avec valeurs null
    // ====================================================================

    @Test
    @DisplayName("hashCode - Branches avec champs null")
    void hashCode_BranchesWithNullFields() {
        // Entité avec tous les champs null
        MemberEntity entityAllNull = MemberEntity.builder()
                .id(null)
                .email(null)
                .password(null)
                .role(null)
                .createdAt(null)
                .updatedAt(null)
                .enabled(false)
                .build();

        // Entité avec quelques champs null
        MemberEntity entitySomeNull = MemberEntity.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .password(null) // null
                .role(null) // null
                .createdAt(LocalDateTime.now())
                .updatedAt(null) // null
                .enabled(true)
                .build();

        // Test que hashCode() ne lance pas d'exception avec des null
        assertThatCode(entityAllNull::hashCode).doesNotThrowAnyException();
        assertThatCode(entitySomeNull::hashCode).doesNotThrowAnyException();

        // Test cohérence hashCode
        int hash1 = entityAllNull.hashCode();
        int hash2 = entityAllNull.hashCode();
        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    @DisplayName("hashCode - Objets égaux ont même hashCode")
    void hashCode_EqualObjectsSameHash() {
        UUID id = UUID.randomUUID();
        LocalDateTime time = LocalDateTime.of(2024, 1, 1, 12, 0);

        MemberEntity entity1 = MemberEntity.builder()
                .id(id)
                .email("hash@example.com")
                .password("HashPass123!")
                .role(Role.ADMIN)
                .createdAt(time)
                .updatedAt(time)
                .enabled(true)
                .build();

        MemberEntity entity2 = MemberEntity.builder()
                .id(id)
                .email("hash@example.com")
                .password("HashPass123!")
                .role(Role.ADMIN)
                .createdAt(time)
                .updatedAt(time)
                .enabled(true)
                .build();

        // Objets égaux doivent avoir même hashCode
        assertThat(entity1).isEqualTo(entity2);
        assertThat(entity1.hashCode()).isEqualTo(entity2.hashCode());
    }

    // ====================================================================
    // Tests COUVERTURE BRANCHES - toString() avec valeurs null
    // ====================================================================

    @Test
    @DisplayName("toString - Branches avec champs null")
    void toString_BranchesWithNullFields() {
        MemberEntity entityWithNulls = MemberEntity.builder()
                .id(null)
                .email(null)
                .password(null)
                .role(null)
                .createdAt(null)
                .updatedAt(null)
                .enabled(false)
                .build();

        String result = entityWithNulls.toString();

        assertThat(result).isNotNull();
        assertThat(result).contains("MemberEntity");
        assertThat(result).contains("id=null");
        assertThat(result).contains("email=null");
        assertThat(result).contains("password=null");
        assertThat(result).contains("role=null");
        assertThat(result).contains("createdAt=null");
        assertThat(result).contains("updatedAt=null");
        assertThat(result).contains("enabled=false");
    }

    @Test
    @DisplayName("toString - Branches avec valeurs extrêmes")
    void toString_BranchesWithExtremeValues() {
        UUID extremeId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        LocalDateTime extremeTime = LocalDateTime.MIN;

        MemberEntity entityWithExtremes = MemberEntity.builder()
                .id(extremeId)
                .email("") // Chaîne vide
                .password("a".repeat(100)) // Très long
                .role(Role.ADMIN)
                .createdAt(extremeTime)
                .updatedAt(LocalDateTime.MAX)
                .enabled(true)
                .build();

        String result = entityWithExtremes.toString();

        assertThat(result).isNotNull();
        assertThat(result).contains("00000000-0000-0000-0000-000000000000");
        assertThat(result).contains("email=");
        assertThat(result).contains("a".repeat(100));
        assertThat(result).contains("role=ADMIN");
    }

    // ====================================================================
    // Tests COUVERTURE BRANCHES - Builder avec @Builder.Default
    // ====================================================================

    @Test
    @DisplayName("Builder - Branches @Builder.Default enabled")
    void builder_BranchesBuilderDefault() {
        // Branche: utilisation de la valeur par défaut
        MemberEntity entityWithDefault = MemberEntity.builder()
                .email("default@example.com")
                .password("DefaultPass123!")
                .role(Role.MEMBER)
                .build();

        assertThat(entityWithDefault.isEnabled()).isFalse(); // Valeur par défaut

        // Branche: override de la valeur par défaut avec true
        MemberEntity entityOverrideTrue = MemberEntity.builder()
                .email("override-true@example.com")
                .password("OverridePass123!")
                .role(Role.MEMBER)
                .enabled(true) // Override
                .build();

        assertThat(entityOverrideTrue.isEnabled()).isTrue();

        // Branche: override de la valeur par défaut avec false (explicite)
        MemberEntity entityOverrideFalse = MemberEntity.builder()
                .email("override-false@example.com")
                .password("OverridePass123!")
                .role(Role.MEMBER)
                .enabled(false) // Override explicite
                .build();

        assertThat(entityOverrideFalse.isEnabled()).isFalse();
    }

    // ====================================================================
    // Tests COUVERTURE BRANCHES - Constructeurs
    // ====================================================================

    @Test
    @DisplayName("Constructeurs - Branches NoArgsConstructor")
    void constructors_NoArgsConstructorBranches() {
        MemberEntity entity = new MemberEntity();

        // Vérifier que tous les champs sont à leur valeur par défaut
        assertThat(entity.getId()).isNull();
        assertThat(entity.getEmail()).isNull();
        assertThat(entity.getPassword()).isNull();
        assertThat(entity.getRole()).isNull();
        assertThat(entity.getCreatedAt()).isNull();
        assertThat(entity.getUpdatedAt()).isNull();
        assertThat(entity.isEnabled()).isFalse(); // @Builder.Default
    }

    @Test
    @DisplayName("Constructeurs - Branches AllArgsConstructor avec null")
    void constructors_AllArgsConstructorWithNulls() {
        MemberEntity entity = new MemberEntity(
                null, // id
                null, // email
                null, // password
                null, // role
                null, // createdAt
                null, // updatedAt
                true  // enabled
        );

        assertThat(entity.getId()).isNull();
        assertThat(entity.getEmail()).isNull();
        assertThat(entity.getPassword()).isNull();
        assertThat(entity.getRole()).isNull();
        assertThat(entity.getCreatedAt()).isNull();
        assertThat(entity.getUpdatedAt()).isNull();
        assertThat(entity.isEnabled()).isTrue();
    }

    // ====================================================================
    // Tests COUVERTURE BRANCHES - Getters/Setters avec valeurs extrêmes
    // ====================================================================

    @Test
    @DisplayName("Getters/Setters - Branches avec valeurs extrêmes")
    void gettersSetters_BranchesWithExtremeValues() {
        MemberEntity entity = new MemberEntity();

        // Test avec UUID minimum et maximum
        UUID minUuid = UUID.fromString("00000000-0000-0000-0000-000000000000");
        UUID maxUuid = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");

        entity.setId(minUuid);
        assertThat(entity.getId()).isEqualTo(minUuid);

        entity.setId(maxUuid);
        assertThat(entity.getId()).isEqualTo(maxUuid);

        // Test avec dates extrêmes
        entity.setCreatedAt(LocalDateTime.MIN);
        assertThat(entity.getCreatedAt()).isEqualTo(LocalDateTime.MIN);

        entity.setUpdatedAt(LocalDateTime.MAX);
        assertThat(entity.getUpdatedAt()).isEqualTo(LocalDateTime.MAX);

        // Test avec chaînes vides et très longues
        entity.setEmail("");
        assertThat(entity.getEmail()).isEmpty();

        String longEmail = "a".repeat(1000) + "@example.com";
        entity.setEmail(longEmail);
        assertThat(entity.getEmail()).isEqualTo(longEmail);

        // Test avec tous les rôles
        for (Role role : Role.values()) {
            entity.setRole(role);
            assertThat(entity.getRole()).isEqualTo(role);
        }
    }
}