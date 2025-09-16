package com.ecclesiaflow.springsecurity.web.config;

import com.ecclesiaflow.springsecurity.application.config.JWTAuthenticationFilter;
import com.ecclesiaflow.springsecurity.business.services.MemberService;
import com.ecclesiaflow.springsecurity.web.security.JwtProcessor;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour JWTAuthenticationFilter
 * 
 * Teste le filtre d'authentification JWT qui extrait et valide les tokens
 * pour sécuriser les endpoints de l'API.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("JWTAuthenticationFilter - Tests de filtre d'authentification")
class JWTAuthenticationFilterTest {

    @Mock
    private JwtProcessor jwtProcessor;

    @Mock
    private MemberService memberService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JWTAuthenticationFilter jwtAuthenticationFilter;

    private TestUserDetails testUserDetails;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        testUserDetails = new TestUserDetails("test@example.com", "password", 
            List.of(new SimpleGrantedAuthority("ROLE_USER")));
        
        when(memberService.userDetailsService()).thenReturn(userDetailsService);
    }

    @Test
    @DisplayName("Devrait passer la requête si pas d'header Authorization")
    void shouldPassRequestWhenNoAuthorizationHeader() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn(null);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(jwtProcessor, never()).extractUsername(anyString());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Devrait passer la requête si header Authorization vide")
    void shouldPassRequestWhenEmptyAuthorizationHeader() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(jwtProcessor, never()).extractUsername(anyString());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Devrait passer la requête si header ne commence pas par Bearer")
    void shouldPassRequestWhenHeaderDoesNotStartWithBearer() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Basic dGVzdDp0ZXN0");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(jwtProcessor, never()).extractUsername(anyString());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Devrait authentifier avec token JWT valide")
    void shouldAuthenticateWithValidJWTToken() throws ServletException, IOException {
        // Given
        String validToken = "valid.jwt.token";
        String userEmail = "test@example.com";
        
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(jwtProcessor.extractUsername(validToken)).thenReturn(userEmail);
        when(userDetailsService.loadUserByUsername(userEmail)).thenReturn(testUserDetails);
        when(jwtProcessor.isTokenValid(validToken)).thenReturn(true);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(jwtProcessor).extractUsername(validToken);
        verify(userDetailsService).loadUserByUsername(userEmail);
        verify(jwtProcessor).isTokenValid(validToken);
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo(userEmail);
        assertThat(authentication.getAuthorities()).hasSize(1);
    }

    @Test
    @DisplayName("Devrait ne pas authentifier avec token JWT invalide")
    void shouldNotAuthenticateWithInvalidJWTToken() throws ServletException, IOException {
        // Given
        String invalidToken = "invalid.jwt.token";
        String userEmail = "test@example.com";
        
        when(request.getHeader("Authorization")).thenReturn("Bearer " + invalidToken);
        when(jwtProcessor.extractUsername(invalidToken)).thenReturn(userEmail);
        when(userDetailsService.loadUserByUsername(userEmail)).thenReturn(testUserDetails);
        when(jwtProcessor.isTokenValid(invalidToken)).thenReturn(false);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(jwtProcessor).extractUsername(invalidToken);
        verify(userDetailsService).loadUserByUsername(userEmail);
        verify(jwtProcessor).isTokenValid(invalidToken);
        
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Devrait ne pas authentifier si email extrait est vide")
    void shouldNotAuthenticateWhenExtractedEmailIsEmpty() throws ServletException, IOException {
        // Given
        String token = "valid.jwt.token";
        
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtProcessor.extractUsername(token)).thenReturn("");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(jwtProcessor).extractUsername(token);
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Devrait ne pas authentifier si email extrait est null")
    void shouldNotAuthenticateWhenExtractedEmailIsNull() throws ServletException, IOException {
        // Given
        String token = "valid.jwt.token";
        
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtProcessor.extractUsername(token)).thenReturn(null);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(jwtProcessor).extractUsername(token);
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Devrait ne pas re-authentifier si utilisateur déjà authentifié")
    void shouldNotReAuthenticateWhenUserAlreadyAuthenticated() throws ServletException, IOException {
        // Given
        String token = "valid.jwt.token";
        String userEmail = "test@example.com";
        
        // Simuler un utilisateur déjà authentifié
        SecurityContext existingContext = SecurityContextHolder.createEmptyContext();
        existingContext.setAuthentication(mock(Authentication.class));
        SecurityContextHolder.setContext(existingContext);
        
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtProcessor.extractUsername(token)).thenReturn(userEmail);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(jwtProcessor).extractUsername(token);
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(jwtProcessor, never()).isTokenValid(anyString());
    }

    @Test
    @DisplayName("Devrait gérer les exceptions lors de l'extraction du username")
    void shouldHandleExceptionsDuringUsernameExtraction() throws ServletException, IOException {
        // Given
        String token = "malformed.jwt.token";
        
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtProcessor.extractUsername(token)).thenThrow(new RuntimeException("Token malformed"));

        // When & Then
        assertThatThrownBy(() -> jwtAuthenticationFilter.doFilterInternal(request, response, filterChain))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Token malformed");
    }

    @Test
    @DisplayName("Devrait gérer les exceptions lors du chargement des détails utilisateur")
    void shouldHandleExceptionsDuringUserDetailsLoading() throws ServletException, IOException {
        // Given
        String token = "valid.jwt.token";
        String userEmail = "nonexistent@example.com";
        
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtProcessor.extractUsername(token)).thenReturn(userEmail);
        when(userDetailsService.loadUserByUsername(userEmail))
                .thenThrow(new RuntimeException("User not found"));

        // When & Then
        assertThatThrownBy(() -> jwtAuthenticationFilter.doFilterInternal(request, response, filterChain))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");
    }

    @Test
    @DisplayName("Devrait gérer les exceptions lors de la validation du token")
    void shouldHandleExceptionsDuringTokenValidation() throws ServletException, IOException {
        // Given
        String token = "problematic.jwt.token";
        String userEmail = "test@example.com";
        
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtProcessor.extractUsername(token)).thenReturn(userEmail);
        when(userDetailsService.loadUserByUsername(userEmail)).thenReturn(testUserDetails);
        when(jwtProcessor.isTokenValid(token)).thenThrow(new RuntimeException("Token validation error"));

        // When & Then
        assertThatThrownBy(() -> jwtAuthenticationFilter.doFilterInternal(request, response, filterChain))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Token validation error");
    }

    @Test
    @DisplayName("Devrait extraire correctement le token du header Authorization")
    void shouldExtractTokenCorrectlyFromAuthorizationHeader() throws ServletException, IOException {
        // Given
        String expectedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.signature";
        String authHeader = "Bearer " + expectedToken;
        
        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(jwtProcessor.extractUsername(expectedToken)).thenReturn("test@example.com");
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(testUserDetails);
        when(jwtProcessor.isTokenValid(expectedToken)).thenReturn(true);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtProcessor).extractUsername(expectedToken);
        verify(jwtProcessor).isTokenValid(expectedToken);
    }

    @Test
    @DisplayName("Devrait configurer correctement le contexte de sécurité")
    void shouldConfigureSecurityContextCorrectly() throws ServletException, IOException {
        // Given
        String token = "valid.jwt.token";
        String userEmail = "test@example.com";
        
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtProcessor.extractUsername(token)).thenReturn(userEmail);
        when(userDetailsService.loadUserByUsername(userEmail)).thenReturn(testUserDetails);
        when(jwtProcessor.isTokenValid(token)).thenReturn(true);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(testUserDetails);
        assertThat(authentication.getCredentials()).isNull();
        assertThat(authentication.getAuthorities()).isEqualTo(testUserDetails.getAuthorities());
        assertThat(authentication.getDetails()).isNotNull();
    }

    @Test
    @DisplayName("Devrait gérer les tokens avec espaces supplémentaires")
    void shouldHandleTokensWithExtraSpaces() throws ServletException, IOException {
        // Given
        String token = "valid.jwt.token";
        String authHeaderWithSpaces = "Bearer   " + token + "   ";
        
        when(request.getHeader("Authorization")).thenReturn(authHeaderWithSpaces);
        when(jwtProcessor.extractUsername(token)).thenReturn("test@example.com");
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(testUserDetails);
        when(jwtProcessor.isTokenValid(token)).thenReturn(true);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtProcessor).extractUsername(token);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
    }

    /**
     * Classe utilitaire pour les tests UserDetails
     */
    private static class TestUserDetails implements UserDetails {
        private final String username;
        private final String password;
        private final Collection<? extends GrantedAuthority> authorities;

        public TestUserDetails(String username, String password, Collection<? extends GrantedAuthority> authorities) {
            this.username = username;
            this.password = password;
            this.authorities = authorities;
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return authorities;
        }

        @Override
        public String getPassword() {
            return password;
        }

        @Override
        public String getUsername() {
            return username;
        }

        @Override
        public boolean isAccountNonExpired() {
            return true;
        }

        @Override
        public boolean isAccountNonLocked() {
            return true;
        }

        @Override
        public boolean isCredentialsNonExpired() {
            return true;
        }

        @Override
        public boolean isEnabled() {
            return true;
        }
    }
}
