package com.ecclesiaflow.springsecurity.web.mappers;

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

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TemporaryTokenMapper - Tests de Validation")
class TemporaryTokenMapperTest {

    private static Validator validator;
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
        mapper = new TemporaryTokenMapper();
    }

    // ====================================================================
    // Tests du mapper TemporaryTokenMapper
    // ====================================================================

    @Test
    @DisplayName("extractEmail() doit renvoyer l'email de la requête")
    void extractEmail_ShouldReturnEmail() {
        String email = mapper.extractEmail(request);
        assertThat(email).isEqualTo("valid.user@example.com");
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