package com.ecclesiaflow.springsecurity.business.domain.member;

import java.util.UUID;

/**
 * Communication port with the EcclesiaFlow Members module.
 * <p>
 * This port allows the authentication module to query the Members module
 * to verify member confirmation status and notify activations.
 * Follows dependency inversion (Clean Architecture).
 * </p>
 * 
 * <p><strong>Responsibilities:</strong></p>
 * <ul>
 *   <li>Member confirmation status verification</li>
 *   <li>Account activation notification after Keycloak creation</li>
 *   <li>Inter-module communication abstraction</li>
 * </ul>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
public interface MembersClient {
    
    /**
     * Checks if a member's email is NOT confirmed.
     * <p>
     * This method queries the Members module to verify a member's
     * confirmation status. Used to block logins from unconfirmed members.
     * </p>
     * 
     * @param email the member's email to verify
     * @return true if the member does NOT exist OR is NOT confirmed, false if confirmed
     * @throws RuntimeException if communication with the Members module fails
     */
    boolean isEmailNotConfirmed(String email);

    /**
     * Notifies the Members module that a user account has been activated in Keycloak.
     * <p>
     * Called after successful Keycloak user creation, this method allows
     * the Members module to update the member status (CONFIRMED → ACTIVE)
     * and associate the keycloakUserId.
     * </p>
     *
     * @param memberId       the internal member identifier (UUID)
     * @param keycloakUserId the Keycloak identifier (sub claim)
     * @return true if the notification was processed successfully
     * @throws RuntimeException if communication with the Members module fails
     */
    boolean notifyAccountActivated(UUID memberId, String keycloakUserId);
}
