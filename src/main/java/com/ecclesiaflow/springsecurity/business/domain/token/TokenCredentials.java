package com.ecclesiaflow.springsecurity.business.domain.token;

/**
 * Objet métier représentant les données nécessaires pour les opérations sur les tokens JWT
 * de façon générale.
 * <p>
 * Cette classe encapsule les informations
 * nécessaires pour manipuler les tokens JWT (temporary token, refresh, validation).
 * Elle assure le découplage entre la couche API (DTOs) et la couche métier.
 * </p>
 *
 * <p><strong>Responsabilité :</strong></p>
 * <ul>
 *   <li>Encapsuler les données de token pour les opérations métier</li>
 *   <li>Maintenir l'indépendance de la couche métier vis-à-vis des DTOs API</li>
 *   <li>Fournir une représentation métier unifiée pour tous types de tokens</li>
 * </ul>
 *
 * <p><strong>Cas d'utilisation typiques :</strong></p>
 * <ul>
 *   <li>Validation de tokens JWT</li>
 *   <li>Rafraîchissement de tokens JWT</li>
 *   <li>Génération de nouveaux tokens</li>
 *   <li>Extraction d'informations depuis les tokens</li>
 * </ul>
 *
 * <p><strong>Garanties :</strong> Immutable après construction, validation métier.</p>
 *
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
public record TokenCredentials (String token) {
}
