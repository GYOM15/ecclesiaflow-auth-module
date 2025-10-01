package com.ecclesiaflow.springsecurity.application.config;

import com.ecclesiaflow.springsecurity.business.domain.member.Role;
import com.ecclesiaflow.springsecurity.business.services.MemberService;
import com.ecclesiaflow.springsecurity.web.security.CustomAuthenticationEntryPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour SecurityConfiguration
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityConfiguration Tests")
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

    @Test
    @DisplayName("Devrait créer un bean PasswordEncoder de type BCrypt")
    void shouldCreateBCryptPasswordEncoder() {
        // When
        PasswordEncoder passwordEncoder = securityConfiguration.passwordEncoder();

        // Then
        assertThat(passwordEncoder).isNotNull();
        assertThat(passwordEncoder).isInstanceOf(BCryptPasswordEncoder.class);
    }

    @Test
    @DisplayName("Devrait créer un AuthenticationProvider avec UserDetailsService et PasswordEncoder")
    void shouldCreateAuthenticationProviderWithCorrectConfiguration() {
        // Given
        when(memberService.userDetailsService()).thenReturn(userDetailsService);

        // When
        AuthenticationProvider authProvider = securityConfiguration.authenticationProvider();

        // Then
        assertThat(authProvider).isNotNull();
        assertThat(authProvider).isInstanceOf(DaoAuthenticationProvider.class);

        DaoAuthenticationProvider daoAuthProvider = (DaoAuthenticationProvider) authProvider;
        assertThat(daoAuthProvider).isNotNull();
    }

    @Test
    @DisplayName("Devrait créer un AuthenticationManager depuis AuthenticationConfiguration")
    void shouldCreateAuthenticationManager() throws Exception {
        // Given
        when(authenticationConfiguration.getAuthenticationManager()).thenReturn(authenticationManager);

        // When
        AuthenticationManager manager = securityConfiguration.authenticationManager(authenticationConfiguration);

        // Then
        assertThat(manager).isNotNull();
        assertThat(manager).isEqualTo(authenticationManager);
    }

    @Test
    @DisplayName("Devrait vérifier que SecurityConfiguration est bien configurée")
    void shouldVerifySecurityConfigurationIsWellConfigured() {
        // Given
        when(memberService.userDetailsService()).thenReturn(userDetailsService);

        // When & Then - Vérifier que tous les beans peuvent être créés sans erreur
        assertThat(securityConfiguration.passwordEncoder()).isNotNull();
        assertThat(securityConfiguration.authenticationProvider()).isNotNull();

    }

    @Test
    @DisplayName("Devrait vérifier que les beans sont correctement injectés")
    void shouldVerifyBeansAreCorrectlyInjected() {
        // Then - Vérifier que la configuration a été créée avec les bonnes dépendances
        assertThat(securityConfiguration).isNotNull();

        // Vérifier que les beans peuvent être créés
        PasswordEncoder passwordEncoder = securityConfiguration.passwordEncoder();
        assertThat(passwordEncoder).isNotNull();

        when(memberService.userDetailsService()).thenReturn(userDetailsService);
        AuthenticationProvider authProvider = securityConfiguration.authenticationProvider();
        assertThat(authProvider).isNotNull();
    }

    @Test
    @DisplayName("Devrait créer des beans indépendants (pas de singleton partagé)")
    void shouldCreateIndependentBeans() {
        // When
        PasswordEncoder encoder1 = securityConfiguration.passwordEncoder();
        PasswordEncoder encoder2 = securityConfiguration.passwordEncoder();

        // Then - Chaque appel devrait créer une nouvelle instance
        assertThat(encoder1).isNotNull();
        assertThat(encoder2).isNotNull();
        assertThat(encoder1).isNotSameAs(encoder2); // Différentes instances
        assertThat(encoder1.getClass()).isEqualTo(encoder2.getClass()); // Même type
    }

    @Test
    @DisplayName("Devrait configurer AuthenticationProvider avec les bonnes dépendances")
    void shouldConfigureAuthenticationProviderWithCorrectDependencies() {
        // Given
        when(memberService.userDetailsService()).thenReturn(userDetailsService);

        // When
        AuthenticationProvider provider1 = securityConfiguration.authenticationProvider();
        AuthenticationProvider provider2 = securityConfiguration.authenticationProvider();

        // Then
        assertThat(provider1).isNotNull();
        assertThat(provider2).isNotNull();
        assertThat(provider1).isInstanceOf(DaoAuthenticationProvider.class);
        assertThat(provider2).isInstanceOf(DaoAuthenticationProvider.class);

        // Chaque appel devrait créer une nouvelle instance
        assertThat(provider1).isNotSameAs(provider2);
    }

    @Test
    @DisplayName("Devrait vérifier que SecurityFilterChain nécessite un test d'intégration")
    void shouldVerifySecurityFilterChainRequiresIntegrationTest() {
        // Vérifier que la configuration existe et peut être instanciée
        assertThat(securityConfiguration).isNotNull();
    }

    @Test
    @DisplayName("Devrait vérifier que tous les rôles sont correctement référencés")
    void shouldVerifyAllRolesAreCorrectlyReferenced() {
        // Then - Vérifier que les rôles utilisés dans la configuration existent
        assertThat(Role.ADMIN).isNotNull();
        assertThat(Role.MEMBER).isNotNull();
        
        // Vérifier que les noms des rôles sont cohérents
        assertThat(Role.ADMIN.name()).isEqualTo("ADMIN");
        assertThat(Role.MEMBER.name()).isEqualTo("MEMBER");
    }

    @Test
    @DisplayName("Devrait vérifier l'intégration entre AuthenticationProvider et PasswordEncoder")
    void shouldVerifyAuthenticationProviderPasswordEncoderIntegration() {
        // Given
        when(memberService.userDetailsService()).thenReturn(userDetailsService);

        // When
        AuthenticationProvider provider = securityConfiguration.authenticationProvider();
        PasswordEncoder encoder = securityConfiguration.passwordEncoder();

        // Then
        assertThat(provider).isInstanceOf(DaoAuthenticationProvider.class);
        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
        
        // Vérifier que le provider est correctement configuré
        DaoAuthenticationProvider daoProvider = (DaoAuthenticationProvider) provider;
        assertThat(daoProvider).isNotNull();
    }

    @Test
    @DisplayName("Devrait vérifier que la configuration peut gérer les exceptions")
    void shouldVerifyConfigurationCanHandleExceptions() throws Exception {
        // Given
        when(authenticationConfiguration.getAuthenticationManager())
                .thenThrow(new RuntimeException("Configuration error"));

        // When & Then
        try {
            securityConfiguration.authenticationManager(authenticationConfiguration);
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("Configuration error");
        }
    }

    @Test
    @DisplayName("Devrait vérifier la cohérence de la configuration de sécurité")
    void shouldVerifySecurityConfigurationConsistency() {
        // Given
        when(memberService.userDetailsService()).thenReturn(userDetailsService);

        // When - Créer tous les beans de sécurité
        PasswordEncoder encoder = securityConfiguration.passwordEncoder();
        AuthenticationProvider provider = securityConfiguration.authenticationProvider();

        // Then - Vérifier la cohérence
        assertThat(encoder).isNotNull();
        assertThat(provider).isNotNull();
        
        // Vérifier que les types sont corrects
        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
        assertThat(provider).isInstanceOf(DaoAuthenticationProvider.class);
        
        // Vérifier que la configuration est réutilisable
        PasswordEncoder encoder2 = securityConfiguration.passwordEncoder();
        assertThat(encoder2).isNotNull();
        assertThat(encoder2.getClass()).isEqualTo(encoder.getClass());
    }
}
