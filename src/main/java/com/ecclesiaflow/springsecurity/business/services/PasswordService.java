package com.ecclesiaflow.springsecurity.business.services;

import com.ecclesiaflow.springsecurity.business.domain.member.Member;

import java.util.Optional;
import java.util.UUID;

/**
 * Service pour la gestion des mots de passe des membres.
 * 
 * <p>Ce service gère les opérations liées aux mots de passe :</p>
 * <ul>
 *   <li>Définition du mot de passe initial après confirmation</li>
 *   <li>Changement de mot de passe pour un membre authentifié</li>
 *   <li>Réinitialisation de mot de passe (mot de passe oublié)</li>
 * </ul>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
public interface PasswordService {

    /**
     * Définit le mot de passe initial d'un membre après confirmation.
     * <p>
     * La validation d'authentification est déléguée à AuthenticationService.
     * </p>
     * 
     * @param email Email du membre (déjà validé par AuthenticationService)
     * @param password Nouveau mot de passe
     * @param memberId UUID du membre (pour lier avec le module members)
     * @return Le membre mis à jour avec le mot de passe défini
     * @throws RuntimeException si le membre n'existe pas ou si l'opération échoue
     */
    Member setInitialPassword(String email, String password, UUID memberId);

    /**
     * Change le mot de passe d'un membre authentifié.
     * <p>
     * Requiert que l'utilisateur connaisse son mot de passe actuel.
     * Utilisé quand l'utilisateur est connecté et veut changer son mot de passe.
     * </p>
     * 
     * @param email Email du membre
     * @param currentPassword Mot de passe actuel
     * @param newPassword Nouveau mot de passe
     * @return Le membre mis à jour avec le nouveau mot de passe
     * @throws RuntimeException si le mot de passe actuel est incorrect ou si le membre n'existe pas
     */
    Member changePassword(String email, String currentPassword, String newPassword);

    /**
     * Initie une demande de réinitialisation de mot de passe.
     * <p>
     * Recherche le membre par email et le retourne s'il existe.
     * La génération du token et du lien est déléguée à la couche Web.
     * </p>
     * <p>
     * <strong>Sécurité:</strong> La couche Web doit toujours retourner le même message,
     * que le compte existe ou non, pour ne pas révéler l'existence d'un email.
     * </p>
     * 
     * @param email Email du membre
     * @return Optional contenant le membre si trouvé, vide sinon
     */
    Optional<Member> requestPasswordReset(String email);

    /**
     * Réinitialise le mot de passe d'un membre avec un token temporaire.
     * <p>
     * Appelé après que l'utilisateur ait cliqué sur le lien de réinitialisation dans l'email.
     * Le token temporaire a déjà été validé par le SecurityFilter avant l'appel de cette méthode.
     * </p>
     * <p>
     * <strong>Différence avec setInitialPassword():</strong> Cette méthode accepte de réinitialiser
     * un mot de passe déjà défini (compte enabled), contrairement à setInitialPassword() qui refuse.
     * </p>
     * 
     * @param email Email du membre (extrait du token validé)
     * @param newPassword Nouveau mot de passe
     * @return Le membre mis à jour avec le nouveau mot de passe
     * @throws RuntimeException si le membre n'existe pas
     */
    Member resetPasswordWithToken(String email, String newPassword);
}
