package com.ecclesiaflow.springsecurity.web.constants;

/**
 * Classe utilitaire contenant tous les messages d'erreur et de succès de l'application.
 * <p>
 * Cette classe centralise les messages pour assurer :
 * <ul>
 *   <li>Cohérence des messages à travers toute l'application</li>
 *   <li>Facilité de maintenance et de traduction</li>
 *   <li>Sécurité : messages génériques ne divulguant pas d'informations sensibles</li>
 * </ul>
 * </p>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
public final class Messages {

    private Messages() {}

    // === Auth / Tokens ===
    public static final String AUTH_REQUIRED = "Authentification requise. Veuillez vous reconnecter.";
    public static final String SESSION_EXPIRED = "Votre session a expiré. Veuillez vous reconnecter.";
    public static final String INVALID_AUTH_HEADER = "En-tête Authorization manquant ou invalide. Format attendu : Bearer <token>.";
    public static final String INVALID_OR_EXPIRED_LINK = "Le lien utilisé n'est plus valide ou a expiré.";
    public static final String PASSWORD_SETUP_ERROR = "Erreur lors de la définition du mot de passe";
    public static final String INVALID_LINK_PURPOSE = "Le lien utilisé n'est pas valide pour cette opération.";

    // === Account ===
    public static final String ACCOUNT_DISABLED = "Ce compte est actuellement désactivé.";
    public static final String ACCOUNT_LOCKED = "Votre compte est verrouillé. Contactez l'administrateur.";

    // === Password ===
    public static final String PASSWORD_SETUP_SUCCESS = "Votre mot de passe a été défini avec succès. Vous êtes maintenant connecté.";
    public static final String PASSWORD_RESET_SUCCESS = "Votre mot de passe a été réinitialisé avec succès. Vous êtes maintenant connecté.";
    public static final String PASSWORD_CHANGE_SUCCESS = "Votre mot de passe a été modifié avec succès.";
    public static final String PASSWORD_INVALID_CURRENT = "Le mot de passe actuel est incorrect.";
    public static final String PASSWORD_POLICY_VIOLATION = "Le mot de passe ne respecte pas les critères de sécurité requis.";
    public static final String PASSWORD_UPDATE_ERROR = "Une erreur est survenue lors de la mise à jour du mot de passe.";

    // === Email / Reset ===
    public static final String RESET_EMAIL_SENT = "Un lien de réinitialisation a été envoyé.";
    public static final String EMAIL_SEND_ERROR = "Une erreur est survenue lors de l'envoi de l'email de réinitialisation.";

}
