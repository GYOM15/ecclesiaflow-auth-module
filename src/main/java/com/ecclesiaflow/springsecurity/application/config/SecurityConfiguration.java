package com.ecclesiaflow.springsecurity.application.config;

import com.ecclesiaflow.springsecurity.web.security.KeycloakJwtConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * OAuth2 Resource Server security configuration for Keycloak integration.
 * <p>
 * Validates JWT tokens issued by Keycloak and extracts roles for authorization.
 * </p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final KeycloakJwtConverter keycloakJwtConverter;

    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:4200}")
    private String allowedOrigins;

    /**
     * Configures the security filter chain for OAuth2 Resource Server.
     *
     * @param http the HttpSecurity to configure
     * @return the configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(request -> request
                        // Password management (unauthenticated flows)
                        .requestMatchers("/ecclesiaflow/auth/password/setup").permitAll()
                        // Actuator - only liveness/readiness probes public
                        .requestMatchers("/actuator/health/liveness", "/actuator/health/readiness").permitAll()
                        .requestMatchers("/actuator/**").hasAnyRole("SUPER_ADMIN", "SUPPORT")
                        // Swagger - permitAll in dev, should be restricted in prod via profile
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(manager -> manager
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakJwtConverter))
                );
        return http.build();
    }

    /**
     * CORS configuration with allowed origins from properties.
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
