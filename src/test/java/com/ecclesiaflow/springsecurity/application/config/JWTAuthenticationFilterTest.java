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
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("JWTAuthenticationFilter - Tests Unitaires - Couverture 100%")
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
        SecurityContextHolder.clearContext();

        Member mockMember = Member.builder()
                .id(UUID.randomUUID())
                .email(USER_EMAIL)
                .password("encoded")
                .role(Role.MEMBER)
                .createdAt(LocalDateTime.now())
                .enabled(true)
                .build();
        userDetails = new MemberUserDetailsAdapter(mockMember);

        when(memberService.userDetailsService()).thenReturn(userDetailsService);
        when(userDetailsService.loadUserByUsername(USER_EMAIL)).thenReturn(userDetails);
        when(jwtProcessor.isTokenValid(VALID_JWT)).thenReturn(true);
        when(jwtProcessor.extractUsername(VALID_JWT)).thenReturn(USER_EMAIL);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ====================================================================
    // Tests shouldNotFilter
    // ====================================================================

    @Test
    @DisplayName("shouldNotFilter - Devrait retourner true pour /ecclesiaflow/auth/signin")
    void shouldNotFilter_ShouldReturnTrueForAuthSignin() {
        when(request.getRequestURI()).thenReturn("/ecclesiaflow/auth/signin");
        assertThat(jwtAuthenticationFilter.shouldNotFilter(request)).isTrue();
    }

    @Test
    @DisplayName("shouldNotFilter - Devrait retourner true pour /ecclesiaflow/auth/signup")
    void shouldNotFilter_ShouldReturnTrueForAuthSignup() {
        when(request.getRequestURI()).thenReturn("/ecclesiaflow/auth/signup");
        assertThat(jwtAuthenticationFilter.shouldNotFilter(request)).isTrue();
    }

    @Test
    @DisplayName("shouldNotFilter - Devrait retourner true pour tout chemin sous /ecclesiaflow/auth/")
    void shouldNotFilter_ShouldReturnTrueForAnyAuthPath() {
        when(request.getRequestURI()).thenReturn("/ecclesiaflow/auth/refresh");
        assertThat(jwtAuthenticationFilter.shouldNotFilter(request)).isTrue();
    }

    @Test
    @DisplayName("shouldNotFilter - Devrait retourner false pour les chemins protégés")
    void shouldNotFilter_ShouldReturnFalseForProtectedPaths() {
        when(request.getRequestURI()).thenReturn("/ecclesiaflow/members/profile");
        assertThat(jwtAuthenticationFilter.shouldNotFilter(request)).isFalse();
    }

    @Test
    @DisplayName("shouldNotFilter - Devrait retourner false pour la racine")
    void shouldNotFilter_ShouldReturnFalseForRoot() {
        when(request.getRequestURI()).thenReturn("/");
        assertThat(jwtAuthenticationFilter.shouldNotFilter(request)).isFalse();
    }

    // ====================================================================
    // Tests doFilterInternal - Cas de succès
    // ====================================================================

    @Test
    @DisplayName("doFilterInternal - Devrait définir l'authentification dans le contexte en cas de JWT valide")
    void doFilterInternal_ShouldSetAuthentication_OnValidJwt() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(AUTH_HEADER);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        SecurityContext context = SecurityContextHolder.getContext();
        assertThat(context.getAuthentication()).isNotNull();
        assertThat(context.getAuthentication().getName()).isEqualTo(USER_EMAIL);
        assertThat(context.getAuthentication().getAuthorities()).isEqualTo(userDetails.getAuthorities());

        verify(filterChain).doFilter(request, response);
        verify(jwtProcessor).extractUsername(VALID_JWT);
        verify(userDetailsService).loadUserByUsername(USER_EMAIL);
        verify(jwtProcessor).isTokenValid(VALID_JWT);
    }

    @Test
    @DisplayName("doFilterInternal - Devrait ignorer le traitement si le contexte est déjà authentifié")
    void doFilterInternal_ShouldSkipProcessing_IfContextIsAlreadyAuthenticated() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(AUTH_HEADER);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("preauth", null)
        );

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("preauth");

        verify(jwtProcessor).extractUsername(VALID_JWT);
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(jwtProcessor, never()).isTokenValid(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal - Devrait gérer un JWT avec espaces avant/après")
    void doFilterInternal_ShouldHandleJwtWithWhitespace() throws ServletException, IOException {
        String jwtWithSpaces = "  " + VALID_JWT + "  ";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + jwtWithSpaces);
        when(jwtProcessor.extractUsername(VALID_JWT)).thenReturn(USER_EMAIL);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        SecurityContext context = SecurityContextHolder.getContext();
        assertThat(context.getAuthentication()).isNotNull();
        verify(filterChain).doFilter(request, response);
        verify(jwtProcessor).extractUsername(VALID_JWT);
    }

    // ====================================================================
    // Tests doFilterInternal - Cas d'échec / Saut
    // ====================================================================

    @Test
    @DisplayName("doFilterInternal - Devrait sauter le filtrage si aucun header Authorization n'est présent")
    void doFilterInternal_ShouldSkipFilter_OnNoHeader() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtProcessor, memberService);
    }

    @Test
    @DisplayName("doFilterInternal - Devrait sauter le filtrage si le header est vide")
    void doFilterInternal_ShouldSkipFilter_OnEmptyHeader() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtProcessor, memberService);
    }

    @Test
    @DisplayName("doFilterInternal - Devrait sauter le filtrage si le header contient seulement des espaces")
    void doFilterInternal_ShouldSkipFilter_OnWhitespaceHeader() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("   ");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtProcessor, memberService);
    }

    @Test
    @DisplayName("doFilterInternal - Devrait sauter le filtrage si le header n'est pas 'Bearer '")
    void doFilterInternal_ShouldSkipFilter_OnInvalidHeaderPrefix() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Basic 12345");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtProcessor, memberService);
    }

    @Test
    @DisplayName("doFilterInternal - Devrait sauter le filtrage si le header est 'Bearer' sans token")
    void doFilterInternal_ShouldSkipFilter_OnBearerWithoutToken() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer ");
        when(jwtProcessor.extractUsername("")).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verify(jwtProcessor).extractUsername("");
    }

    @Test
    @DisplayName("doFilterInternal - Devrait sauter le filtrage si le header est 'Bearer' avec seulement des espaces")
    void doFilterInternal_ShouldSkipFilter_OnBearerWithOnlySpaces() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer    ");
        when(jwtProcessor.extractUsername("")).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verify(jwtProcessor).extractUsername("");
    }

    @Test
    @DisplayName("doFilterInternal - Devrait sauter l'authentification si l'email extrait est null")
    void doFilterInternal_ShouldSkipAuthentication_OnNullEmail() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(AUTH_HEADER);
        when(jwtProcessor.extractUsername(VALID_JWT)).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verify(jwtProcessor).extractUsername(VALID_JWT);
        verifyNoInteractions(userDetailsService);
    }

    @Test
    @DisplayName("doFilterInternal - Devrait sauter l'authentification si l'email extrait est vide")
    void doFilterInternal_ShouldSkipAuthentication_OnEmptyEmail() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(AUTH_HEADER);
        when(jwtProcessor.extractUsername(VALID_JWT)).thenReturn("");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verify(jwtProcessor).extractUsername(VALID_JWT);
        verifyNoInteractions(userDetailsService);
    }

    @Test
    @DisplayName("doFilterInternal - Devrait sauter l'authentification si l'utilisateur n'est pas trouvé")
    void doFilterInternal_ShouldSkipAuthentication_OnUserNotFound() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(AUTH_HEADER);
        when(userDetailsService.loadUserByUsername(USER_EMAIL))
                .thenThrow(new UsernameNotFoundException("Not found"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verify(userDetailsService).loadUserByUsername(USER_EMAIL);
        verify(jwtProcessor, never()).isTokenValid(anyString());
    }

    @Test
    @DisplayName("doFilterInternal - Devrait sauter l'authentification si le token est invalide")
    void doFilterInternal_ShouldSkipAuthentication_OnInvalidToken() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(AUTH_HEADER);
        when(jwtProcessor.isTokenValid(VALID_JWT)).thenReturn(false);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verify(jwtProcessor).isTokenValid(VALID_JWT);
        verify(userDetailsService).loadUserByUsername(USER_EMAIL);
    }

    @Test
    @DisplayName("doFilterInternal - Devrait sauter l'authentification si userDetails est null après chargement")
    void doFilterInternal_ShouldSkipAuthentication_OnNullUserDetails() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(AUTH_HEADER);
        when(userDetailsService.loadUserByUsername(USER_EMAIL)).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verify(userDetailsService).loadUserByUsername(USER_EMAIL);
        verify(jwtProcessor, never()).isTokenValid(anyString());
    }

    @Test
    @DisplayName("doFilterInternal - Devrait gérer le cas Bearer avec casse différente")
    void doFilterInternal_ShouldNotHandleBearerWithDifferentCase() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("bearer " + VALID_JWT);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtProcessor, memberService);
    }

    @Test
    @DisplayName("doFilterInternal - Devrait gérer le cas BEARER en majuscules")
    void doFilterInternal_ShouldNotHandleBearerInUpperCase() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("BEARER " + VALID_JWT);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtProcessor, memberService);
    }
}