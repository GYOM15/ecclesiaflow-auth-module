package com.ecclesiaflow.springsecurity.business.domain.member;

import java.util.UUID;

/**
 * Port de communication avec le module Members d'EcclesiaFlow.
 * <p>
 * Ce port permet au module d'authentification d'interroger le module Members
 * pour vérifier le statut de confirmation des membres et notifier les activations.
 * Respecte l'inversion de dépendance (Clean Architecture).
 * </p>
 * 
 * <p><strong>Responsabilités :</strong></p>
 * <ul>
 *   <li>Vérification du statut de confirmation des membres</li>
 *   <li>Notification d'activation de compte après création Keycloak</li>
 *   <li>Abstraction de la communication inter-modules</li>
 * </ul>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
public interface MembersClient {
    
    /**
     * Vérifie si l'email d'un membre n'est PAS confirmé.
     * <p>
     * Cette méthode interroge le module Members pour vérifier le statut 
     * de confirmation d'un membre. Utilisée pour bloquer les connexions 
     * des membres non confirmés.
     * </p>
     * 
     * @param email l'email du membre à vérifier
     * @return true si le membre n'existe PAS OU n'est PAS confirmé, false si confirmé
     * @throws RuntimeException si la communication avec le module Members échoue
     */
    boolean isEmailNotConfirmed(String email);

    /**
     * Notifie le module Members qu'un compte utilisateur a été activé dans Keycloak.
     * <p>
     * Appelée après la création réussie de l'utilisateur dans Keycloak,
     * cette méthode permet au module Members de mettre à jour le statut
     * du membre (CONFIRMED → ACTIVE) et d'associer le keycloakUserId.
     * </p>
     *
     * @param memberId       l'identifiant interne du membre (UUID)
     * @param keycloakUserId l'identifiant Keycloak (sub claim)
     * @return true si la notification a été traitée avec succès
     * @throws RuntimeException si la communication avec le module Members échoue
     */
    boolean notifyAccountActivated(UUID memberId, String keycloakUserId);
}
