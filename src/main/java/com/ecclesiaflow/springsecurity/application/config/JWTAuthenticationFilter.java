package com.ecclesiaflow.springsecurity.application.config;

import com.ecclesiaflow.springsecurity.web.security.JwtProcessor;
import com.ecclesiaflow.springsecurity.business.services.MemberService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JWTAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProcessor jwtProcessor;
    private final MemberService memberService;


    /**
     * Exécute le filtrage JWT pour chaque requête HTTP entrante.
     * <p>
     * Cette méthode :
     * <ul>
     *   <li>Vérifie la présence d'un header {@code Authorization} avec un token Bearer</li>
     *   <li>Extrait et valide le JWT via {@link JwtProcessor}</li>
     *   <li>Charge l'utilisateur via {@link MemberService}</li>
     *   <li>Met à jour le {@link SecurityContext} si l'authentification est valide</li>
     * </ul>
     *
     * @param request     la requête HTTP entrante
     * @param response    la réponse HTTP en cours de construction
     * @param filterChain la chaîne des filtres Spring Security
     * @throws ServletException si une erreur de servlet survient
     * @throws IOException      si une erreur d'entrée/sortie survient lors du traitement
     */
    @Override
    protected void doFilterInternal(        @NonNull HttpServletRequest request,
                                            @NonNull HttpServletResponse response,
                                            @NonNull FilterChain filterChain
    )
            throws ServletException, IOException {
        
        final String authorizationHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;
        if (StringUtils.isEmpty(authorizationHeader) || !StringUtils.startsWith(authorizationHeader,"Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        jwt = authorizationHeader.substring(7).trim();
        userEmail = jwtProcessor.extractUsername(jwt);

        if(StringUtils.isNotEmpty(userEmail) && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = memberService.userDetailsService().loadUserByUsername(userEmail);

            if (jwtProcessor.isTokenValid(jwt)) {
                SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
                UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken
                        (
                                userDetails, null, userDetails.getAuthorities()
                        );
                token.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                securityContext.setAuthentication(token);
                SecurityContextHolder.setContext(securityContext);
            }
        }
        filterChain.doFilter(request, response);

    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/ecclesiaflow/auth/");
    }
}
