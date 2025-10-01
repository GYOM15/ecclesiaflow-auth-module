package com.ecclesiaflow.springsecurity.application.config;

import com.ecclesiaflow.springsecurity.business.domain.member.Member;
import com.ecclesiaflow.springsecurity.business.domain.member.Role;
import com.ecclesiaflow.springsecurity.business.services.MemberService;
import com.ecclesiaflow.springsecurity.business.services.adapters.MemberUserDetailsAdapter;
import com.ecclesiaflow.springsecurity.web.security.JwtProcessor;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // <-- CORRECTION POUR UNNECESSARY_STUBBING
@DisplayName("JWTAuthenticationFilter - Tests Unitaires")
class JWTAuthenticationFilterTest {

    @Mock
    private JwtProcessor jwtProcessor;
    @Mock
    private MemberService memberService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;
    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private JWTAuthenticationFilter jwtAuthenticationFilter;

    private static final String VALID_JWT = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9";
    private static final String AUTH_HEADER = "Bearer " + VALID_JWT;
    private static final String USER_EMAIL = "test@ecclesiaflow.com";
    private MemberUserDetailsAdapter userDetails;

    @BeforeEach
    void setUp() {
        // Nettoyer le contexte avant chaque test
        SecurityContextHolder.clearContext();

        // Configurer l'utilisateur simulé
        Member mockMember = Member.builder()
                .id(UUID.randomUUID())
                .email(USER_EMAIL)
                .password("encoded")
                .role(Role.MEMBER)
                .createdAt(LocalDateTime.now()) // Ajouté pour la cohérence du builder
                .enabled(true)
                .build();
        userDetails = new MemberUserDetailsAdapter(mockMember);

        // Configurer les mocks de service pour le succès.
        // L'annotation @MockitoSettings(strictness = Strictness.LENIENT) rend l'utilisation de lenient() optionnelle
        // mais je la laisse ici pour la clarté des stubs généraux.
        when(memberService.userDetailsService()).thenReturn(userDetailsService);
        when(userDetailsService.loadUserByUsername(USER_EMAIL)).thenReturn(userDetails);
        when(jwtProcessor.isTokenValid(VALID_JWT)).thenReturn(true);
        when(jwtProcessor.extractUsername(VALID_JWT)).thenReturn(USER_EMAIL);
    }

    @AfterEach
    void tearDown() {
        // Nettoyer le contexte après chaque test
        SecurityContextHolder.clearContext();
    }

    // ====================================================================
    // Tests shouldNotFilter
    // ====================================================================

    @Test
    @DisplayName("shouldNotFilter - Devrait retourner true pour les chemins d'authentification exclus")
    void shouldNotFilter_ShouldReturnTrueForAuthPaths() {
        when(request.getRequestURI()).thenReturn("/ecclesiaflow/auth/signin");
        assertThat(jwtAuthenticationFilter.shouldNotFilter(request)).isTrue();
    }

    @Test
    @DisplayName("shouldNotFilter - Devrait retourner false pour les chemins protégés")
    void shouldNotFilter_ShouldReturnFalseForProtectedPaths() {
        when(request.getRequestURI()).thenReturn("/ecclesiaflow/members/profile");
        assertThat(jwtAuthenticationFilter.shouldNotFilter(request)).isFalse();
    }

    // ====================================================================
    // Tests doFilterInternal - Cas de succès
    // ====================================================================

    @Test
    @DisplayName("doFilterInternal - Devrait définir l'authentification dans le contexte en cas de JWT valide")
    void doFilterInternal_ShouldSetAuthentication_OnValidJwt() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn(AUTH_HEADER);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        SecurityContext context = SecurityContextHolder.getContext();
        assertThat(context.getAuthentication()).isNotNull();
        assertThat(context.getAuthentication().getName()).isEqualTo(USER_EMAIL);

        // Vérifie que la chaîne continue
        verify(filterChain).doFilter(request, response);

        // Vérifie les étapes du processus
        verify(jwtProcessor).extractUsername(VALID_JWT);
        verify(userDetailsService).loadUserByUsername(USER_EMAIL);
        verify(jwtProcessor).isTokenValid(VALID_JWT);
    }

    @Test
    @DisplayName("doFilterInternal - Devrait ignorer le traitement si le contexte est déjà authentifié")
    void doFilterInternal_ShouldSkipProcessing_IfContextIsAlreadyAuthenticated() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn(AUTH_HEADER);

        // Pré-authentifier le contexte (simule un autre filtre l'ayant déjà fait)
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("preauth", null)
        );

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        // Le contexte initial doit être conservé
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("preauth");

        // Vérifie que l'extraction a eu lieu (car elle est souvent la première étape)
        verify(jwtProcessor).extractUsername(VALID_JWT);
        // Mais que les autres étapes critiques n'ont pas été appelées car l'authentification est déjà faite
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(jwtProcessor, never()).isTokenValid(anyString());

        verify(filterChain).doFilter(request, response);
    }

    // ====================================================================
    // Tests doFilterInternal - Cas d'échec / Saut
    // ====================================================================

    @Test
    @DisplayName("doFilterInternal - Devrait sauter le filtrage si aucun header Authorization n'est présent")
    void doFilterInternal_ShouldSkipFilter_OnNoHeader() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn(null);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtProcessor, memberService);
    }

    @Test
    @DisplayName("doFilterInternal - Devrait sauter le filtrage si le header n'est pas 'Bearer '")
    void doFilterInternal_ShouldSkipFilter_OnInvalidHeaderPrefix() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Basic 12345");

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtProcessor, memberService);
    }

    @Test
    @DisplayName("doFilterInternal - Devrait sauter l'authentification si l'email est vide")
    void doFilterInternal_ShouldSkipAuthentication_OnEmptyEmail() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn(AUTH_HEADER);
        // Simuler un JWT qui ne contient pas d'email
        when(jwtProcessor.extractUsername(VALID_JWT)).thenReturn(null);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verify(jwtProcessor).extractUsername(VALID_JWT);
        verifyNoInteractions(userDetailsService); // userDetailsService ne doit pas être appelé
    }

    // **CORRECTION POUR L'ERREUR UsernameNotFoundException**
    @Test
    @DisplayName("doFilterInternal - Devrait sauter l'authentification si l'utilisateur n'est pas trouvé")
    void doFilterInternal_ShouldSkipAuthentication_OnUserNotFound() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn(AUTH_HEADER);
        // Simuler que le service lève l'exception
        when(userDetailsService.loadUserByUsername(USER_EMAIL)).thenThrow(new UsernameNotFoundException("Not found"));

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        // L'authentification doit échouer mais le filtre doit continuer (Spring Security comportement standard)
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verify(userDetailsService).loadUserByUsername(USER_EMAIL);
    }

    @Test
    @DisplayName("doFilterInternal - Devrait sauter l'authentification si le token est invalide")
    void doFilterInternal_ShouldSkipAuthentication_OnInvalidToken() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn(AUTH_HEADER);
        // Le comportement est overridé par le mock
        when(jwtProcessor.isTokenValid(VALID_JWT)).thenReturn(false);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verify(jwtProcessor).isTokenValid(VALID_JWT);
    }
}