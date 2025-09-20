package com.ecclesiaflow.springsecurity.business.domain.member;

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
public record MemberRegistration(String email, String password) {
}
