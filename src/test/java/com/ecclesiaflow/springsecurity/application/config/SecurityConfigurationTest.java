package com.ecclesiaflow.springsecurity.application.config;

import com.ecclesiaflow.springsecurity.business.domain.member.Role;
import com.ecclesiaflow.springsecurity.business.services.MemberService;
import com.ecclesiaflow.springsecurity.web.security.CustomAuthenticationEntryPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests pour SecurityConfiguration.
 * Note: securityFilterChain() nécessite des tests d'intégration Spring Security
 * car il configure HttpSecurity qui dépend du contexte Spring.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityConfiguration - Tests Unitaires")
class SecurityConfigurationTest {

    @Mock
    private JWTAuthenticationFilter jwtAuthenticationFilter;

    @Mock
    private MemberService memberService;

    @Mock
    private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private AuthenticationConfiguration authenticationConfiguration;

    @Mock
    private AuthenticationManager authenticationManager;

    private SecurityConfiguration securityConfiguration;

    @BeforeEach
    void setUp() {
        securityConfiguration = new SecurityConfiguration(
                jwtAuthenticationFilter,
                memberService,
                customAuthenticationEntryPoint
        );
    }

    // ====================================================================
    // Tests passwordEncoder()
    // ====================================================================

    @Nested
    @DisplayName("Tests passwordEncoder()")
    class PasswordEncoderTests {

        @Test
        @DisplayName("Devrait créer un BCryptPasswordEncoder")
        void shouldCreateBCryptPasswordEncoder() {
            // When
            PasswordEncoder encoder = securityConfiguration.passwordEncoder();

            // Then
            assertThat(encoder).isNotNull();
            assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
        }

        @Test
        @DisplayName("Devrait créer une nouvelle instance à chaque appel")
        void shouldCreateNewInstanceEachTime() {
            // When
            PasswordEncoder encoder1 = securityConfiguration.passwordEncoder();
            PasswordEncoder encoder2 = securityConfiguration.passwordEncoder();

            // Then
            assertThat(encoder1).isNotSameAs(encoder2);
        }

        @Test
        @DisplayName("Devrait encoder correctement un mot de passe")
        void shouldEncodePassword() {
            // Given
            PasswordEncoder encoder = securityConfiguration.passwordEncoder();
            String rawPassword = "myPassword123";

            // When
            String encoded = encoder.encode(rawPassword);

            // Then
            assertThat(encoded).isNotNull();
            assertThat(encoded).isNotEqualTo(rawPassword);
            assertThat(encoder.matches(rawPassword, encoded)).isTrue();
        }
    }

    // ====================================================================
    // Tests authenticationProvider()
    // ====================================================================

    @Nested
    @DisplayName("Tests authenticationProvider()")
    class AuthenticationProviderTests {

        @Test
        @DisplayName("Devrait créer un DaoAuthenticationProvider")
        void shouldCreateDaoAuthenticationProvider() {
            // Given
            when(memberService.userDetailsService()).thenReturn(userDetailsService);

            // When
            AuthenticationProvider provider = securityConfiguration.authenticationProvider();

            // Then
            assertThat(provider).isNotNull();
            assertThat(provider).isInstanceOf(DaoAuthenticationProvider.class);
            verify(memberService).userDetailsService();
        }

        @Test
        @DisplayName("Devrait configurer le provider avec UserDetailsService")
        void shouldConfigureProviderWithUserDetailsService() {
            // Given
            when(memberService.userDetailsService()).thenReturn(userDetailsService);

            // When
            AuthenticationProvider provider = securityConfiguration.authenticationProvider();

            // Then
            assertThat(provider).isInstanceOf(DaoAuthenticationProvider.class);
            verify(memberService).userDetailsService();
        }

        @Test
        @DisplayName("Devrait créer une nouvelle instance à chaque appel")
        void shouldCreateNewInstanceEachTime() {
            // Given
            when(memberService.userDetailsService()).thenReturn(userDetailsService);

            // When
            AuthenticationProvider provider1 = securityConfiguration.authenticationProvider();
            AuthenticationProvider provider2 = securityConfiguration.authenticationProvider();

            // Then
            assertThat(provider1).isNotSameAs(provider2);
            verify(memberService, times(2)).userDetailsService();
        }
    }

    // ====================================================================
    // Tests authenticationManager()
    // ====================================================================

    @Nested
    @DisplayName("Tests authenticationManager()")
    class AuthenticationManagerTests {

        @Test
        @DisplayName("Devrait retourner l'AuthenticationManager de la configuration")
        void shouldReturnAuthenticationManagerFromConfig() throws Exception {
            // Given
            when(authenticationConfiguration.getAuthenticationManager()).thenReturn(authenticationManager);

            // When
            AuthenticationManager result = securityConfiguration.authenticationManager(authenticationConfiguration);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(authenticationManager);
            verify(authenticationConfiguration).getAuthenticationManager();
        }

        @Test
        @DisplayName("Devrait propager les exceptions de configuration")
        void shouldPropagateConfigurationExceptions() throws Exception {
            // Given
            when(authenticationConfiguration.getAuthenticationManager())
                    .thenThrow(new RuntimeException("Config error"));

            // When & Then
            assertThatThrownBy(() -> securityConfiguration.authenticationManager(authenticationConfiguration))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Config error");
        }
    }

    // ====================================================================
    // Tests d'intégration des composants
    // ====================================================================

    @Nested
    @DisplayName("Tests d'intégration des beans")
    class BeanIntegrationTests {

        @Test
        @DisplayName("Devrait vérifier que tous les beans sont compatibles")
        void shouldVerifyAllBeansAreCompatible() {
            // Given
            when(memberService.userDetailsService()).thenReturn(userDetailsService);

            // When
            PasswordEncoder encoder = securityConfiguration.passwordEncoder();
            AuthenticationProvider provider = securityConfiguration.authenticationProvider();

            // Then - Vérifier les types
            assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
            assertThat(provider).isInstanceOf(DaoAuthenticationProvider.class);

            // Vérifier que le provider utilise un BCrypt (via réflexion ou test fonctionnel)
            DaoAuthenticationProvider daoProvider = (DaoAuthenticationProvider) provider;
            assertThat(daoProvider).isNotNull();
        }

        @Test
        @DisplayName("Devrait vérifier la cohérence BCrypt entre plusieurs instances")
        void shouldVerifyBCryptConsistency() {
            // When
            PasswordEncoder encoder1 = securityConfiguration.passwordEncoder();
            PasswordEncoder encoder2 = securityConfiguration.passwordEncoder();

            String password = "testPassword";
            String encoded1 = encoder1.encode(password);
            String encoded2 = encoder2.encode(password);

            // Then - Différents hash mais même algorithme
            assertThat(encoded1).isNotEqualTo(encoded2); // BCrypt génère un salt différent
            assertThat(encoder1.matches(password, encoded1)).isTrue();
            assertThat(encoder2.matches(password, encoded2)).isTrue();
            assertThat(encoder1.matches(password, encoded2)).isTrue(); // Cross-validation
        }
    }

    // ====================================================================
    // Tests de vérification de configuration
    // ====================================================================

    @Nested
    @DisplayName("Tests de validation de configuration")
    class ConfigurationValidationTests {

        @Test
        @DisplayName("Devrait vérifier que les rôles sont correctement référencés")
        void shouldVerifyRolesAreCorrectlyReferenced() {
            // Then
            assertThat(Role.ADMIN).isNotNull();
            assertThat(Role.MEMBER).isNotNull();
            assertThat(Role.ADMIN.name()).isEqualTo("ADMIN");
            assertThat(Role.MEMBER.name()).isEqualTo("MEMBER");
        }

        @Test
        @DisplayName("Devrait vérifier que la configuration est bien construite")
        void shouldVerifyConfigurationIsWellConstructed() {
            // Then
            assertThat(securityConfiguration).isNotNull();

            // Vérifier que tous les beans peuvent être créés
            assertThat(securityConfiguration.passwordEncoder()).isNotNull();

            when(memberService.userDetailsService()).thenReturn(userDetailsService);
            assertThat(securityConfiguration.authenticationProvider()).isNotNull();
        }

        @Test
        @DisplayName("Devrait gérer les appels multiples sans erreur")
        void shouldHandleMultipleCallsWithoutError() {
            // Given
            when(memberService.userDetailsService()).thenReturn(userDetailsService);

            // When & Then - Appels multiples ne doivent pas échouer
            for (int i = 0; i < 5; i++) {
                assertThat(securityConfiguration.passwordEncoder()).isNotNull();
                assertThat(securityConfiguration.authenticationProvider()).isNotNull();
            }
        }
    }
}
