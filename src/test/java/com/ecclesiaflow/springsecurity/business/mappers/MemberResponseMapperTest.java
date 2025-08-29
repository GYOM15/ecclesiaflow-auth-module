package com.ecclesiaflow.springsecurity.business.mappers;

import com.ecclesiaflow.springsecurity.web.dto.MemberResponse;
import com.ecclesiaflow.springsecurity.io.entities.Member;
import com.ecclesiaflow.springsecurity.io.entities.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitaires pour MemberResponseMapper
 * 
 * Teste les conversions entre entités Member et DTOs de réponse web
 * pour garantir l'isolation correcte entre la couche persistance et la couche web.
 */
@DisplayName("MemberResponseMapper - Tests de conversion")
class MemberResponseMapperTest {

    private Member testMember;

    @BeforeEach
    void setUp() {
        testMember = createTestMember();
    }

    @Test
    @DisplayName("Devrait convertir Member vers MemberResponse avec message et token")
    void shouldConvertMemberToMemberResponseWithMessageAndToken() {
        // Given
        String message = "Connexion réussie";
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test";

        // When
        MemberResponse response = MemberResponseMapper.fromMember(testMember, message, token);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEqualTo(message);
        assertThat(response.getToken()).isEqualTo(token);
        assertThat(response.getFirstName()).isEqualTo(testMember.getFirstName());
        assertThat(response.getLastName()).isEqualTo(testMember.getLastName());
        assertThat(response.getEmail()).isEqualTo(testMember.getEmail());
        assertThat(response.getPassword()).isEqualTo(testMember.getPassword());
        assertThat(response.getRole()).isEqualTo(testMember.getRole().name());
        assertThat(response.isAccountNonLocked()).isEqualTo(testMember.isAccountNonLocked());
        assertThat(response.isEnabled()).isEqualTo(testMember.isEnabled());
        assertThat(response.getAuthorities()).isEqualTo(testMember.getAuthorities());
        assertThat(response.getUsername()).isEqualTo(testMember.getUsername());
        assertThat(response.isAccountNonExpired()).isEqualTo(testMember.isAccountNonExpired());
        assertThat(response.isCredentialsNonExpired()).isEqualTo(testMember.isCredentialsNonExpired());
    }

    @Test
    @DisplayName("Devrait convertir Member vers MemberResponse avec message seulement")
    void shouldConvertMemberToMemberResponseWithMessageOnly() {
        // Given
        String message = "Profil récupéré";

        // When
        MemberResponse response = MemberResponseMapper.fromMember(testMember, message);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEqualTo(message);
        assertThat(response.getToken()).isNull();
        assertThat(response.getFirstName()).isEqualTo(testMember.getFirstName());
        assertThat(response.getLastName()).isEqualTo(testMember.getLastName());
        assertThat(response.getEmail()).isEqualTo(testMember.getEmail());
    }

    @Test
    @DisplayName("Devrait gérer Member avec token null")
    void shouldHandleMemberWithNullToken() {
        // Given
        String message = "Test message";

        // When
        MemberResponse response = MemberResponseMapper.fromMember(testMember, message, null);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEqualTo(message);
        assertThat(response.getToken()).isNull();
        assertThat(response.getFirstName()).isEqualTo(testMember.getFirstName());
    }

    @Test
    @DisplayName("Devrait lancer NullPointerException pour Member null")
    void shouldThrowNullPointerExceptionForNullMember() {
        // When & Then
        assertThatThrownBy(() -> MemberResponseMapper.fromMember(null, "message", "token"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Devrait gérer message null")
    void shouldHandleNullMessage() {
        // When
        MemberResponse response = MemberResponseMapper.fromMember(testMember, null, "token");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getToken()).isEqualTo("token");
        assertThat(response.getFirstName()).isEqualTo(testMember.getFirstName());
    }

    @Test
    @DisplayName("Devrait gérer Member avec des valeurs nulles")
    void shouldHandleMemberWithNullValues() {
        // Given
        Member memberWithNulls = new Member();
        memberWithNulls.setId(UUID.randomUUID());
        memberWithNulls.setFirstName(null);
        memberWithNulls.setLastName(null);
        memberWithNulls.setEmail(null);
        memberWithNulls.setPassword(null);
        memberWithNulls.setRole(Role.MEMBER);

        String message = "Test avec valeurs nulles";

        // When
        MemberResponse response = MemberResponseMapper.fromMember(memberWithNulls, message, "token");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEqualTo(message);
        assertThat(response.getFirstName()).isNull();
        assertThat(response.getLastName()).isNull();
        assertThat(response.getEmail()).isNull();
        assertThat(response.getPassword()).isNull();
        assertThat(response.getRole()).isEqualTo("MEMBER");
    }

    @Test
    @DisplayName("Devrait gérer différents rôles correctement")
    void shouldHandleDifferentRolesCorrectly() {
        // Given
        Member adminMember = createTestMember();
        adminMember.setRole(Role.ADMIN);

        Member pastorMember = createTestMember();
        pastorMember.setRole(Role.MEMBER);

        String message = "Test rôles";

        // When
        MemberResponse adminResponse = MemberResponseMapper.fromMember(adminMember, message);
        MemberResponse pastorResponse = MemberResponseMapper.fromMember(pastorMember, message);

        // Then
        assertThat(adminResponse.getRole()).isEqualTo("ADMIN");
        assertThat(pastorResponse.getRole()).isEqualTo("MEMBER");
    }

    @Test
    @DisplayName("Devrait préserver les caractères spéciaux")
    void shouldPreserveSpecialCharacters() {
        // Given
        Member memberWithSpecialChars = createTestMember();
        memberWithSpecialChars.setFirstName("Jean-François");
        memberWithSpecialChars.setLastName("O'Connor");
        memberWithSpecialChars.setEmail("jean.françois@église.com");

        String messageWithSpecialChars = "Bienvenue chez EcclesiaFlow! éàùç";
        String tokenWithSpecialChars = "token.with.special.chars!@#$%";

        // When
        MemberResponse response = MemberResponseMapper.fromMember(
            memberWithSpecialChars, 
            messageWithSpecialChars, 
            tokenWithSpecialChars
        );

        // Then
        assertThat(response.getFirstName()).isEqualTo("Jean-François");
        assertThat(response.getLastName()).isEqualTo("O'Connor");
        assertThat(response.getEmail()).isEqualTo("jean.françois@église.com");
        assertThat(response.getMessage()).isEqualTo("Bienvenue chez EcclesiaFlow! éàùç");
        assertThat(response.getToken()).isEqualTo("token.with.special.chars!@#$%");
    }

    @Test
    @DisplayName("Devrait gérer les chaînes vides")
    void shouldHandleEmptyStrings() {
        // Given
        Member memberWithEmptyStrings = createTestMember();
        memberWithEmptyStrings.setFirstName("");
        memberWithEmptyStrings.setLastName("");
        memberWithEmptyStrings.setEmail("");
        memberWithEmptyStrings.setPassword("");

        // When
        MemberResponse response = MemberResponseMapper.fromMember(memberWithEmptyStrings, "", "");

        // Then
        assertThat(response.getFirstName()).isEmpty();
        assertThat(response.getLastName()).isEmpty();
        assertThat(response.getEmail()).isEmpty();
        assertThat(response.getPassword()).isEmpty();
        assertThat(response.getMessage()).isEmpty();
        assertThat(response.getToken()).isEmpty();
    }

    @Test
    @DisplayName("Devrait créer des objets indépendants")
    void shouldCreateIndependentObjects() {
        // Given
        String message = "Test message";
        String token = "test.token";

        // When
        MemberResponse response1 = MemberResponseMapper.fromMember(testMember, message, token);
        MemberResponse response2 = MemberResponseMapper.fromMember(testMember, message, token);

        // Then
        assertThat(response1).isNotSameAs(response2);
        assertThat(response1.getMessage()).isEqualTo(response2.getMessage());
        assertThat(response1.getToken()).isEqualTo(response2.getToken());
        assertThat(response1.getFirstName()).isEqualTo(response2.getFirstName());
    }

    @Test
    @DisplayName("Devrait gérer les données très longues")
    void shouldHandleVeryLongData() {
        // Given
        Member memberWithLongData = createTestMember();
        String longString = "a".repeat(1000);
        memberWithLongData.setFirstName(longString);
        memberWithLongData.setLastName(longString);

        String longMessage = "b".repeat(2000);
        String longToken = "c".repeat(3000);

        // When
        MemberResponse response = MemberResponseMapper.fromMember(memberWithLongData, longMessage, longToken);

        // Then
        assertThat(response.getFirstName()).hasSize(1000);
        assertThat(response.getLastName()).hasSize(1000);
        assertThat(response.getMessage()).hasSize(2000);
        assertThat(response.getToken()).hasSize(3000);
    }

    @Test
    @DisplayName("Devrait maintenir la cohérence entre les deux méthodes fromMember")
    void shouldMaintainConsistencyBetweenFromMemberMethods() {
        // Given
        String message = "Test consistency";
        String token = "test.token";

        // When
        MemberResponse responseWithToken = MemberResponseMapper.fromMember(testMember, message, token);
        MemberResponse responseWithoutToken = MemberResponseMapper.fromMember(testMember, message);

        // Then
        // Tous les champs doivent être identiques sauf le token
        assertThat(responseWithToken.getMessage()).isEqualTo(responseWithoutToken.getMessage());
        assertThat(responseWithToken.getFirstName()).isEqualTo(responseWithoutToken.getFirstName());
        assertThat(responseWithToken.getLastName()).isEqualTo(responseWithoutToken.getLastName());
        assertThat(responseWithToken.getEmail()).isEqualTo(responseWithoutToken.getEmail());
        assertThat(responseWithToken.getRole()).isEqualTo(responseWithoutToken.getRole());
        
        // Seul le token doit différer
        assertThat(responseWithToken.getToken()).isEqualTo(token);
        assertThat(responseWithoutToken.getToken()).isNull();
    }

    @Test
    @DisplayName("Devrait gérer les états de compte correctement")
    void shouldHandleAccountStatesCorrectly() {
        // Given
        Member member = createTestMember();

        // When
        MemberResponse response = MemberResponseMapper.fromMember(member, "Test états");

        // Then
        // Les méthodes UserDetails retournent des valeurs fixes dans l'implémentation
        assertThat(response.isAccountNonLocked()).isTrue();
        assertThat(response.isEnabled()).isTrue();
        assertThat(response.isAccountNonExpired()).isTrue();
        assertThat(response.isCredentialsNonExpired()).isTrue();
    }

    private Member createTestMember() {
        Member member = new Member();
        member.setId(UUID.randomUUID());
        member.setFirstName("John");
        member.setLastName("Doe");
        member.setEmail("john.doe@example.com");
        member.setPassword("encodedPassword123");
        member.setRole(Role.MEMBER);
        // Les méthodes UserDetails (isAccountNonLocked, isEnabled, etc.) 
        // retournent des valeurs fixes dans l'implémentation
        return member;
    }
}
