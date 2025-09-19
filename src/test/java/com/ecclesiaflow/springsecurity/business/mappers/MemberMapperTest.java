package com.ecclesiaflow.springsecurity.business.mappers;

import com.ecclesiaflow.springsecurity.business.domain.MemberRegistration;
import com.ecclesiaflow.springsecurity.business.domain.SigninCredentials;
import com.ecclesiaflow.springsecurity.web.payloads.SignUpRequest;
import com.ecclesiaflow.springsecurity.web.payloads.SigninRequest;
import com.ecclesiaflow.springsecurity.web.mappers.MemberMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitaires pour MemberMapper
 * 
 * Teste les conversions entre DTOs web et objets métier pour garantir
 * l'isolation correcte entre la couche web et la couche métier.
 */
@DisplayName("MemberMapper - Tests de conversion")
class MemberMapperTest {

    @Test
    @DisplayName("Devrait convertir SignUpRequest vers MemberRegistration correctement")
    void shouldConvertSignUpRequestToMemberRegistration() {
        // Given
        SignUpRequest request = new SignUpRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setEmail("john.doe@example.com");
        request.setPassword("securePassword123");

        // When
        MemberRegistration registration = MemberMapper.fromSignUpRequest(request);

        // Then
        assertThat(registration).isNotNull();
        assertThat(registration.getFirstName()).isEqualTo("John");
        assertThat(registration.getLastName()).isEqualTo("Doe");
        assertThat(registration.getEmail()).isEqualTo("john.doe@example.com");
        assertThat(registration.getPassword()).isEqualTo("securePassword123");
    }

    @Test
    @DisplayName("Devrait gérer SignUpRequest avec des valeurs nulles")
    void shouldHandleSignUpRequestWithNullValues() {
        // Given
        SignUpRequest request = new SignUpRequest();
        request.setFirstName(null);
        request.setLastName(null);
        request.setEmail(null);
        request.setPassword(null);

        // When
        MemberRegistration registration = MemberMapper.fromSignUpRequest(request);

        // Then
        assertThat(registration).isNotNull();
        assertThat(registration.getFirstName()).isNull();
        assertThat(registration.getLastName()).isNull();
        assertThat(registration.getEmail()).isNull();
        assertThat(registration.getPassword()).isNull();
    }

    @Test
    @DisplayName("Devrait lancer NullPointerException pour SignUpRequest null")
    void shouldThrowNullPointerExceptionForNullSignUpRequest() {
        // When & Then
        assertThatThrownBy(() -> MemberMapper.fromSignUpRequest(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Devrait convertir SigninRequest vers SigninCredentials correctement")
    void shouldConvertSigninRequestToSigninCredentials() {
        // Given
        SigninRequest request = new SigninRequest();
        request.setEmail("user@example.com");
        request.setPassword("myPassword");

        // When
        SigninCredentials credentials = MemberMapper.fromSigninRequest(request);

        // Then
        assertThat(credentials).isNotNull();
        assertThat(credentials.getEmail()).isEqualTo("user@example.com");
        assertThat(credentials.getPassword()).isEqualTo("myPassword");
    }

    @Test
    @DisplayName("Devrait gérer SigninRequest avec des valeurs nulles")
    void shouldHandleSigninRequestWithNullValues() {
        // Given
        SigninRequest request = new SigninRequest();
        request.setEmail(null);
        request.setPassword(null);

        // When
        SigninCredentials credentials = MemberMapper.fromSigninRequest(request);

        // Then
        assertThat(credentials).isNotNull();
        assertThat(credentials.getEmail()).isNull();
        assertThat(credentials.getPassword()).isNull();
    }

    @Test
    @DisplayName("Devrait lancer NullPointerException pour SigninRequest null")
    void shouldThrowNullPointerExceptionForNullSigninRequest() {
        // When & Then
        assertThatThrownBy(() -> MemberMapper.fromSigninRequest(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Devrait préserver les caractères spéciaux dans les conversions")
    void shouldPreserveSpecialCharactersInConversions() {
        // Given
        SignUpRequest signUpRequest = new SignUpRequest();
        signUpRequest.setFirstName("Jean-François");
        signUpRequest.setLastName("O'Connor");
        signUpRequest.setEmail("jean.françois@église.com");
        signUpRequest.setPassword("pàssw0rd!@#$%");

        SigninRequest signinRequest = new SigninRequest();
        signinRequest.setEmail("user+tag@domain.co.uk");
        signinRequest.setPassword("spéc!@l_ch@rs");

        // When
        MemberRegistration registration = MemberMapper.fromSignUpRequest(signUpRequest);
        SigninCredentials credentials = MemberMapper.fromSigninRequest(signinRequest);

        // Then
        assertThat(registration.getFirstName()).isEqualTo("Jean-François");
        assertThat(registration.getLastName()).isEqualTo("O'Connor");
        assertThat(registration.getEmail()).isEqualTo("jean.françois@église.com");
        assertThat(registration.getPassword()).isEqualTo("pàssw0rd!@#$%");

        assertThat(credentials.getEmail()).isEqualTo("user+tag@domain.co.uk");
        assertThat(credentials.getPassword()).isEqualTo("spéc!@l_ch@rs");
    }

    @Test
    @DisplayName("Devrait gérer les chaînes vides")
    void shouldHandleEmptyStrings() {
        // Given
        SignUpRequest signUpRequest = new SignUpRequest();
        signUpRequest.setFirstName("");
        signUpRequest.setLastName("");
        signUpRequest.setEmail("");
        signUpRequest.setPassword("");

        SigninRequest signinRequest = new SigninRequest();
        signinRequest.setEmail("");
        signinRequest.setPassword("");

        // When
        MemberRegistration registration = MemberMapper.fromSignUpRequest(signUpRequest);
        SigninCredentials credentials = MemberMapper.fromSigninRequest(signinRequest);

        // Then
        assertThat(registration.getFirstName()).isEmpty();
        assertThat(registration.getLastName()).isEmpty();
        assertThat(registration.getEmail()).isEmpty();
        assertThat(registration.getPassword()).isEmpty();

        assertThat(credentials.getEmail()).isEmpty();
        assertThat(credentials.getPassword()).isEmpty();
    }

    @Test
    @DisplayName("Devrait créer des objets indépendants")
    void shouldCreateIndependentObjects() {
        // Given
        SignUpRequest request = new SignUpRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setEmail("john@example.com");
        request.setPassword("password");

        // When
        MemberRegistration registration1 = MemberMapper.fromSignUpRequest(request);
        MemberRegistration registration2 = MemberMapper.fromSignUpRequest(request);

        // Then
        assertThat(registration1).isNotSameAs(registration2);
        assertThat(registration1.getFirstName()).isEqualTo(registration2.getFirstName());
        assertThat(registration1.getLastName()).isEqualTo(registration2.getLastName());
        assertThat(registration1.getEmail()).isEqualTo(registration2.getEmail());
        assertThat(registration1.getPassword()).isEqualTo(registration2.getPassword());
    }

    @Test
    @DisplayName("Devrait gérer les données très longues")
    void shouldHandleVeryLongData() {
        // Given
        String longString = "a".repeat(1000);
        SignUpRequest request = new SignUpRequest();
        request.setFirstName(longString);
        request.setLastName(longString);
        request.setEmail(longString + "@example.com");
        request.setPassword(longString);

        // When
        MemberRegistration registration = MemberMapper.fromSignUpRequest(request);

        // Then
        assertThat(registration.getFirstName()).hasSize(1000);
        assertThat(registration.getLastName()).hasSize(1000);
        assertThat(registration.getEmail()).hasSize(1012); // 1000 + "@example.com"
        assertThat(registration.getPassword()).hasSize(1000);
    }

    @Test
    @DisplayName("Devrait maintenir l'intégrité des données lors des conversions multiples")
    void shouldMaintainDataIntegrityDuringMultipleConversions() {
        // Given
        SignUpRequest originalRequest = new SignUpRequest();
        originalRequest.setFirstName("Original");
        originalRequest.setLastName("User");
        originalRequest.setEmail("original@test.com");
        originalRequest.setPassword("originalPassword");

        // When
        MemberRegistration registration = MemberMapper.fromSignUpRequest(originalRequest);
        
        // Modifier la requête originale
        originalRequest.setFirstName("Modified");
        originalRequest.setEmail("modified@test.com");

        // Then
        // L'objet converti ne doit pas être affecté par les modifications de l'original
        assertThat(registration.getFirstName()).isEqualTo("Original");
        assertThat(registration.getEmail()).isEqualTo("original@test.com");
    }

    @Test
    @DisplayName("Devrait gérer les espaces en début et fin de chaîne")
    void shouldHandleLeadingAndTrailingSpaces() {
        // Given
        SignUpRequest request = new SignUpRequest();
        request.setFirstName("  John  ");
        request.setLastName("  Doe  ");
        request.setEmail("  john@example.com  ");
        request.setPassword("  password  ");

        // When
        MemberRegistration registration = MemberMapper.fromSignUpRequest(request);

        // Then
        // Les espaces doivent être préservés (pas de trim automatique)
        assertThat(registration.getFirstName()).isEqualTo("  John  ");
        assertThat(registration.getLastName()).isEqualTo("  Doe  ");
        assertThat(registration.getEmail()).isEqualTo("  john@example.com  ");
        assertThat(registration.getPassword()).isEqualTo("  password  ");
    }
}
