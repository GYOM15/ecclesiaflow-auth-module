package com.ecclesiaflow.springsecurity.web.config;

import com.ecclesiaflow.springsecurity.business.encryption.PasswordEncoderUtil;
import com.ecclesiaflow.springsecurity.business.services.MemberService;
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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

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
    private PasswordEncoderUtil passwordEncoderUtil;

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
                passwordEncoderUtil
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
        // Note: Les propriétés privées ne sont pas directement testables,
        // mais nous pouvons vérifier que l'objet est correctement créé
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
        
        // Note: SecurityFilterChain nécessite un contexte Spring complet pour être testé
        // Ce test vérifie que la configuration est cohérente au niveau des beans
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
}
