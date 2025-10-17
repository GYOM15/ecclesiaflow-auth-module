package com.ecclesiaflow.springsecurity.business.domain.token;

import java.util.UUID;

/**
 * Value Object représentant une requête de génération de token temporaire.
 * <p>
 * Objet domain pur, indépendant du framework web et des DTOs OpenAPI.
 * Encapsule les données nécessaires pour générer un token temporaire
 * après confirmation d'un membre.
 * </p>
 *
 * <p><strong>Rôle architectural:</strong> Value Object - Domain Layer</p>
 *
 * @param email l'email du membre, non null
 * @param memberId l'UUID du membre (identifiant partagé), non null
 *
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
public record TemporaryToken(String email, UUID memberId) {
    
    /**
     * Constructeur compact avec validation.
     * 
     * @throws IllegalArgumentException si email ou memberId est null
     */
    public TemporaryToken {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("L'email ne peut pas être null ou vide");
        }
        if (memberId == null) {
            throw new IllegalArgumentException("Le memberId ne peut pas être null");
        }
    }
}
