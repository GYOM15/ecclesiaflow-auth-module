package com.ecclesiaflow.springsecurity.business.services;

import com.ecclesiaflow.springsecurity.business.domain.AuthenticationResult;
import com.ecclesiaflow.springsecurity.business.domain.MemberRegistration;
import com.ecclesiaflow.springsecurity.business.domain.SigninCredentials;
import com.ecclesiaflow.springsecurity.business.domain.TokenRefreshData;
import com.ecclesiaflow.springsecurity.io.entities.Member;
import com.ecclesiaflow.springsecurity.web.exception.InvalidCredentialsException;
import com.ecclesiaflow.springsecurity.web.exception.InvalidRequestException;
import com.ecclesiaflow.springsecurity.web.exception.InvalidTokenException;
import com.ecclesiaflow.springsecurity.web.exception.JwtProcessingException;

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
     * Authentifie un membre et génère ses tokens JWT d'accès et de rafraîchissement.
     * <p>
     * Cette méthode orchestre l'authentification complète : validation des identifiants
     * via Spring Security, puis génération des tokens JWT pour l'accès aux ressources protégées.
     * Combine authentification et génération de tokens en une seule opération atomique.
     * </p>
     * 
     * @param signinCredentials identifiants de connexion (email et mot de passe), non null
     * @return un {@link AuthenticationResult} contenant le membre authentifié et ses tokens JWT
     * @throws InvalidCredentialsException si l'email ou le mot de passe est incorrect
     * @throws JwtProcessingException si la génération des tokens échoue
     * @throws IllegalArgumentException si signinCredentials est null
     * 
     * @implNote Opération en lecture seule sur la base de données, génération de tokens en mémoire.
     */
    AuthenticationResult getAuthenticatedMember(SigninCredentials signinCredentials)
            throws InvalidCredentialsException, JwtProcessingException;
    
    /**
     * Rafraîchit un token d'accès à partir d'un refresh token valide.
     * <p>
     * Valide le refresh token, extrait l'utilisateur associé et génère un nouveau
     * token d'accès. Le refresh token reste inchangé pour permettre de futurs
     * rafraîchissements jusqu'à son expiration.
     * </p>
     * 
     * @param refreshData objet métier contenant le refresh token, non null
     * @return un {@link AuthenticationResult} avec le nouveau token d'accès et l'ancien refresh token
     * @throws InvalidTokenException si le refresh token est invalide, expiré ou de mauvais type
     * @throws JwtProcessingException si la génération du nouveau token d'accès échoue
     * @throws IllegalArgumentException si refreshData est null
     * 
     * @implNote Opération en lecture seule, génère un nouveau token d'accès en mémoire.
     */
    AuthenticationResult refreshToken(TokenRefreshData refreshData) 
            throws InvalidTokenException, JwtProcessingException;
}
