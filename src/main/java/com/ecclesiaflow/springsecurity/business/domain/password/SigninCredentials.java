package com.ecclesiaflow.springsecurity.business.domain.password;

/**
 * Objet métier représentant les identifiants de connexion d'un utilisateur.
 * <p>
 * Cette classe encapsule les informations d'authentification nécessaires
 * pour se connecter au système EcclesiaFlow. Utilise le pattern Value Object
 * avec des données immutables une fois créées.
 * </p>
 *
 * <p><strong>Cas d'utilisation typiques :</strong></p>
 * <ul>
 *   <li>Authentification via email et mot de passe</li>
 *   <li>Validation des identifiants par les services d'authentification</li>
 * </ul>
 *
 * <p><strong>Garanties :</strong> Immutable, thread-safe.</p>
 *
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
public record SigninCredentials(String email, String password) {
}
