package com.ecclesiaflow.springsecurity.web.constants;

/**
 * Utility class containing all error and success messages for the application.
 * <p>
 * This class centralizes messages to ensure:
 * <ul>
 *   <li>Consistency across the entire application</li>
 *   <li>Ease of maintenance and translation</li>
 *   <li>Security: generic messages that do not expose sensitive information</li>
 * </ul>
 * </p>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
public final class Messages {

    private Messages() {}

    // === Auth / Tokens ===
    public static final String AUTH_REQUIRED = "Authentication required. Please sign in again.";
    public static final String SESSION_EXPIRED = "Your session has expired. Please sign in again.";
    public static final String INVALID_AUTH_HEADER = "Missing or invalid Authorization header. Expected format: Bearer <token>.";
    public static final String INVALID_OR_EXPIRED_LINK = "The link is no longer valid or has expired.";
    public static final String PASSWORD_SETUP_ERROR = "An error occurred while setting up the password.";
    public static final String INVALID_LINK_PURPOSE = "The link is not valid for this operation.";

    // === Account ===
    public static final String ACCOUNT_DISABLED = "This account is currently disabled.";
    public static final String ACCOUNT_LOCKED = "Your account is locked. Please contact the administrator.";

    // === Password ===
    public static final String PASSWORD_SETUP_SUCCESS = "Your password has been successfully set. You are now signed in.";
    public static final String PASSWORD_RESET_SUCCESS = "Your password has been successfully reset. You are now signed in.";
    public static final String PASSWORD_CHANGE_SUCCESS = "Your password has been successfully changed.";
    public static final String PASSWORD_INVALID_CURRENT = "The current password is incorrect.";
    public static final String PASSWORD_POLICY_VIOLATION = "The password does not meet the required security criteria.";
    public static final String PASSWORD_UPDATE_ERROR = "An error occurred while updating the password.";

    // === Email / Reset ===
    public static final String RESET_EMAIL_SENT = "A password reset link has been sent.";
    public static final String EMAIL_SEND_ERROR = "An error occurred while sending the reset email.";

}
