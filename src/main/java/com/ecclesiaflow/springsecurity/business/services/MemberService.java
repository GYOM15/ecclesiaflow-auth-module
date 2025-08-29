package com.ecclesiaflow.springsecurity.business.services;

import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * Interface du service de gestion des membres pour l'intégration Spring Security.
 * <p>
 * Cette interface définit les services nécessaires à l'intégration avec Spring Security,
 * notamment la fourniture d'un {@link UserDetailsService} pour l'authentification.
 * Respecte le principe de responsabilité unique en se concentrant sur les services
 * liés aux membres pour le framework de sécurité.
 * </p>
 * 
 * <p><strong>Rôle architectural :</strong> Service de domaine - Intégration Spring Security</p>
 * 
 * <p><strong>Responsabilités principales :</strong></p>
 * <ul>
 *   <li>Fourniture du UserDetailsService pour Spring Security</li>
 *   <li>Chargement des détails utilisateur lors de l'authentification</li>
 *   <li>Intégration avec les filtres de sécurité Spring</li>
 * </ul>
 * 
 * <p><strong>Cas d'utilisation typiques :</strong></p>
 * <ul>
 *   <li>Configuration du UserDetailsService dans SecurityConfig</li>
 *   <li>Chargement automatique des détails utilisateur par Spring Security</li>
 *   <li>Validation des identifiants lors de l'authentification</li>
 * </ul>
 * 
 * <p><strong>Garanties :</strong> Thread-safe, transactionnel en lecture seule.</p>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
public interface MemberService {

    /**
     * Fournit un service de chargement des détails utilisateur pour Spring Security.
     * <p>
     * Cette méthode retourne une implémentation de {@link UserDetailsService} qui charge
     * les informations d'un membre à partir de son email. Utilisée par Spring Security
     * lors du processus d'authentification pour récupérer les détails de l'utilisateur.
     * </p>
     * 
     * @return un {@link UserDetailsService} configuré pour charger les membres par email
     * @throws RuntimeException si une erreur survient lors de la configuration du service
     * 
     * @implNote L'implémentation retournée effectue une recherche en base de données
     *           et lève UsernameNotFoundException si le membre n'existe pas.
     */
    UserDetailsService userDetailsService();
}
