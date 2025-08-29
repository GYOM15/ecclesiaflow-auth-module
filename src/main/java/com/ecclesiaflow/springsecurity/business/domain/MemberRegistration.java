package com.ecclesiaflow.springsecurity.business.domain;

/**
 * Objet métier représentant les données d'inscription d'un nouveau membre EcclesiaFlow.
 * <p>
 * Cette classe encapsule toutes les informations nécessaires pour créer un compte utilisateur
 * dans le système. Utilise le pattern Value Object avec des données immutables une fois créées.
 * </p>
 * 
 * <p><strong>Cas d'utilisation typiques :</strong></p>
 * <ul>
 *   <li>Inscription de nouveaux membres via formulaire web</li>
 *   <li>Validation des données avant création de compte</li>
 *   <li>Transfer d'informations entre couches web et métier</li>
 * </ul>
 * 
 * <p><strong>Garanties :</strong> Immutable, thread-safe.</p>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
public class MemberRegistration {
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String password;

    /**
     * Constructeur pour créer une nouvelle inscription de membre.
     * 
     * @param firstName le prénom du membre, non null
     * @param lastName le nom de famille du membre, non null
     * @param email l'adresse email du membre (doit être valide et unique), non null
     * @param password le mot de passe en clair (sera encodé lors de la création du compte), non null
     */
    public MemberRegistration(String firstName, String lastName, String email, String password) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
    }

    /**
     * Renvoie le prénom du membre.
     * 
     * @return le prénom du membre
     */
    public String getFirstName() { return firstName; }

    /**
     * Renvoie le nom de famille du membre.
     * 
     * @return le nom de famille du membre
     */
    public String getLastName() { return lastName; }

    /**
     * Renvoie l'adresse email du membre.
     * 
     * @return l'adresse email du membre
     */
    public String getEmail() { return email; }

    /**
     * Renvoie le mot de passe en clair du membre.
     * 
     * @return le mot de passe en clair (sera encodé lors de la création du compte)
     */
    public String getPassword() { return password; }
}
