package com.ecclesiaflow.springsecurity.business.domain.password;


/**
 * Business object representing a password management operation.
 * <p>
 * This class encapsulates the data needed for password setup:
 * the password and the setup token. Part of the business layer
 * and remains independent of web layer DTOs.
 * </p>
 * 
 *
 * <p><strong>Main responsibilities:</strong></p>
 * <ul>
 *   <li>Authentication data encapsulation</li>
 *   <li>Information transport between service and web layers</li>
 * </ul>
 * 
 * <p><strong>Guarantees:</strong> Immutable after construction, thread-safe.</p>
 * 
 * @author EcclesiaFlow Team
 * @since 1.0.0
 */
public record PasswordManagement(String password, String xSetupToken) {
}
