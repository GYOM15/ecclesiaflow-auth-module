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
public class SigninCredentials {
    private final String email;
    private final String password;

    /**
     * Constructeur pour créer de nouveaux identifiants de connexion.
     * 
     * @param email l'adresse email de l'utilisateur (utilisée comme identifiant unique), non null
     * @param password le mot de passe en clair de l'utilisateur, non null
     */
    public SigninCredentials(String email, String password) {
        this.email = email;
        this.password = password;
    }

    /**
     * Renvoie l'adresse email de l'utilisateur.
     * 
     * @return l'adresse email utilisée comme identifiant
     */
    public String getEmail() { return email; }

    /**
     * Renvoie le mot de passe en clair de l'utilisateur.
     * 
     * @return le mot de passe en clair (sera validé contre le hash stocké)
     */
    public String getPassword() { return password; }
}
