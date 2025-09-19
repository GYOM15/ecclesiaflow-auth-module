package com.ecclesiaflow.springsecurity.business.services;

import com.ecclesiaflow.springsecurity.business.domain.MemberRegistration;
import com.ecclesiaflow.springsecurity.business.domain.SigninCredentials;
import com.ecclesiaflow.springsecurity.io.entities.Member;
import com.ecclesiaflow.springsecurity.web.exception.InvalidCredentialsException;
import com.ecclesiaflow.springsecurity.web.exception.InvalidRequestException;
import com.ecclesiaflow.springsecurity.web.exception.JwtProcessingException;
import com.ecclesiaflow.springsecurity.business.exceptions.MemberNotFoundException;

/**
 * Interface du service d'authentification centralisée pour EcclesiaFlow.
 * <p>
 * Cette interface définit les opérations d'authentification, d'inscription et de gestion
 * des tokens JWT. Constitue le point d'entrée principal pour toutes les opérations
 * liées à l'authentification des membres dans le système multi-tenant.
 * </p>
 * 
 * <p><strong>Rôle architectural :</strong> Service de domaine - Authentification centralisée</p>
 * 
 * <p><strong>Responsabilités principales :</strong></p>
 * <ul>
 *   <li>Inscription de nouveaux membres avec validation</li>
 *   <li>Authentification des membres existants</li>
 *   <li>Génération et rafraîchissement des tokens JWT</li>
 *   <li>Orchestration des opérations d'authentification</li>
 * </ul>
 * 
 * <p><strong>Cas d'utilisation typiques :</strong></p>
 * <ul>
 *   <li>Inscription de nouveaux utilisateurs via API REST</li>
 *   <li>Connexion avec email/mot de passe</li>
 *   <li>Renouvellement automatique des tokens expirés</li>
 *   <li>Intégration avec les contrôleurs d'authentification</li>
 * </ul>
 * 
 * <p><strong>Garanties :</strong> Thread-safe, transactionnel, gestion d'erreurs complète.</p>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
public interface AuthenticationService {
    
    /**
     * Enregistre un nouveau membre dans le système EcclesiaFlow.
     * <p>
     * Cette méthode valide les données d'inscription, vérifie l'unicité de l'email
     * et crée un nouveau compte membre avec encodage sécurisé du mot de passe.
     * Opération transactionnelle qui garantit la cohérence des données.
     * </p>
     * 
     * @param memberRegistration données d'inscription du nouveau membre, non null
     * @return le membre enregistré avec son identifiant UUID généré
     * @throws InvalidRequestException si les données sont invalides ou l'email déjà utilisé
     * @throws IllegalArgumentException si memberRegistration est null
     * 
     * @implNote Délègue à MemberRegistrationService pour respecter la séparation des responsabilités.
     */
    Member registerMember(MemberRegistration memberRegistration) throws InvalidRequestException;
    
    /**
     * Authentifie un membre à partir de ses identifiants.
     * <p>
     * Cette méthode effectue une authentification pure en validant
     * l'email et le mot de passe via Spring Security. Elle retourne
     * uniquement le membre authentifié sans génération de tokens.
     * </p>
     * 
     * @param signinCredentials identifiants de connexion (email et mot de passe), non null
     * @return le {@link Member} authentifié
     * @throws InvalidCredentialsException si l'email ou le mot de passe est incorrect
     * @throws JwtProcessingException si une erreur survient pendant l'authentification
     * @throws IllegalArgumentException si signinCredentials est null
     * 
     * @implNote Opération en lecture seule sur la base de données, authentification pure sans tokens.
     */
    Member getAuthenticatedMember(SigninCredentials signinCredentials)
            throws InvalidCredentialsException, JwtProcessingException;


    /**
     * Récupère un membre par son email.
     * <p>
     * Méthode métier pour récupérer un membre existant par son adresse email.
     * Utilisée notamment lors du processus de rafraîchissement des tokens.
     * </p>
     *
     * @param email l'adresse email du membre à récupérer
     * @return le membre correspondant à l'email
     * @throws MemberNotFoundException si aucun membre n'est trouvé pour cet email
     */
    Member getMemberByEmail(String email) throws MemberNotFoundException ;

}
