package com.ecclesiaflow.springsecurity.io.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Entité JPA représentant un membre EcclesiaFlow dans la base de données.
 * <p>
 * Cette classe implémente {@link UserDetails} pour l'intégration avec Spring Security,
 * permettant l'authentification et l'autorisation des membres. Utilise UUID comme
 * identifiant primaire pour garantir l'unicité dans un environnement multi-tenant.
 * </p>
 * 
 * <p><strong>Dépendances critiques :</strong></p>
 * <ul>
 *   <li>JPA/Hibernate pour la persistance</li>
 *   <li>Spring Security pour l'authentification</li>
 *   <li>UUID Generator pour les identifiants uniques</li>
 * </ul>
 * 
 * <p><strong>Garanties :</strong> Thread-safe pour les opérations de lecture, 
 * gestion transactionnelle via JPA.</p>
 *
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
@Entity
@Table(name = "member")
public class Member implements UserDetails {

    /**
     * Identifiant unique du membre, généré automatiquement via UUID.
     * <p>
     * Assure l'unicité du membre dans la base de données.
     * </p>
     */
    @Id
    @GeneratedValue(generator = "uuid2")
    @UuidGenerator
    @Column(name = "id", columnDefinition = "BINARY(16)", updatable = false, nullable = false)
    private UUID id;
    /**
     * Adresse e-mail du membre, utilisée comme identifiant pour la connexion.
     */
    private String email;

    /**
     * Mot de passe du membre, stocké de manière sécurisée.
     */
    private String password;

    /**
     * Rôle du membre dans l'application, déterminant ses autorisations.
     */
    private Role role;
    private LocalDateTime updatedAt;

    /**
     * Retourne les autorisations du membre sous forme de collection de {@link GrantedAuthority}.
     * <p>
     * Dans ce cas, l'autorisation est basée sur le rôle du membre.
     * </p>
     *
     * @return Collection d'autorisations
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    /**
     * Retourne l'identifiant du membre, qui est son adresse e-mail.
     *
     * @return Identifiant du membre
     */
    @Override
    public String getUsername() {
        return email;
    }

    /**
     * Retourne le mot de passe du membre.
     *
     * @return Mot de passe du membre
     */
    @Override
    public String getPassword() {
        return password;
    }

    /**
     * Indique si le compte du membre n'est pas expiré.
     * <p>
     * Dans ce cas, les comptes ne sont jamais expirés.
     * </p>
     *
     * @return Vrai si le compte n'est pas expiré
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Indique si le compte du membre n'est pas verrouillé.
     * <p>
     * Dans ce cas, les comptes ne sont jamais verrouillés.
     * </p>
     *
     * @return Vrai si le compte n'est pas verrouillé
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * Indique si les informations d'identification du membre n'ont pas expiré.
     * <p>
     * Dans ce cas, les informations d'identification ne sont jamais expirées.
     * </p>
     *
     * @return Vrai si les informations d'identification n'ont pas expiré
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Indique si le compte du membre est activé.
     * <p>
     * Dans ce cas, les comptes sont toujours activés.
     * </p>
     *
     * @return Vrai si le compte est activé
     */
    @Override
    public boolean isEnabled() {
        return true;
    }

    // Getters and setters

    /**
     * Retourne l'adresse e-mail du membre.
     *
     * @return Adresse e-mail du membre
     */
    public String getEmail() {
        return email;
    }

    /**
     * Définit l'adresse e-mail du membre.
     *
     * @param email Adresse e-mail du membre
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Définit le mot de passe du membre.
     *
     * @param password Mot de passe du membre
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Retourne le rôle du membre.
     *
     * @return Rôle du membre
     */
    public Role getRole() {
        return role;
    }

    /**
     * Définit le rôle du membre.
     *
     * @param role Rôle du membre
     */
    public void setRole(Role role) {
        this.role = role;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
