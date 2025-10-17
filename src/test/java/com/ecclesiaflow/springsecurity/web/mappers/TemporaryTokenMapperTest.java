package com.ecclesiaflow.springsecurity.web.mappers;

import com.ecclesiaflow.springsecurity.business.domain.token.TemporaryToken;
import com.ecclesiaflow.springsecurity.web.model.TemporaryTokenRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TemporaryTokenMapper - Transformation Web DTO → Domain")
class TemporaryTokenMapperTest {

    private static Validator validator;
    private static final UUID VALID_MEMBER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    
    private TemporaryTokenRequest request;
    private TemporaryTokenMapper mapper;

    @BeforeAll
    static void setUpValidator() {
        // Initialisation du validateur Jakarta Validation (Bean Validation)
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @BeforeEach
    void setUp() {
        // Objet valide de base
        request = new TemporaryTokenRequest();
        request.setEmail("valid.user@example.com");
        request.setMemberId(VALID_MEMBER_ID);
        mapper = new TemporaryTokenMapper();
    }

    // ====================================================================
    // Tests du Mapper - Transformation toDomain()
    // ====================================================================

    @Test
    @DisplayName("toDomain() devrait transformer DTO web en Value Object domain")
    void toDomain_ShouldTransformWebDtoToDomainObject() {
        // When
        TemporaryToken domainToken = mapper.toDomain(request);

        // Then
        assertThat(domainToken).isNotNull();
        assertThat(domainToken.email()).isEqualTo("valid.user@example.com");
        assertThat(domainToken.memberId()).isEqualTo(VALID_MEMBER_ID);
    }

    @Test
    @DisplayName("toDomain() devrait conserver tous les champs du DTO")
    void toDomain_ShouldPreserveAllFields() {
        // Given
        String complexEmail = "user.name+tag@sub.domain.com";
        UUID specificMemberId = UUID.randomUUID();
        request.setEmail(complexEmail);
        request.setMemberId(specificMemberId);

        // When
        TemporaryToken domainToken = mapper.toDomain(request);

        // Then
        assertThat(domainToken.email()).isEqualTo(complexEmail);
        assertThat(domainToken.memberId()).isEqualTo(specificMemberId);
    }

    @Test
    @DisplayName("toDomain() devrait lever une exception si email est null")
    void toDomain_ShouldThrowException_WhenEmailIsNull() {
        // Given
        request.setEmail(null);

        // When/Then
        assertThatThrownBy(() -> mapper.toDomain(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("L'email ne peut pas être null ou vide");
    }

    @Test
    @DisplayName("toDomain() devrait lever une exception si memberId est null")
    void toDomain_ShouldThrowException_WhenMemberIdIsNull() {
        // Given
        request.setMemberId(null);

        // When/Then
        assertThatThrownBy(() -> mapper.toDomain(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Le memberId ne peut pas être null");
    }

    // ====================================================================
    // Tests de Validation Réussis
    // ====================================================================

    @Test
    @DisplayName("Devrait passer la validation avec un email valide")
    void shouldPassValidation_WithValidEmail() {
        // Act
        Set<ConstraintViolation<TemporaryTokenRequest>> violations = validator.validate(request);

        // Assert
        assertThat(violations).isEmpty();
    }

    // ====================================================================
    // Tests de Validation Échoués (@NotBlank)
    // ====================================================================

    @Test
    @DisplayName("Devrait échouer si l'email est null (NotNull)")
    void shouldFailValidation_WhenEmailIsNull() {
        // Arrange
        request.setEmail(null);

        // Act
        Set<ConstraintViolation<TemporaryTokenRequest>> violations = validator.validate(request);

        // Assert
        assertThat(violations).hasSize(1);
        ConstraintViolation<TemporaryTokenRequest> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath().toString()).isEqualTo("email");
        assertThat(violation.getMessage()).isEqualTo("ne doit pas être nul");
    }

    @Test
    @DisplayName("Devrait échouer si l'email est vide (@Email)")
    void shouldFailValidation_WhenEmailIsBlank() {
        // Arrange
        request.setEmail("  "); // Une chaîne contenant uniquement des espaces

        // Act
        Set<ConstraintViolation<TemporaryTokenRequest>> violations = validator.validate(request);

        // Assert
        // Les modèles OpenAPI générés utilisent @Email qui ne valide que le format
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
            .isEqualTo("doit être une adresse électronique syntaxiquement correcte");
    }

    // ====================================================================
    // Tests de Validation Échoués (@Email)
    // ====================================================================

    @Test
    @DisplayName("Devrait échouer si l'email n'a pas le format correct (@Email)")
    void shouldFailValidation_WhenEmailHasInvalidFormat() {
        // Arrange
        request.setEmail("invalid-email-format");

        // Act
        Set<ConstraintViolation<TemporaryTokenRequest>> violations = validator.validate(request);

        // Assert
        assertThat(violations).hasSize(1);
        ConstraintViolation<TemporaryTokenRequest> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath().toString()).isEqualTo("email");
        assertThat(violation.getMessage()).isEqualTo("doit être une adresse électronique syntaxiquement correcte");
    }

    @Test
    @DisplayName("Devrait échouer si l'email n'a pas de domaine (@Email)")
    void shouldFailValidation_WhenEmailHasNoDomain() {
        // Arrange
        request.setEmail("user@");

        // Act
        Set<ConstraintViolation<TemporaryTokenRequest>> violations = validator.validate(request);

        // Assert
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
            .isEqualTo("doit être une adresse électronique syntaxiquement correcte");
    }
}